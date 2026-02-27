package com.topjohnwu.magisk.ui.superuser

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.databinding.Bindable
import androidx.databinding.ObservableArrayList
import androidx.lifecycle.viewModelScope
import com.topjohnwu.magisk.BR
import com.topjohnwu.magisk.arch.AsyncLoadViewModel
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.R
import com.topjohnwu.magisk.core.data.magiskdb.PolicyDao
import com.topjohnwu.magisk.core.ktx.getLabel
import com.topjohnwu.magisk.core.model.su.SuPolicy
import com.topjohnwu.magisk.databinding.MergeObservableList
import com.topjohnwu.magisk.databinding.RvItem
import com.topjohnwu.magisk.databinding.bindExtra
import com.topjohnwu.magisk.databinding.diffList
import com.topjohnwu.magisk.databinding.set
import com.topjohnwu.magisk.dialog.SuperuserRevokeDialog
import com.topjohnwu.magisk.events.AuthEvent
import com.topjohnwu.magisk.events.SnackbarEvent
import com.topjohnwu.magisk.utils.asText
import com.topjohnwu.magisk.view.TextItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class SuperuserUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val query: String = "",
    val showSystemApps: Boolean = true,
    val policies: List<PolicyCardUiState> = emptyList(),
    val errorMessage: String? = null,
    val revision: Long = 0L,
    // 撤销权限对话框状态
    val revokeDialogState: com.topjohnwu.magisk.dialog.SuperuserRevokeDialog.DialogState = com.topjohnwu.magisk.dialog.SuperuserRevokeDialog.DialogState()
)

data class PolicyCardUiState(
    val key: String,
    val uid: Int,
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val policy: Int,
    val shouldNotify: Boolean,
    val shouldLog: Boolean,
    val showSlider: Boolean,
    val isEnabled: Boolean
)

class SuperuserViewModel(
    private val db: PolicyDao
) : AsyncLoadViewModel() {

    private val itemNoData = TextItem(R.string.superuser_policy_none)

    private val itemsHelpers = ObservableArrayList<TextItem>()
    val itemsPolicies = diffList<PolicyRvItem>()

    val items = MergeObservableList<RvItem>()
        .insertList(itemsHelpers)
        .insertList(itemsPolicies)
    val extraBindings = bindExtra {
        it.put(BR.listener, this)
    }

    @get:Bindable
    var loading = true
        private set(value) = set(value, field, { field = it }, BR.loading)

    private val _uiState = MutableStateFlow(SuperuserUiState())
    val uiState: StateFlow<SuperuserUiState> = _uiState.asStateFlow()

    private var allPolicies: List<PolicyRvItem> = emptyList()

    private fun policyKey(uid: Int, packageName: String) = "$uid:$packageName"

    private fun PolicyRvItem.toCardUiState() = PolicyCardUiState(
        key = policyKey(item.uid, packageName),
        uid = item.uid,
        packageName = packageName,
        appName = appName,
        icon = icon,
        policy = item.policy,
        shouldNotify = item.notification,
        shouldLog = item.logging,
        showSlider = Config.suRestrict || item.policy == SuPolicy.RESTRICT,
        isEnabled = item.policy >= SuPolicy.ALLOW
    )

    private fun findPolicyByKey(key: String) =
        allPolicies.firstOrNull { policyKey(it.item.uid, it.packageName) == key }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        publishFilteredPolicies()
    }

    fun toggleShowSystemApps() {
        _uiState.update { it.copy(showSystemApps = !it.showSystemApps) }
        publishFilteredPolicies()
    }

    fun refresh() {
        viewModelScope.launch {
            loadPolicies(isInitialLoad = false)
        }
    }

    @SuppressLint("InlinedApi")
    override suspend fun doLoadWork() {
        loadPolicies(isInitialLoad = true)
    }

    @SuppressLint("InlinedApi")
    private suspend fun loadPolicies(isInitialLoad: Boolean) {
        if (!Info.showSuperUser) {
            loading = false
            itemsPolicies.update(emptyList())
            itemsHelpers.clear()
            if (itemsHelpers.isEmpty()) {
                itemsHelpers.add(itemNoData)
            }
            allPolicies = emptyList()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    policies = emptyList(),
                    errorMessage = null,
                    revision = it.revision + 1
                )
            }
            return
        }

        if (isInitialLoad) {
            loading = true
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        } else {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        }

        try {
            val policies = withContext(Dispatchers.IO) {
                db.deleteOutdated()
                db.delete(AppContext.applicationInfo.uid)
                val newPolicies = ArrayList<PolicyRvItem>()
                val pm = AppContext.packageManager
                for (policy in db.fetchAll()) {
                    val pkgs =
                        if (policy.uid == Process.SYSTEM_UID) arrayOf("android")
                        else pm.getPackagesForUid(policy.uid)
                    if (pkgs == null) {
                        db.delete(policy.uid)
                        continue
                    }
                    val map = pkgs.mapNotNull { pkg ->
                        try {
                            val info = pm.getPackageInfo(pkg, MATCH_UNINSTALLED_PACKAGES)
                            PolicyRvItem(
                                this@SuperuserViewModel,
                                policy,
                                info.packageName,
                                info.sharedUserId != null,
                                info.applicationInfo?.loadIcon(pm) ?: pm.defaultActivityIcon,
                                info.applicationInfo?.getLabel(pm) ?: info.packageName
                            )
                        } catch (e: PackageManager.NameNotFoundException) {
                            null
                        }
                    }
                    if (map.isEmpty()) {
                        db.delete(policy.uid)
                        continue
                    }
                    newPolicies.addAll(map)
                }
                newPolicies.sortedWith(
                    compareBy(
                        { it.appName.lowercase(Locale.ROOT) },
                        { it.packageName }
                    )
                )
            }

            allPolicies = policies
            itemsPolicies.update(policies)
            if (policies.isNotEmpty()) {
                itemsHelpers.clear()
            } else if (itemsHelpers.isEmpty()) {
                itemsHelpers.add(itemNoData)
            }
            publishFilteredPolicies(errorMessage = null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            itemsPolicies.update(emptyList())
            itemsHelpers.clear()
            if (itemsHelpers.isEmpty()) {
                itemsHelpers.add(itemNoData)
            }
            allPolicies = emptyList()
            _uiState.update {
                it.copy(
                    policies = emptyList(),
                    errorMessage = e.message,
                    revision = it.revision + 1
                )
            }
        } finally {
            loading = false
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }

    private fun publishFilteredPolicies(errorMessage: String? = _uiState.value.errorMessage) {
        val state = _uiState.value
        val query = state.query.trim()
        val base = if (state.showSystemApps) {
            allPolicies
        } else {
            allPolicies.filter { it.item.uid >= Process.FIRST_APPLICATION_UID }
        }
        val filtered = if (query.isEmpty()) {
            base
        } else {
            base.filter {
                it.appName.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
        val mapped = filtered.map { it.toCardUiState() }
        _uiState.update {
            it.copy(
                policies = mapped,
                errorMessage = errorMessage,
                revision = it.revision + 1
            )
        }
    }

    // ---

    fun deleteByKey(key: String) {
        findPolicyByKey(key)?.let { onRevokePressed(key) }
    }

    fun toggleNotifyByKey(key: String) {
        findPolicyByKey(key)?.let {
            it.item.notification = !it.item.notification
            updateNotify(it)
        }
    }

    fun toggleLogByKey(key: String) {
        findPolicyByKey(key)?.let {
            it.item.logging = !it.item.logging
            updateLogging(it)
        }
    }

    fun updatePolicyByKey(key: String, policy: Int) {
        findPolicyByKey(key)?.let {
            updatePolicy(it, policy)
        }
    }

    // ---

    /**
     * 显示撤销权限确认对话框
     *
     * @param key 策略唯一标识
     */
    fun showRevokeDialog(key: String) {
        val item = findPolicyByKey(key) ?: return
        _uiState.update {
            it.copy(
                revokeDialogState = com.topjohnwu.magisk.dialog.SuperuserRevokeDialog.DialogState(
                    visible = true,
                    appName = item.appName
                )
            )
        }
    }

    /**
     * 关闭撤销权限确认对话框
     */
    fun dismissRevokeDialog() {
        _uiState.update {
            it.copy(
                revokeDialogState = it.revokeDialogState.copy(visible = false)
            )
        }
    }

    /**
     * 确认撤销权限
     *
     * @param key 策略唯一标识
     */
    fun confirmRevoke(key: String) {
        dismissRevokeDialog()
        findPolicyByKey(key)?.let { item ->
            viewModelScope.launch {
                db.delete(item.item.uid)
                allPolicies = allPolicies.filterNot { it.item.uid == item.item.uid }
                itemsPolicies.update(allPolicies)
                if (allPolicies.isEmpty() && itemsHelpers.isEmpty()) {
                    itemsHelpers.add(itemNoData)
                } else if (allPolicies.isNotEmpty()) {
                    itemsHelpers.clear()
                }
                publishFilteredPolicies()
            }
        }
    }

    /**
     * 处理撤销按钮点击（带认证检查）
     *
     * @param key 策略唯一标识
     */
    fun onRevokePressed(key: String) {
        val item = findPolicyByKey(key) ?: return

        fun doRevoke() {
            viewModelScope.launch {
                db.delete(item.item.uid)
                allPolicies = allPolicies.filterNot { it.item.uid == item.item.uid }
                itemsPolicies.update(allPolicies)
                if (allPolicies.isEmpty() && itemsHelpers.isEmpty()) {
                    itemsHelpers.add(itemNoData)
                } else if (allPolicies.isNotEmpty()) {
                    itemsHelpers.clear()
                }
                publishFilteredPolicies()
            }
        }

        if (Config.suAuth) {
            AuthEvent { doRevoke() }.publish()
        } else {
            showRevokeDialog(key)
        }
    }

    @Deprecated("使用 onRevokePressed 替代", ReplaceWith("onRevokePressed"))
    fun deletePressed(item: PolicyRvItem) {
        val key = policyKey(item.item.uid, item.packageName)
        onRevokePressed(key)
    }

    fun updateNotify(item: PolicyRvItem) {
        publishFilteredPolicies()
        viewModelScope.launch {
            db.update(item.item)
            val res = when {
                item.item.notification -> R.string.su_snack_notif_on
                else -> R.string.su_snack_notif_off
            }
            itemsPolicies.forEach {
                if (it.item.uid == item.item.uid) {
                    it.notifyPropertyChanged(BR.shouldNotify)
                }
            }
            publishFilteredPolicies()
            SnackbarEvent(res.asText(item.appName)).publish()
        }
    }

    fun updateLogging(item: PolicyRvItem) {
        publishFilteredPolicies()
        viewModelScope.launch {
            db.update(item.item)
            val res = when {
                item.item.logging -> R.string.su_snack_log_on
                else -> R.string.su_snack_log_off
            }
            itemsPolicies.forEach {
                if (it.item.uid == item.item.uid) {
                    it.notifyPropertyChanged(BR.shouldLog)
                }
            }
            publishFilteredPolicies()
            SnackbarEvent(res.asText(item.appName)).publish()
        }
    }

    fun updatePolicy(item: PolicyRvItem, policy: Int) {
        if (item.item.policy == policy) return
        val items = itemsPolicies.filter { it.item.uid == item.item.uid }
        fun updateState() {
            item.item.policy = policy
            publishFilteredPolicies()
            viewModelScope.launch {
                val res = if (policy >= SuPolicy.ALLOW) R.string.su_snack_grant else R.string.su_snack_deny
                db.update(item.item)
                items.forEach {
                    it.notifyPropertyChanged(BR.enabled)
                    it.notifyPropertyChanged(BR.sliderValue)
                }
                publishFilteredPolicies()
                SnackbarEvent(res.asText(item.appName)).publish()
            }
        }

        if (Config.suAuth) {
            AuthEvent { updateState() }.publish()
        } else {
            updateState()
        }
    }
}
