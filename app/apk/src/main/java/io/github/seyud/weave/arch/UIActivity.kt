package io.github.seyud.weave.arch

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.use
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import io.github.seyud.weave.BR
import io.github.seyud.weave.R
import io.github.seyud.weave.core.Config
import io.github.seyud.weave.core.base.ActivityExtension
import io.github.seyud.weave.core.base.IActivityExtension
import io.github.seyud.weave.core.isRunningAsStub
import io.github.seyud.weave.core.ktx.reflectField
import io.github.seyud.weave.core.wrap
import rikka.insets.WindowInsetsHelper
import rikka.layoutinflater.view.LayoutInflaterFactory

abstract class UIActivity<Binding : ViewDataBinding>
    : AppCompatActivity(), ViewModelHolder, IActivityExtension {

    protected lateinit var binding: Binding
    protected abstract val layoutRes: Int
    override val extension = ActivityExtension(this)

    protected val binded get() = ::binding.isInitialized

    init {
        AppCompatDelegate.setDefaultNightMode(Config.darkTheme)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.wrap())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        layoutInflater.factory2 = LayoutInflaterFactory(delegate)
            .addOnViewCreatedListener(WindowInsetsHelper.LISTENER)

        extension.onCreate(savedInstanceState)
        if (isRunningAsStub) {
            // Overwrite private members to avoid nasty "false" stack traces being logged
            val delegate = delegate
            val clz = delegate.javaClass
            clz.reflectField("mActivityHandlesConfigFlagsChecked").set(delegate, true)
            clz.reflectField("mActivityHandlesConfigFlags").set(delegate, 0)
        }

        super.onCreate(savedInstanceState)

        startObserveLiveData()

        // We need to set the window background explicitly since for whatever reason it's not
        // propagated upstream
        obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
            .use { it.getDrawable(0) }
            .also { window.setBackgroundDrawable(it) }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window?.decorView?.post {
                // If navigation bar is short enough (gesture navigation enabled), make it transparent
                val bottomInset = ViewCompat.getRootWindowInsets(window.decorView)
                    ?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
                if (bottomInset < Resources.getSystem().displayMetrics.density * 40) {
                    // navigationBarColor / dividerColor / isStatusBarContrastEnforced are
                    // deprecated in API 35 (edge-to-edge default), but there is no direct
                    // Compat replacement — the code already enables edge-to-edge via
                    // WindowCompat.setDecorFitsSystemWindows, so these are only effective
                    // on older API levels where the deprecation does not apply.
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        @Suppress("DEPRECATION")
                        window.navigationBarDividerColor = Color.TRANSPARENT
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                        @Suppress("DEPRECATION")
                        window.isStatusBarContrastEnforced = false
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        extension.onSaveInstanceState(outState)
    }

    fun setContentView() {
        binding = DataBindingUtil.setContentView<Binding>(this, layoutRes).also {
            it.setVariable(BR.viewModel, viewModel)
            it.lifecycleOwner = this
        }
    }

    fun setAccessibilityDelegate(delegate: View.AccessibilityDelegate?) {
        binding.root.rootView.accessibilityDelegate = delegate
    }

    override fun onResume() {
        super.onResume()
        viewModel.let {
            if (it is AsyncLoadViewModel)
                it.startLoading()
        }
    }

    override fun onEventDispatched(event: ViewEvent) = when (event) {
        is ContextExecutor -> event(this)
        is ActivityExecutor -> event(this)
        else -> Unit
    }
}

fun ViewGroup.startAnimations() {
    val transition = AutoTransition()
        .setInterpolator(FastOutSlowInInterpolator())
        .setDuration(400)
        .excludeTarget(R.id.main_toolbar, true)
    TransitionManager.beginDelayedTransition(
        this,
        transition
    )
}
