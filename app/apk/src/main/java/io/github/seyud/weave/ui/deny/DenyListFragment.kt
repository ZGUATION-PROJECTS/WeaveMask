package io.github.seyud.weave.ui.deny

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.github.seyud.weave.arch.AsyncLoadViewModel
import io.github.seyud.weave.arch.ViewModelHolder
import io.github.seyud.weave.arch.viewModel
import io.github.seyud.weave.ui.theme.WeaveMagiskTheme
import io.github.seyud.weave.core.R as CoreR

class DenyListFragment : Fragment(), ViewModelHolder {

    override val viewModel by viewModel<DenyListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startObserveLiveData()
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(CoreR.string.denylist)
    }

    override fun onResume() {
        super.onResume()
        (viewModel as? AsyncLoadViewModel)?.startLoading()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.onSaveState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        savedInstanceState?.let { viewModel.onRestoreState(it) }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WeaveMagiskTheme {
                    DenyListScreen(
                        viewModel = viewModel,
                        onNavigateBack = { findNavController().navigateUp() },
                    )
                }
            }
        }
    }
}
