package com.topjohnwu.magisk.ui.install

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.R as CoreR
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 安装方法枚举
 * 定义不同的安装方式
 */
enum class InstallMethod {
    PATCH,
    DIRECT,
    INACTIVE_SLOT
}

/**
 * 安装页面屏幕
 * 显示安装选项和方法选择
 */
@Composable
fun InstallScreen(
    viewModel: InstallViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val step = viewModel.step
    val method = viewModel.method

    var keepVerity by remember { mutableStateOf(Config.keepVerity) }
    var keepEnc by remember { mutableStateOf(Config.keepEnc) }
    var recovery by remember { mutableStateOf(Config.recovery) }

    val skipOptions = viewModel.skipOptions
    val isRooted = viewModel.isRooted
    val noSecondSlot = viewModel.noSecondSlot

    val notes = viewModel.notes
    val hasNotes = notes.isNotEmpty()

    var selectedMethod by remember { mutableStateOf<InstallMethod?>(null) }

    MiuixTheme {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = context.getString(CoreR.string.install),
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.padding(start = 16.dp),
                            onClick = onNavigateBack
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                    }
                )
            },
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!skipOptions) {
                        OptionsCard(
                            step = step,
                            keepVerity = keepVerity,
                            keepEnc = keepEnc,
                            recovery = recovery,
                            isSAR = Info.isSAR,
                            isFDE = Info.isFDE,
                            hasRamdisk = Info.ramdisk,
                            onKeepVerityChange = {
                                keepVerity = !keepVerity
                                Config.keepVerity = keepVerity
                            },
                            onKeepEncChange = {
                                keepEnc = !keepEnc
                                Config.keepEnc = keepEnc
                            },
                            onRecoveryChange = {
                                recovery = !recovery
                                Config.recovery = recovery
                            },
                            onNextClick = { viewModel.step = 1 }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    MethodCard(
                        step = step,
                        selectedMethod = selectedMethod,
                        isRooted = isRooted,
                        noSecondSlot = noSecondSlot,
                        dataUri = viewModel.data.value,
                        onMethodChange = { newMethod ->
                            selectedMethod = newMethod
                            viewModel.method = when (newMethod) {
                                InstallMethod.PATCH -> METHOD_PATCH
                                InstallMethod.DIRECT -> METHOD_DIRECT
                                InstallMethod.INACTIVE_SLOT -> METHOD_INACTIVE_SLOT
                                null -> -1
                            }
                        },
                        onInstallClick = { viewModel.install() }
                    )

                    if (hasNotes) {
                        Spacer(modifier = Modifier.height(8.dp))
                        NotesCard(notes = notes.toString())
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        )
    }
}

/**
 * 安装选项卡片
 * 显示安装前的配置选项
 */
@Composable
private fun OptionsCard(
    step: Int,
    keepVerity: Boolean,
    keepEnc: Boolean,
    recovery: Boolean,
    isSAR: Boolean,
    isFDE: Boolean,
    hasRamdisk: Boolean,
    onKeepVerityChange: () -> Unit,
    onKeepEncChange: () -> Unit,
    onRecoveryChange: () -> Unit,
    onNextClick: () -> Unit
) {
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = MiuixIcons.Ok,
                    contentDescription = null,
                    tint = if (step > 0) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceContainer,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = context.getString(CoreR.string.install_options_title),
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (step == 0) {
                    TextButton(
                        text = context.getString(CoreR.string.install_next),
                        onClick = onNextClick
                    )
                }
            }

            if (step > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                if (!isSAR) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onKeepVerityChange)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = keepVerity,
                            onCheckedChange = { onKeepVerityChange() }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = context.getString(CoreR.string.keep_dm_verity),
                            style = MiuixTheme.textStyles.body1
                        )
                    }
                }

                if (isFDE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onKeepEncChange)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = keepEnc,
                            onCheckedChange = { onKeepEncChange() }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = context.getString(CoreR.string.keep_force_encryption),
                            style = MiuixTheme.textStyles.body1
                        )
                    }
                }

                if (!hasRamdisk) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRecoveryChange)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = recovery,
                            onCheckedChange = { onRecoveryChange() }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = context.getString(CoreR.string.recovery_mode),
                            style = MiuixTheme.textStyles.body1
                        )
                    }
                }
            }
        }
    }
}

/**
 * 安装方法卡片
 * 显示安装方法选择和开始安装按钮
 */
@Composable
private fun MethodCard(
    step: Int,
    selectedMethod: InstallMethod?,
    isRooted: Boolean,
    noSecondSlot: Boolean,
    dataUri: Uri?,
    onMethodChange: (InstallMethod?) -> Unit,
    onInstallClick: () -> Unit
) {
    val context = LocalContext.current

    val isMethodPatch = selectedMethod == InstallMethod.PATCH
    val isMethodSelected = if (isMethodPatch) dataUri != null else selectedMethod != null

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = MiuixIcons.Ok,
                    contentDescription = null,
                    tint = if (step > 1) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceContainer,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = context.getString(CoreR.string.install_method_title),
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (step == 1) {
                    Button(
                        onClick = onInstallClick,
                        enabled = isMethodSelected,
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(text = context.getString(CoreR.string.install_start))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = MiuixIcons.ChevronForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (step == 1) {
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.selectableGroup()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedMethod == InstallMethod.PATCH,
                                onClick = { onMethodChange(InstallMethod.PATCH) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMethod == InstallMethod.PATCH,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = context.getString(CoreR.string.select_patch_file),
                            style = MiuixTheme.textStyles.body1
                        )
                    }

                    if (isRooted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedMethod == InstallMethod.DIRECT,
                                    onClick = { onMethodChange(InstallMethod.DIRECT) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == InstallMethod.DIRECT,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(CoreR.string.direct_install),
                                style = MiuixTheme.textStyles.body1
                            )
                        }
                    }

                    if (!noSecondSlot) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedMethod == InstallMethod.INACTIVE_SLOT,
                                    onClick = { onMethodChange(InstallMethod.INACTIVE_SLOT) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == InstallMethod.INACTIVE_SLOT,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(CoreR.string.install_inactive_slot),
                                style = MiuixTheme.textStyles.body1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 发布说明卡片
 * 显示版本更新说明
 */
@Composable
private fun NotesCard(notes: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = notes,
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * 安装方法常量
 * 与原有 R.id 值保持一致
 */
private const val METHOD_PATCH = 1
private const val METHOD_DIRECT = 2
private const val METHOD_INACTIVE_SLOT = 3
