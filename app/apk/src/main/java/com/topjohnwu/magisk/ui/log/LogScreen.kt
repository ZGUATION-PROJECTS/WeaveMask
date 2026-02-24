package com.topjohnwu.magisk.ui.log

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.topjohnwu.magisk.core.R as CoreR
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 日志页面
 * 使用 Compose 实现日志查看界面
 */
@Composable
fun LogScreen(
    viewModel: LogViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSuperuserLog by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.surface,
        tint = HazeTint(MiuixTheme.colorScheme.surface.copy(0.8f))
    )

    val loading = viewModel.loading
    val magiskLogs = viewModel.logs
    val suLogs = viewModel.items

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
                    title = if (showSuperuserLog) {
                        context.getString(CoreR.string.superuser)
                    } else {
                        context.getString(CoreR.string.logs)
                    },
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
                    },
                    actions = {
                        TextButton(
                            text = context.getString(CoreR.string.menuSaveLog),
                            onClick = { viewModel.saveMagiskLog() }
                        )
                        TextButton(
                            text = context.getString(CoreR.string.menuClearLog),
                            onClick = {
                                if (showSuperuserLog) {
                                    viewModel.clearLog()
                                } else {
                                    viewModel.clearMagiskLog()
                                }
                            }
                        )
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showSuperuserLog = !showSuperuserLog }
                ) {
                    Icon(
                        imageVector = if (showSuperuserLog) {
                            MiuixIcons.Folder
                        } else {
                            MiuixIcons.ListView
                        },
                        contentDescription = null
                    )
                }
            },
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
            content = { paddingValues ->
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
                        showSuperuserLog -> {
                            SuperuserLogContent(
                                suLogs = suLogs,
                                isEmpty = suLogs.isEmpty()
                            )
                        }
                        else -> {
                            MagiskLogContent(
                                logs = magiskLogs,
                                isEmpty = magiskLogs.all { it.item.isEmpty() }
                            )
                        }
                    }
                }
            }
        )
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
 * Magisk 日志内容
 * 水平滚动显示日志
 */
@Composable
private fun MagiskLogContent(
    logs: List<LogRvItem>,
    isEmpty: Boolean
) {
    val context = LocalContext.current

    if (isEmpty) {
        EmptyContent(message = context.getString(CoreR.string.log_data_magisk_none))
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                items(
                    items = logs.filter { it.item.isNotEmpty() }
                ) { logItem ->
                    MagiskLogItem(text = logItem.item)
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

/**
 * 单条 Magisk 日志项
 */
@Composable
private fun MagiskLogItem(
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 8.dp
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            style = MiuixTheme.textStyles.body2.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = MiuixTheme.colorScheme.onSurface
        )
    }
}

/**
 * Superuser 日志内容
 */
@Composable
private fun SuperuserLogContent(
    suLogs: List<SuLogRvItem>,
    isEmpty: Boolean
) {
    val context = LocalContext.current

    if (isEmpty) {
        EmptyContent(message = context.getString(CoreR.string.log_data_none))
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(
                items = suLogs,
                key = { it.log.appName + it.log.time }
            ) { logItem ->
                SuperuserLogItem(
                    logItem = logItem,
                    isTop = logItem.isTop,
                    isBottom = logItem.isBottom
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * 单条 Superuser 日志项
 */
@Composable
private fun SuperuserLogItem(
    logItem: SuLogRvItem,
    isTop: Boolean,
    isBottom: Boolean
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = logItem.log.appName,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val actionText = if (logItem.log.action >= 2) {
                    context.getString(CoreR.string.su_allow_toast, logItem.log.appName)
                } else {
                    context.getString(CoreR.string.su_deny_toast, logItem.log.appName)
                }
                val actionColor = if (logItem.log.action >= 2) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.error
                }

                Text(
                    text = actionText,
                    style = MiuixTheme.textStyles.body2,
                    color = actionColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                color = MiuixTheme.colorScheme.surfaceContainerHigh,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = logItem.info,
                style = MiuixTheme.textStyles.body2.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                color = MiuixTheme.colorScheme.onSurfaceContainer
            )
        }
    }
}

/**
 * 空状态显示
 */
@Composable
private fun EmptyContent(
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MiuixTheme.textStyles.title3,
            color = MiuixTheme.colorScheme.onSurfaceContainer
        )
    }
}
