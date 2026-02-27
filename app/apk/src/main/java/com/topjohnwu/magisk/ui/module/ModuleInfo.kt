package com.topjohnwu.magisk.ui.module

import androidx.compose.runtime.Immutable
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.model.module.LocalModule
import com.topjohnwu.magisk.core.model.module.OnlineModule
import com.topjohnwu.magisk.core.R as CoreR
import com.topjohnwu.magisk.utils.TextHolder
import com.topjohnwu.magisk.utils.asText

/**
 * 模块信息数据类
 * 使用 @Immutable 注解标记为不可变，符合 Compose 最佳实践
 */
@Immutable
data class ModuleInfo(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val versionCode: Int,
    val description: String,
    val enabled: Boolean,
    val removed: Boolean,
    val updated: Boolean,
    val updateInfo: OnlineModule?,
    val isZygisk: Boolean,
    val isRiru: Boolean,
    val zygiskUnloaded: Boolean,
    val hasAction: Boolean,
) {
    /**
     * 是否显示警告信息
     */
    val showNotice: Boolean
        get() = zygiskUnloaded ||
            (Info.isZygiskEnabled && isRiru) ||
            (!Info.isZygiskEnabled && isZygisk)

    /**
     * 是否显示 Action 按钮
     */
    val showAction: Boolean
        get() = hasAction && !showNotice

    /**
     * 警告文本
     */
    val noticeText: TextHolder
        get() = when {
            zygiskUnloaded -> CoreR.string.zygisk_module_unloaded.asText()
            isRiru -> CoreR.string.suspend_text_riru.asText(CoreR.string.zygisk.asText())
            else -> CoreR.string.suspend_text_zygisk.asText(CoreR.string.zygisk.asText())
        }

    /**
     * 是否显示更新按钮
     */
    val showUpdate: Boolean
        get() = updateInfo != null

    /**
     * 更新按钮是否可用
     */
    val updateReady: Boolean
        get() = updateInfo != null && !removed && enabled

    /**
     * 是否有可用更新
     */
    val outdated: Boolean
        get() = updateInfo != null

    companion object {
        /**
         * 从 LocalModule 创建 ModuleInfo
         */
        fun from(localModule: LocalModule): ModuleInfo {
            return ModuleInfo(
                id = localModule.id,
                name = localModule.name,
                author = localModule.author,
                version = localModule.version,
                versionCode = localModule.versionCode,
                description = localModule.description,
                enabled = localModule.enable,
                removed = localModule.remove,
                updated = localModule.updated,
                updateInfo = localModule.updateInfo,
                isZygisk = localModule.isZygisk,
                isRiru = localModule.isRiru,
                zygiskUnloaded = localModule.zygiskUnloaded,
                hasAction = localModule.hasAction,
            )
        }
    }
}
