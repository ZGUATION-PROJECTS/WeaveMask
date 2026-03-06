package io.github.seyud.weave.ui.deny

import android.graphics.drawable.Drawable
import androidx.compose.ui.state.ToggleableState

data class DenyListAppUiModel(
    val info: DenyListAppInfo,
    val icon: Drawable?,
    val toggleState: ToggleableState,
    val processes: List<ProcessInfo>,
    val isLoadingProcesses: Boolean,
    val hasLoadedProcesses: Boolean,
)
