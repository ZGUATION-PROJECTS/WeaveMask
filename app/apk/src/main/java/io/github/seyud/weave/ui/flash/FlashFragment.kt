package io.github.seyud.weave.ui.flash

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import io.github.seyud.weave.MainDirections
import io.github.seyud.weave.R
import io.github.seyud.weave.arch.BaseFragment
import io.github.seyud.weave.arch.viewModel
import io.github.seyud.weave.core.Const
import io.github.seyud.weave.core.cmp
import io.github.seyud.weave.databinding.FragmentFlashMd2Binding
import io.github.seyud.weave.ui.MainActivity
import io.github.seyud.weave.core.R as CoreR

class FlashFragment : BaseFragment<FragmentFlashMd2Binding>(), MenuProvider {

    override val layoutRes = R.layout.fragment_flash_md2
    override val viewModel by viewModel<FlashViewModel>()

    private var defaultOrientation = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.args = FlashFragmentArgs.fromBundle(requireArguments())
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(CoreR.string.flash_screen_title)

        viewModel.state.observe(this) {
            activity?.supportActionBar?.setSubtitle(
                when (it) {
                    FlashViewModel.State.FLASHING -> CoreR.string.flashing
                    FlashViewModel.State.SUCCESS -> CoreR.string.done
                    FlashViewModel.State.FAILED -> CoreR.string.failure
                }
            )
            if (it == FlashViewModel.State.SUCCESS && viewModel.showReboot) {
                binding.restartBtn.apply {
                    if (!this.isVisible) this.show()
                    if (!this.isFocused) this.requestFocus()
                }
            }
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_flash, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return viewModel.onMenuItemClicked(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        defaultOrientation = activity?.requestedOrientation ?: -1
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        if (savedInstanceState == null) {
            viewModel.startFlashing()
        }
    }

    @SuppressLint("WrongConstant")
    override fun onDestroyView() {
        if (defaultOrientation != -1) {
            activity?.requestedOrientation = defaultOrientation
        }
        super.onDestroyView()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> true
            else -> false
        }
    }

    override fun onBackPressed(): Boolean {
        if (viewModel.flashing.value == true)
            return true
        return super.onBackPressed()
    }

    override fun onPreBind(binding: FragmentFlashMd2Binding) = Unit

    companion object {

        private fun createIntent(context: Context, action: String, uri: Uri?): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_FLASH_ACTION, action)
                uri?.let { putExtra(MainActivity.EXTRA_FLASH_URI, it.toString()) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            return PendingIntent.getActivity(context, action.hashCode(), intent, flag)
        }

        private fun flashType(isSecondSlot: Boolean) =
            if (isSecondSlot) Const.Value.FLASH_INACTIVE_SLOT else Const.Value.FLASH_MAGISK

        /* Flashing is understood as installing / flashing magisk itself */

        fun flash(isSecondSlot: Boolean) = MainDirections.actionFlashFragment(
            action = flashType(isSecondSlot)
        )

        /* Patching is understood as injecting img files with magisk */

        fun patch(uri: Uri) = MainDirections.actionFlashFragment(
            action = Const.Value.PATCH_FILE,
            additionalData = uri
        )

        /* Uninstalling is understood as removing magisk entirely */

        fun uninstall() = MainDirections.actionFlashFragment(
            action = Const.Value.UNINSTALL
        )

        /* Installing is understood as flashing modules / zips */

        fun installIntent(context: Context, file: Uri) =
            createIntent(context, Const.Value.FLASH_ZIP, file)

        fun install(file: Uri) = MainDirections.actionFlashFragment(
            action = Const.Value.FLASH_ZIP,
            additionalData = file,
        )
    }

}
