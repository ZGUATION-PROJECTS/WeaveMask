package com.topjohnwu.magisk.ui.module

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.topjohnwu.magisk.core.R as CoreR
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 模块列表页面
 * 使用 Compose 实现模块管理界面
 *
 * @param viewModel 模块 ViewModel
 * @param bottomPadding 底部内边距，用于避免内容被底部导航栏遮挡
 * @param modifier Modifier
 */
@Composable
fun ModuleScreen(
    viewModel: ModuleViewModel,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.surface,
        tint = HazeTint(MiuixTheme.colorScheme.surface.copy(0.8f))
    )

    MiuixTheme {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    modifier = Modifier.hazeEffect(hazeState) {
                        style = hazeStyle
                        blurRadius = 30.dp
                        noiseFactor = 0f
                    },
                    color = Color.Transparent,
                    title = context.getString(CoreR.string.modules)
                )
            },
            content = { paddingValues ->
                ModuleListContent(
                    viewModel = viewModel,
                    paddingValues = paddingValues,
                    bottomPadding = bottomPadding,
                    hazeState = hazeState
                )
            }
        )
    }
}

/**
 * 模块列表内容区域
 * 处理加载状态、空状态和列表显示
 *
 * @param viewModel 模块 ViewModel
 * @param paddingValues 内边距
 * @param bottomPadding 底部内边距
 * @param hazeState Haze 状态
 */
@Composable
private fun ModuleListContent(
    viewModel: ModuleViewModel,
    paddingValues: PaddingValues,
    bottomPadding: Dp,
    hazeState: HazeState
) {
    val context = LocalContext.current
    val items = viewModel.items
    val loading = viewModel.loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(state = hazeState)
            .padding(paddingValues)
    ) {
        when {
            loading -> {
                LoadingContent()
            }
            items.size <= 1 -> {
                EmptyContent()
            }
            else -> {
                ModuleList(
                    viewModel = viewModel,
                    items = items,
                    bottomPadding = bottomPadding
                )
            }
        }
    }
}

/**
 * 加载状态显示
 */
@Composable
private fun LoadingContent() {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = context.getString(CoreR.string.loading),
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
        }
    }
}

/**
 * 空状态显示
 */
@Composable
private fun EmptyContent() {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = context.getString(CoreR.string.module_empty),
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 模块列表
 * 使用 LazyColumn 显示所有模块项
 *
 * @param viewModel 模块 ViewModel
 * @param items 模块列表
 * @param bottomPadding 底部内边距
 */
@Composable
private fun ModuleList(
    viewModel: ModuleViewModel,
    items: List<Any>,
    bottomPadding: Dp
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        items(
            items = items.filter { it !is InstallModule },
            key = { (it as? LocalModuleRvItem)?.item?.id?.hashCode() ?: it.hashCode() }
        ) { item ->
            when (item) {
                is LocalModuleRvItem -> {
                    ModuleItem(
                        item = item,
                        viewModel = viewModel
                    )
                }
                is InstallModule -> {
                    // Install module button - not implemented in this version
                }
            }
        }

        // 底部间距 - 使用传入的 bottomPadding 确保最后一个卡片内容可以正常显示
        item {
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

/**
 * 单个模块项组件
 * 显示模块信息、开关、删除/恢复按钮等
 */
@Composable
private fun ModuleItem(
    item: LocalModuleRvItem,
    viewModel: ModuleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val module = item.item
    val isRemoved = item.isRemoved
    val isEnabled = item.isEnabled
    val showUpdate = item.showUpdate
    val updateReady = item.updateReady
    val showAction = item.showAction
    val showNotice = item.showNotice
    val noticeText = item.noticeText.getText(context.resources).toString()

    val isEnabledState = !isRemoved && isEnabled && !showNotice

    val cardAlpha = if (isEnabledState) 1f else 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        cornerRadius = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = module.name,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        textDecoration = if (isRemoved) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${module.version} by ${module.author}",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                        textDecoration = if (isRemoved) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = isEnabled,
                    enabled = isEnabledState,
                    onCheckedChange = { checked ->
                        module.enable = checked
                    }
                )
            }

            if (module.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = module.description,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    textDecoration = if (isRemoved) TextDecoration.LineThrough else null,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showNotice) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = noticeText,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                color = MiuixTheme.colorScheme.surfaceContainerHigh,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showAction) {
                    TextButton(
                        text = context.getString(CoreR.string.module_action),
                        onClick = { viewModel.runAction(module.id, module.name) },
                        enabled = isEnabled
                    )
                }

                if (showUpdate) {
                    TextButton(
                        text = context.getString(CoreR.string.update),
                        onClick = { viewModel.downloadPressed(module.updateInfo) },
                        enabled = updateReady
                    )
                }

                TextButton(
                    text = if (isRemoved) context.getString(CoreR.string.module_state_restore) else context.getString(CoreR.string.module_state_remove),
                    onClick = { item.delete() },
                    enabled = !item.isUpdated
                )
            }
        }
    }
}
