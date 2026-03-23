package io.github.seyud.weave.events

import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavDirections
import io.github.seyud.weave.arch.ActivityExecutor
import io.github.seyud.weave.arch.ContextExecutor
import io.github.seyud.weave.arch.NavigationActivity
import io.github.seyud.weave.arch.UIActivity
import io.github.seyud.weave.arch.ViewEvent
import io.github.seyud.weave.ui.MainActivity
import io.github.seyud.weave.core.base.ContentResultCallback
import io.github.seyud.weave.core.base.IActivityExtension
import io.github.seyud.weave.core.base.relaunch
import io.github.seyud.weave.utils.TextHolder
import io.github.seyud.weave.utils.asText
import io.github.seyud.weave.view.MagiskDialog
import io.github.seyud.weave.view.Shortcuts
import top.yukonga.miuix.kmp.basic.SnackbarDuration

class PermissionEvent(
    private val permission: String,
    private val callback: (Boolean) -> Unit
) : ViewEvent(), ActivityExecutor {

    override fun invoke(activity: AppCompatActivity) =
        (activity as? IActivityExtension)?.withPermission(permission, callback) ?: callback(false)
}

class BackPressEvent : ViewEvent(), ActivityExecutor {
    override fun invoke(activity: AppCompatActivity) {
        activity.onBackPressedDispatcher.onBackPressed()
    }
}

class DieEvent : ViewEvent(), ActivityExecutor {
    override fun invoke(activity: AppCompatActivity) {
        activity.finish()
    }
}

/**
 * @deprecated SuRequestActivity 已迁移到 Compose，不再需要此事件
 */
@Deprecated("SuRequestActivity 已使用 Compose 实现，不再需要 ShowUIEvent")
class ShowUIEvent(private val accessibilityDelegate: View.AccessibilityDelegate?)
    : ViewEvent(), ActivityExecutor {
    override fun invoke(activity: AppCompatActivity) {
        (activity as? UIActivity<*>)?.apply {
            setContentView()
            setAccessibilityDelegate(accessibilityDelegate)
        }
    }
}

class RecreateEvent : ViewEvent(), ActivityExecutor {
    override fun invoke(activity: AppCompatActivity) {
        activity.relaunch()
    }
}

class AuthEvent(
    private val callback: () -> Unit
) : ViewEvent(), ActivityExecutor {

    override fun invoke(activity: AppCompatActivity) {
        (activity as? IActivityExtension)?.withAuthentication { if (it) callback() }
    }
}

class GetContentEvent(
    private val type: String,
    private val callback: ContentResultCallback
) : ViewEvent(), ActivityExecutor {
    override fun invoke(activity: AppCompatActivity) {
        (activity as? IActivityExtension)?.getContent(type, callback)
    }
}

class NavigationEvent(
    private val directions: NavDirections,
    private val pop: Boolean
) : ViewEvent(), ActivityExecutor {
    override fun invoke(activity: AppCompatActivity) {
        (activity as? NavigationActivity<*>)?.apply {
            if (pop) navigation.popBackStack()
            directions.navigate()
        }
    }
}

class AddHomeIconEvent : ViewEvent(), ContextExecutor {
    override fun invoke(context: Context) {
        Shortcuts.addHomeIcon(context)
    }
}

class SnackbarEvent(
    private val msg: TextHolder,
    private val duration: SnackbarDuration = SnackbarDuration.Short,
) : ViewEvent(), ActivityExecutor {

    constructor(
        @StringRes res: Int,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ) : this(res.asText(), duration)

    constructor(
        msg: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ) : this(msg.asText(), duration)

    fun resolveMessage(activity: AppCompatActivity): String =
        msg.getText(activity.resources).toString()

    fun resolveDuration(): SnackbarDuration = duration

    override fun invoke(activity: AppCompatActivity) {
        (activity as? MainActivity)?.showSnackbar(this)
    }
}

class DialogEvent(
    private val builder: DialogBuilder
) : ViewEvent(), ActivityExecutor {
    override fun invoke(activity: AppCompatActivity) {
        MagiskDialog(activity).apply(builder::build).show()
    }
}

interface DialogBuilder {
    fun build(dialog: MagiskDialog)
}
