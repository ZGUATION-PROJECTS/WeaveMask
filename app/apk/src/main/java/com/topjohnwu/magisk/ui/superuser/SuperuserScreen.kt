package com.topjohnwu.magisk.ui.superuser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.topjohnwu.magisk.core.R as CoreR
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 超级用户页面
 * 使用 Compose 实现超级用户授权管理界面
 *
 * @param viewModel 超级用户 ViewModel
 * @param bottomPadding 底部内边距，用于避免内容被底部导航栏遮挡
 * @param modifier Modifier
 */
@Composable
fun SuperuserScreen(
    viewModel: SuperuserViewModel,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.surface,
        tint = HazeTint(MiuixTheme.colorScheme.surface.copy(0.8f))
    )

    val loading = viewModel.loading
    val policies = viewModel.itemsPolicies

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
                    title = context.getString(CoreR.string.superuser)
                )
            },
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
                        policies.isEmpty() -> {
                            EmptyContent()
                        }
                        else -> {
                            PolicyList(
                                policies = policies,
                                viewModel = viewModel,
                                bottomPadding = bottomPadding
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
            text = context.getString(CoreR.string.superuser_policy_none),
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 策略列表
 *
 * @param policies 策略列表
 * @param viewModel 超级用户 ViewModel
 * @param bottomPadding 底部内边距
 */
@Composable
private fun PolicyList(
    policies: List<PolicyRvItem>,
    viewModel: SuperuserViewModel,
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
            items = policies,
            key = { it.item.uid }
        ) { policyItem ->
            PolicyItem(
                item = policyItem,
                onDelete = { viewModel.deletePressed(policyItem) },
                onUpdateNotify = { viewModel.updateNotify(policyItem) },
                onUpdateLogging = { viewModel.updateLogging(policyItem) },
                onUpdatePolicy = { policy -> viewModel.updatePolicy(policyItem, policy) }
            )
        }

        // 底部间距 - 使用传入的 bottomPadding 确保最后一个卡片内容可以正常显示
        item {
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

/**
 * 单个策略项组件
 */
@Composable
private fun PolicyItem(
    item: PolicyRvItem,
    onDelete: () -> Unit,
    onUpdateNotify: () -> Unit,
    onUpdateLogging: () -> Unit,
    onUpdatePolicy: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val policy = item.item
    val appName = item.appName
    val packageName = item.packageName
    val isEnabled = item.isEnabled

    val cardAlpha = if (isEnabled) 1f else 0.5f

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.icon?.let { drawable ->
                    androidx.compose.foundation.Image(
                        bitmap = drawable.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = appName,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = packageName,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onUpdatePolicy(if (policy.policy >= 2) 1 else 2) },
                        colors = if (policy.policy >= 2) {
                            ButtonDefaults.buttonColorsPrimary()
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        enabled = isEnabled
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Ok,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (policy.policy >= 2) {
                                context.getString(CoreR.string.grant)
                            } else {
                                context.getString(CoreR.string.deny)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(CoreR.string.superuser_toggle_notification),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = item.shouldNotify,
                        enabled = isEnabled,
                        onCheckedChange = { onUpdateNotify() }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(CoreR.string.superuser_toggle_revoke),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = item.shouldLog,
                        enabled = isEnabled,
                        onCheckedChange = { onUpdateLogging() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = context.getString(CoreR.string.su_revoke_title))
            }
        }
    }
}
