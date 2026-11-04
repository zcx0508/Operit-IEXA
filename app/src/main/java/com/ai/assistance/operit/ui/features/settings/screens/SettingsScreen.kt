package com.ai.assistance.operit.ui.features.settings.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.assistance.operit.ui.theme.LocalIexaPalette
import com.ai.assistance.operit.ui.theme.IexaIconBadge
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.ai.assistance.operit.R
import com.ai.assistance.operit.core.tools.defaultTool.standard.CookiePrivacyManager
import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.repository.ChatHistoryManager
import com.ai.assistance.operit.ui.features.remote.DshRemoteScreen
import com.ai.assistance.operit.ui.features.remote.IexaRemoteScreen
import com.ai.assistance.operit.ui.theme.LocalThemePreferenceSnapshot
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

// 保存滑动状态变量，使其跨重组保持
private val SettingsScreenScrollPosition = mutableStateOf(0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        navigateToUserPreferences: () -> Unit,
        navigateToToolPermissions: () -> Unit,
        navigateToModelConfig: () -> Unit,
        navigateToThemeSettings: () -> Unit,
        navigateToGlobalDisplaySettings: () -> Unit,
        navigateToModelPrompts: () -> Unit,
        navigateToFunctionalConfig: () -> Unit,
        navigateToChatHistorySettings: () -> Unit,
        navigateToChatBackupSettings: () -> Unit,
        navigateToLanguageSettings: () -> Unit,
        navigateToSpeechServicesSettings: () -> Unit,
        navigateToExternalHttpChatSettings: () -> Unit,
        navigateToPersonaCardGeneration: () -> Unit,
        navigateToWaifuModeSettings: () -> Unit,
        navigateToTokenUsageStatistics: () -> Unit,
        navigateToPerformanceMonitor: () -> Unit,
        navigateToContextSummarySettings: () -> Unit,
        navigateToLayoutAdjustmentSettings: () -> Unit,
        // 以下为补齐 Operit 原有能力入口（工具/包/记忆/终端/工作流/文档等）
        navigateToMemory: () -> Unit,
        navigateToPackages: () -> Unit,
        navigateToMarket: () -> Unit,
        navigateToToolbox: () -> Unit,
        navigateToShizuku: () -> Unit,
        navigateToTerminal: () -> Unit,
        navigateToWorkflow: () -> Unit,
        navigateToAgentConfig: () -> Unit,
        navigateToMnnModelDownload: () -> Unit,
        navigateToCustomEmoji: () -> Unit,
        navigateToTagMarket: () -> Unit,
        navigateToHelp: () -> Unit,
        navigateToAgreement: () -> Unit,
        navigateToUpdateHistory: () -> Unit,
        navigateToAbout: () -> Unit
) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var showClearCookieConfirm by remember { mutableStateOf(false) }
        var showRemoteConnect by remember { mutableStateOf(false) }
        var showDshRemote by remember { mutableStateOf(false) }

        // 创建和记住滚动状态，设置为上次保存的位置
        val scrollState = rememberScrollState(SettingsScreenScrollPosition.value)

        // 当滚动状态改变时更新保存的位置
        LaunchedEffect(scrollState) {
                snapshotFlow { scrollState.value }.collect { position ->
                        SettingsScreenScrollPosition.value = position
                }
        }

        val hasBackgroundImage = LocalThemePreferenceSnapshot.current.useBackgroundImage
        val useIexaPalette = LocalThemePreferenceSnapshot.current.iexaPalette

        val cardContainerColor = when {
                hasBackgroundImage -> MaterialTheme.colorScheme.surface
                // IEXA：卡片是不透明纯色 surface（浅色 #FFFFFF / 深色 #1C1C1E）
                useIexaPalette -> LocalIexaPalette.current.surface
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }

        Column(
                modifier = Modifier.fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(scrollState)
        ) {
                // ======= 账号（已移除：本版本不提供账号登录） =======

                // ======= 个性化配置 =======
                SettingsSection(
                        title = stringResource(id = R.string.settings_section_personalization),
                        icon = Icons.Default.Person,
                        containerColor = cardContainerColor
                ) {
                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_user_preferences),
                                subtitle = stringResource(id = R.string.settings_user_preferences_subtitle),
                                icon = Icons.Default.Face,
                                onClick = navigateToUserPreferences
                        )

                        CompactSettingsItem(
                                title = stringResource(R.string.language_settings),
                                subtitle = stringResource(id = R.string.settings_language_subtitle),
                                icon = Icons.Default.Language,
                                onClick = navigateToLanguageSettings
                        )
                        
                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_theme_appearance),
                                subtitle = stringResource(id = R.string.settings_theme_subtitle),
                                icon = Icons.Default.Palette,
                                onClick = navigateToThemeSettings
                        )
                        
                        CompactSettingsItem(
                                title = stringResource(R.string.settings_global_display),
                                subtitle = stringResource(R.string.settings_global_display_subtitle),
                                icon = Icons.Default.Visibility,
                                onClick = navigateToGlobalDisplaySettings
                        )
                        
                        CompactSettingsItem(
                                title = stringResource(R.string.layout_adjustment),
                                subtitle = stringResource(R.string.layout_adjustment_subtitle),
                                icon = Icons.Default.AspectRatio,
                                onClick = navigateToLayoutAdjustmentSettings
                        )
                }

                // ======= AI模型配置 =======
                SettingsSection(
                        title = stringResource(id = R.string.settings_section_ai_model),
                        icon = Icons.Default.Settings,
                        containerColor = cardContainerColor
                ) {
                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_model_parameters),
                                subtitle = stringResource(id = R.string.settings_model_params_subtitle),
                                icon = Icons.Default.Api,
                                onClick = navigateToModelConfig
                        )
                        
                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_functional_model),
                                subtitle = stringResource(id = R.string.settings_functional_model_subtitle),
                                icon = Icons.Default.Tune,
                                onClick = navigateToFunctionalConfig
                        )
                        
                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_speech_services),
                                subtitle = stringResource(id = R.string.settings_speech_services_subtitle),
                                icon = Icons.Default.RecordVoiceOver,
                                onClick = navigateToSpeechServicesSettings
                        )

                        // 本地模型（MNN）：无网络时的兜底推理
                        CompactSettingsItem(
                                title = stringResource(R.string.screen_title_mnn_model_download),
                                subtitle = "本地离线推理模型",
                                icon = Icons.Default.Memory,
                                onClick = navigateToMnnModelDownload
                        )
                        
                }

                // ======= 提示词配置 =======
                SettingsSection(
                        title = stringResource(R.string.settings_prompt_section),
                        icon = Icons.Default.Message,
                        containerColor = cardContainerColor
                ) {
                        CompactSettingsItem(
                                title = stringResource(R.string.settings_prompt_title),
                                subtitle = stringResource(id = R.string.settings_system_prompts_subtitle),
                                icon = Icons.Default.ChatBubble,
                                onClick = navigateToModelPrompts
                        )
                        
                        // 新增：人设卡生成
                        CompactSettingsItem(
                                title = stringResource(R.string.persona_card_generation),
                                subtitle = stringResource(R.string.persona_card_generation_desc),
                                icon = Icons.Default.Face,
                                onClick = navigateToPersonaCardGeneration
                        )
                        
                        // 新增：Waifu模式设置
                        CompactSettingsItem(
                                title = stringResource(R.string.waifu_mode_settings),
                                subtitle = stringResource(R.string.waifu_mode_settings_desc),
                                icon = Icons.Default.EmojiEmotions,
                                onClick = navigateToWaifuModeSettings
                        )

                        // 智能体（角色 / 助手配置）
                        CompactSettingsItem(
                                title = "智能体",
                                subtitle = "管理助手角色与工作模式",
                                icon = Icons.Default.SmartToy,
                                onClick = navigateToAgentConfig
                        )
                }

                // ======= 能力（记忆 / 包 / 工具） =======
                SettingsSection(
                        title = "能力",
                        icon = Icons.Default.Extension,
                        containerColor = cardContainerColor
                ) {
                        CompactSettingsItem(
                                title = stringResource(R.string.screen_title_memory_base),
                                subtitle = "长期记忆 · 画像 / 偏好 / 日记",
                                icon = Icons.Default.Psychology,
                                onClick = navigateToMemory
                        )

                        CompactSettingsItem(
                                title = "包管理",
                                subtitle = "已安装的工具包 / 技能 / MCP 服务",
                                icon = Icons.Default.Inventory2,
                                onClick = navigateToPackages
                        )

                        CompactSettingsItem(
                                title = stringResource(R.string.screen_title_market),
                                subtitle = "浏览、订阅与发布包",
                                icon = Icons.Default.Storefront,
                                onClick = navigateToMarket
                        )

                        CompactSettingsItem(
                                title = stringResource(R.string.manage_custom_emoji),
                                subtitle = "管理 AI 可用的自定义表情",
                                icon = Icons.Default.EmojiEmotions,
                                onClick = navigateToCustomEmoji
                        )

                        CompactSettingsItem(
                                title = stringResource(R.string.screen_title_tag_market),
                                subtitle = "按标签查找角色卡与工具包",
                                icon = Icons.Default.Sell,
                                onClick = navigateToTagMarket
                        )

                        CompactSettingsItem(
                                title = stringResource(R.string.nav_toolbox),
                                subtitle = "文件管理 / Logcat / FFmpeg / 自动化等系统工具",
                                icon = Icons.Default.Handyman,
                                onClick = navigateToToolbox
                        )

                        CompactSettingsItem(
                                title = stringResource(R.string.shizuku_commands),
                                subtitle = "授权后可用系统级 shell 命令",
                                icon = Icons.Default.Terminal,
                                onClick = navigateToShizuku
                        )
                }

                // ======= 运行（终端 / 工作流） =======
                SettingsSection(
                        title = "运行",
                        icon = Icons.Default.PlayCircle,
                        containerColor = cardContainerColor
                ) {
                        CompactSettingsItem(
                                title = stringResource(R.string.screen_title_terminal),
                                subtitle = "本地 shell 与命令执行",
                                icon = Icons.Default.Terminal,
                                onClick = navigateToTerminal
                        )

                        CompactSettingsItem(
                                title = stringResource(R.string.nav_workflow),
                                subtitle = "可视化流程编排",
                                icon = Icons.Default.AccountTree,
                                onClick = navigateToWorkflow
                        )
                }

                // ======= 上下文和总结设置 =======
                SettingsSection(
                        title = stringResource(id = R.string.settings_section_context_summary),
                        icon = Icons.Default.Analytics,
                        containerColor = cardContainerColor
                ) {
                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_section_context_summary),
                                subtitle = stringResource(id = R.string.settings_context_summary_subtitle),
                                icon = Icons.Default.Tune,
                                onClick = navigateToContextSummarySettings
                        )
                }

                // ======= 数据和权限 =======
                SettingsSection(
                        title = stringResource(id = R.string.settings_data_permissions),
                        icon = Icons.Default.Security,
                        containerColor = cardContainerColor
                ) {
                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_tool_permissions),
                                subtitle = stringResource(id = R.string.settings_tool_permissions_subtitle),
                                icon = Icons.Default.AdminPanelSettings,
                                onClick = navigateToToolPermissions
                        )

                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_data_backup),
                                subtitle = stringResource(id = R.string.settings_data_backup_desc),
                                icon = Icons.Default.CloudUpload,
                                onClick = navigateToChatBackupSettings
                        )
                        
                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_chat_history_management),
                                subtitle = stringResource(id = R.string.settings_chat_history_management_subtitle),
                                icon = Icons.Default.ManageHistory,
                                onClick = navigateToChatHistorySettings
                        )

                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_token_usage_stats),
                                subtitle = stringResource(id = R.string.settings_token_usage_subtitle),
                                icon = Icons.Default.Analytics,
                                onClick = navigateToTokenUsageStatistics
                        )

                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_performance_monitor),
                                subtitle = stringResource(id = R.string.settings_performance_monitor_subtitle),
                                icon = Icons.Default.Speed,
                                onClick = navigateToPerformanceMonitor
                        )
                }

                // ======= 隐私与数据清理 =======
                SettingsSection(
                        title = stringResource(id = R.string.settings_privacy_data_cleanup),
                        icon = Icons.Default.DeleteSweep,
                        containerColor = cardContainerColor
                ) {
                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_clear_cookies),
                                subtitle = stringResource(id = R.string.settings_clear_cookies_subtitle),
                                icon = Icons.Default.DeleteSweep,
                                onClick = { showClearCookieConfirm = true }
                        )
                }

                // ======= 外部调用 =======
                SettingsSection(
                        title = stringResource(id = R.string.settings_section_external_calls),
                        icon = Icons.Default.SettingsEthernet,
                        containerColor = cardContainerColor
                ) {
                        CompactSettingsItem(
                                title = stringResource(id = R.string.settings_external_http_chat),
                                subtitle = stringResource(id = R.string.settings_external_http_chat_subtitle),
                                icon = Icons.Default.SettingsEthernet,
                                onClick = navigateToExternalHttpChatSettings
                        )
                }

                // ======= 远程连接（电脑端 IEXA） =======
                SettingsSection(
                        title = "远程连接",
                        icon = Icons.Default.Devices,
                        containerColor = cardContainerColor
                ) {
                        CompactSettingsItem(
                                title = "IEXA 远程",
                                subtitle = "配对电脑端 IEXA，在任意网络远程操控",
                                icon = Icons.Default.Devices,
                                onClick = { showRemoteConnect = true }
                        )

                        CompactSettingsItem(
                                title = "DeepSeek Harness 远程",
                                subtitle = "在应用内打开电脑端 DeepSeek Harness 远程界面",
                                icon = Icons.Default.Computer,
                                onClick = { showDshRemote = true }
                        )
                }

                // ======= 其它（帮助 / 协议 / 更新 / 关于） =======
                SettingsSection(
                        title = "其它",
                        icon = Icons.Default.Info,
                        containerColor = cardContainerColor
                ) {
                        CompactSettingsItem(
                                title = stringResource(R.string.nav_help),
                                subtitle = "使用说明与常见问题",
                                icon = Icons.Default.HelpOutline,
                                onClick = navigateToHelp
                        )

                        CompactSettingsItem(
                                title = stringResource(R.string.agreement_title),
                                subtitle = "使用前请阅读并同意",
                                icon = Icons.Default.Description,
                                onClick = navigateToAgreement
                        )

                        CompactSettingsItem(
                                title = stringResource(R.string.update_history),
                                subtitle = "查看每个版本的改动",
                                icon = Icons.Default.History,
                                onClick = navigateToUpdateHistory
                        )

                        CompactSettingsItem(
                                title = stringResource(R.string.nav_about),
                                subtitle = "版本信息与开源许可",
                                icon = Icons.Default.Info,
                                onClick = navigateToAbout
                        )
                }

                // 底部间距
                Spacer(modifier = Modifier.height(16.dp))
        }

        if (showRemoteConnect) {
                IexaRemoteScreen(onClose = { showRemoteConnect = false })
        }

        if (showDshRemote) {
                DshRemoteScreen(onClose = { showDshRemote = false })
        }

        if (showClearCookieConfirm) {
                AlertDialog(
                        onDismissRequest = { showClearCookieConfirm = false },
                        title = { Text(stringResource(R.string.clear_cookies_dialog_title)) },
                        text = { Text(stringResource(R.string.clear_cookies_dialog_message)) },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                showClearCookieConfirm = false
                                                scope.launch {
                                                        try {
                                                                CookiePrivacyManager.clearAllCookies()
                                                                Toast.makeText(
                                                                        context,
                                                                        context.getString(R.string.clear_cookies_success),
                                                                        Toast.LENGTH_SHORT
                                                                ).show()
                                                        } catch (e: Exception) {
                                                                AppLogger.e("SettingsScreen", "Failed to clear cookies", e)
                                                                Toast.makeText(
                                                                        context,
                                                                        context.getString(R.string.clear_cookies_failed),
                                                                        Toast.LENGTH_SHORT
                                                                ).show()
                                                        }
                                                }
                                        }
                                ) {
                                        Text(stringResource(R.string.clear_cookies_confirm))
                                }
                        },
                        dismissButton = {
                                TextButton(onClick = { showClearCookieConfirm = false }) {
                                        Text(stringResource(android.R.string.cancel))
                                }
                        }
                )
        }
}
@Composable
private fun SettingsSection(
        title: String,
        icon: ImageVector,
        containerColor: Color,
        content: @Composable ColumnScope.() -> Unit
) {
        val useIexa = LocalThemePreferenceSnapshot.current.iexaPalette
        val iexaPalette = LocalIexaPalette.current
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                if (useIexa) {
                        // IEXA 分组小标题：全大写、12sp、字距 1sp、mutedText，无图标
                        Text(
                                text = title.uppercase(),
                                fontSize = 12.sp,
                                letterSpacing = 1.sp,
                                color = iexaPalette.mutedText,
                                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                        )
                } else {
                        // 分组标题
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                                Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                )
                        }
                }

                // 内容区域
                // IEXA：12dp 圆角 + 1px 描边卡片（preview.html .setting-card），
                // 内边距交给每行自己（行与行之间有分隔线），非 IEXA 保持原来的 12dp 内边距。
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = if (useIexa) RoundedCornerShape(12.dp) else CardDefaults.shape,
                        border = if (useIexa) BorderStroke(1.dp, iexaPalette.border) else null,
                        colors = CardDefaults.cardColors(
                                containerColor = containerColor
                        )
                ) {
                        Column(
                                modifier = Modifier.padding(if (useIexa) 0.dp else 12.dp),
                                content = content
                        )
                }
        }
}

@Composable
private fun CompactSettingsItem(
        title: String,
        subtitle: String,
        icon: ImageVector,
        onClick: () -> Unit
) {
        val useIexa = LocalThemePreferenceSnapshot.current.iexaPalette
        if (useIexa) {
                val palette = LocalIexaPalette.current
                // IEXA 设置行（preview.html .setting-row）：32dp 图标色块 + 15sp 标题
                // + 12sp 副标题 + 右侧尖括号，行间 1px 分隔线
                Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onClick() }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // 每项一个渐变圆角块 + 白色字形（配色按标题哈希稳定分配）
                                IexaIconBadge(
                                        icon = icon,
                                        seed = title,
                                        size = 32.dp,
                                        iconSize = 17.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = palette.primaryText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                                text = subtitle,
                                                fontSize = 12.sp,
                                                color = palette.mutedText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                }
                                Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = palette.mutedText,
                                        modifier = Modifier.size(18.dp)
                                )
                        }
                        HorizontalDivider(thickness = 1.dp, color = palette.border)
                }
                return
        }

        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onClick() }
                        .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                        Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }
                
                Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                )
        }
}
