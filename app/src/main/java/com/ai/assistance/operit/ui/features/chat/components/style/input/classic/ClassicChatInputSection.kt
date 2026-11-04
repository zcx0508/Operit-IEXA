package com.ai.assistance.operit.ui.features.chat.components.style.input.classic

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.assistance.operit.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.ai.assistance.operit.data.model.AttachmentInfo
import com.ai.assistance.operit.data.model.InputProcessingState
import com.ai.assistance.operit.data.model.ChatMessage
import com.ai.assistance.operit.core.tools.ToolProgressBus
import com.ai.assistance.operit.ui.common.animations.SimpleAnimatedVisibility
import com.ai.assistance.operit.ui.features.chat.components.AttachmentChip
import com.ai.assistance.operit.ui.features.chat.components.AttachmentSelectorPanel
import com.ai.assistance.operit.ui.features.chat.components.FullscreenInputDialog
import com.ai.assistance.operit.ui.features.chat.components.style.input.common.rememberMentionVisualTransformation
import com.ai.assistance.operit.ui.features.chat.components.SimpleLinearProgressIndicator
import com.ai.assistance.operit.ui.features.chat.components.style.input.common.PendingMessageQueuePanel
import com.ai.assistance.operit.ui.features.chat.components.style.input.common.PendingQueueMessageItem
import com.ai.assistance.operit.ui.features.chat.viewmodel.ChatViewModel
import com.ai.assistance.operit.ui.floating.FloatingMode
import com.ai.assistance.operit.ui.theme.LocalThemePreferenceSnapshot
import com.ai.assistance.operit.ui.theme.isLiquidGlassSupported
import com.ai.assistance.operit.ui.theme.isWaterGlassSupported
import com.ai.assistance.operit.ui.theme.liquidGlass
import com.ai.assistance.operit.ui.theme.waterGlass
import com.ai.assistance.operit.util.ChatUtils
import androidx.compose.ui.res.stringResource
import android.net.Uri

@Composable
fun ClassicChatInputSection(
    actualViewModel: ChatViewModel,
    userMessage: TextFieldValue,
    onUserMessageChange: (TextFieldValue) -> Unit,
    enableEnterToSend: Boolean = false,
    onSendMessage: () -> Unit,
    onQueueMessage: () -> Unit,
    onCancelMessage: () -> Unit,
    isLoading: Boolean,
    inputState: InputProcessingState = InputProcessingState.Idle,
    allowTextInputWhileProcessing: Boolean = false,
    onAttachmentRequest: (String) -> Unit = {},
    attachments: List<AttachmentInfo> = emptyList(),
    onRemoveAttachment: (String) -> Unit = {},
    onInsertAttachment: (AttachmentInfo) -> Unit = {},
    onAttachScreenContent: () -> Unit = {},
    onAttachNotifications: () -> Unit = {},
    onAttachLocation: () -> Unit = {},
    onAttachMemory: () -> Unit = {},
    onAttachPackage: (String) -> Unit = {},
    onTakePhoto: (Uri) -> Unit,
    hasBackgroundImage: Boolean = false,
    chatInputTransparent: Boolean = false,
    chatInputFloating: Boolean = false,
    chatInputLiquidGlass: Boolean = false,
    chatInputWaterGlass: Boolean = false,
    modifier: Modifier = Modifier,
    externalAttachmentPanelState: Boolean? = null,
    onAttachmentPanelStateChange: ((Boolean) -> Unit)? = null,
    showInputProcessingStatus: Boolean = true,
    enableTools: Boolean = true,
    replyToMessage: ChatMessage? = null, // 回复目标消息
    onClearReply: (() -> Unit)? = null, // 清除回复状态的回调
    isWorkspaceOpen: Boolean = false,
    pendingQueueMessages: List<PendingQueueMessageItem> = emptyList(),
    isPendingQueueExpanded: Boolean = true,
    onPendingQueueExpandedChange: (Boolean) -> Unit = {},
    onDeletePendingQueueMessage: (Long) -> Unit = {},
    onEditPendingQueueMessage: (Long) -> Unit = {},
    onSendPendingQueueMessage: (Long) -> Unit = {},
    onQuickPanel: () -> Unit = {}
) {
    val showTokenLimitDialog = remember { mutableStateOf(false) }
    val showFullscreenInput = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isProcessing =
        isLoading ||
            inputState is InputProcessingState.Connecting ||
            inputState is InputProcessingState.ExecutingTool ||
            inputState is InputProcessingState.ToolProgress ||
            inputState is InputProcessingState.Processing ||
            inputState is InputProcessingState.ProcessingToolResult ||
            inputState is InputProcessingState.Summarizing ||
            inputState is InputProcessingState.Receiving

    if (showTokenLimitDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showTokenLimitDialog.value = false
            },
            title = { Text(context.getString(R.string.token_limit_warning)) },
            text = { Text(context.getString(R.string.token_limit_warning_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTokenLimitDialog.value = false
                        onSendMessage()
                    }
                ) { Text(context.getString(R.string.continue_send)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTokenLimitDialog.value = false
                    },
                ) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
    // IEXA 三档字号里的「聊天输入」档：输入框字号独立缩放
    val chatInputFontScale = LocalThemePreferenceSnapshot.current.fontScaleChatInput
    val modernTextStyle =
        remember(chatInputFontScale) {
            val scale = if (chatInputFontScale > 0f) chatInputFontScale else 1f
            // 基线 14sp：与消息正文 bodyMedium 对齐（IEXA 输入区与气泡同字号）
            TextStyle(fontSize = (14f * scale).sp, lineHeight = (18f * scale).sp)
        }
    val mentionVisualTransformation = rememberMentionVisualTransformation(modernTextStyle)
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val toolProgressEvent by ToolProgressBus.progress.collectAsState()

    // Token limit calculation
    val currentWindowSize by actualViewModel.currentWindowSize.collectAsState()
    val maxWindowSizeInK by actualViewModel.maxWindowSizeInK.collectAsState()
    val maxTokens = (maxWindowSizeInK * 1024).toLong().coerceAtLeast(0L)
    val userMessageTokens = remember(userMessage.text) { ChatUtils.estimateTokenCount(userMessage.text) }
    val projectedTokens = userMessageTokens.toLong() + currentWindowSize

    val isOverTokenLimit =
        if (maxTokens > 0) {
            projectedTokens > maxTokens
        } else {
            false
        }

    val hasDraftText = userMessage.text.isNotBlank()
    val canSendMessage = hasDraftText || attachments.isNotEmpty()
    val showQueueAction = isProcessing && hasDraftText
    val showCancelAction = isProcessing && !showQueueAction
    val sendButtonEnabled =
        when {
            isProcessing -> true // Queue / Cancel button
            canSendMessage -> true // Send button is always enabled if there's content
            else -> true // Mic button
        }

    val voicePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                actualViewModel.launchFloatingModeIn(
                    FloatingMode.FULLSCREEN,
                    colorScheme,
                    typography
                )
            } else {
                actualViewModel.showToast(context.getString(R.string.microphone_permission_denied_toast))
            }
        }

    // 控制附件面板的展开状态 - 使用外部状态或本地状态
    val (showAttachmentPanel, setShowAttachmentPanel) =
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(
                externalAttachmentPanelState ?: false
            )
        }

    // 当外部状态变化时更新本地状态
    androidx.compose.runtime.LaunchedEffect(externalAttachmentPanelState) {
        externalAttachmentPanelState?.let { setShowAttachmentPanel(it) }
    }

    // 当本地状态改变时通知外部
    androidx.compose.runtime.LaunchedEffect(showAttachmentPanel) {
        onAttachmentPanelStateChange?.invoke(showAttachmentPanel)
    }
    fun handleEnterSendAction() {
        if (!canSendMessage) return
        if (showQueueAction) {
            onQueueMessage()
            setShowAttachmentPanel(false)
            return
        }
        if (isOverTokenLimit) {
            showTokenLimitDialog.value = true
            return
        }
        onSendMessage()
        setShowAttachmentPanel(false)
    }

    val surfaceColor = when {
        chatInputTransparent -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        hasBackgroundImage -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.surface
    }
    val queueContainerColor = when {
        chatInputTransparent -> MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        else -> surfaceColor
    }
    val queueItemColor = when {
        chatInputTransparent -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        hasBackgroundImage -> MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        else -> MaterialTheme.colorScheme.surface
    }
    val inputLiquidGlassEnabled =
        chatInputTransparent && chatInputLiquidGlass && !chatInputWaterGlass && isLiquidGlassSupported()
    val inputWaterGlassEnabled =
        chatInputTransparent && chatInputWaterGlass && isWaterGlassSupported()
    val containerShape = if (chatInputFloating) RoundedCornerShape(22.dp) else RoundedCornerShape(0.dp)
    val containerModifier =
        if (chatInputFloating) {
            modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        } else {
            modifier
        }

    // 玻璃层单独做成兄弟节点，不再和输入区内容挂在同一个 Box 上。
    // 原因：liquidGlass 的高斯模糊按「节点每次绘制」重算，而输入区里有个进度条动画（AI 工作时一直跑），
    // 玻璃和内容同节点时整条输入框每帧判脏、每帧重跑一次 16~20dp 高斯模糊
    //（simpleperf 实测 AI 思考期间 RenderThread 占 63% CPU，热点就是高斯模糊 + 每帧 GPU 缓冲分配）。
    // 拆开后玻璃节点只随输入框尺寸变化重算。
    val inputGlassEnabled = inputLiquidGlassEnabled || inputWaterGlassEnabled

    Box(
        modifier =
            containerModifier
                .then(
                    if (chatInputFloating && !inputGlassEnabled) {
                        Modifier.shadow(4.dp, containerShape, clip = false)
                    } else {
                        Modifier
                    }
                ),
    ) {
        if (inputGlassEnabled) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .waterGlass(
                            enabled = inputWaterGlassEnabled,
                            shape = containerShape,
                            containerColor = MaterialTheme.colorScheme.surface,
                            shadowElevation = if (chatInputFloating) 10.dp else 14.dp,
                            borderWidth = 0.7.dp,
                            overlayAlphaBoost = if (chatInputFloating) 0.04f else 0.08f,
                        )
                        .liquidGlass(
                            enabled = inputLiquidGlassEnabled,
                            shape = containerShape,
                            containerColor = MaterialTheme.colorScheme.surface,
                            shadowElevation = if (chatInputFloating) 10.dp else 14.dp,
                            borderWidth = 0.42.dp,
                            blurRadius = if (chatInputFloating) 16.dp else 20.dp,
                            overlayAlphaBoost = if (chatInputFloating) 0.06f else 0.10f,
                        )
            )
        }

        Box(
            modifier =
                Modifier
                    .clip(containerShape)
                    .background(
                        if (inputGlassEnabled) {
                            Color.Transparent
                        } else {
                            surfaceColor
                        }
                    ),
        ) {
        Column {
            // Reply preview section
            replyToMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = context.getString(R.string.reply_message),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        val previewText = message.content
                            .replace(Regex("<[^>]*>"), "") // 移除XML标签
                            .trim()
                            .let { if (it.length > 50) it.take(50) + "..." else it }
                        Text(
                            text = "${stringResource(R.string.reply_message)}: $previewText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(
                            onClick = { onClearReply?.invoke() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = context.getString(R.string.cancel_reply),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            PendingMessageQueuePanel(
                queuedMessages = pendingQueueMessages,
                expanded = isPendingQueueExpanded,
                onExpandedChange = onPendingQueueExpandedChange,
                onDeleteMessage = onDeletePendingQueueMessage,
                onEditMessage = onEditPendingQueueMessage,
                onSendMessage = onSendPendingQueueMessage,
                containerColor = queueContainerColor,
                itemColor = queueItemColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Input processing indicator
            SimpleAnimatedVisibility(
                visible =
                    showInputProcessingStatus &&
                        inputState !is InputProcessingState.Idle &&
                        inputState !is InputProcessingState.Completed &&
                        inputState !is InputProcessingState.Error
            ) {
                val (progressColor, baseMessage) = when (inputState) {
                    is InputProcessingState.Connecting -> MaterialTheme.colorScheme.tertiary to inputState.message
                    is InputProcessingState.ExecutingTool -> MaterialTheme.colorScheme.secondary to context.getString(R.string.executing_tool, inputState.toolName)
                    is InputProcessingState.ToolProgress -> MaterialTheme.colorScheme.secondary to inputState.message
                    is InputProcessingState.Processing -> MaterialTheme.colorScheme.primary to inputState.message
                    is InputProcessingState.ProcessingToolResult -> MaterialTheme.colorScheme.tertiary.copy(
                        alpha = 0.8f
                    ) to context.getString(R.string.processing_tool_result, inputState.toolName)
                    is InputProcessingState.Summarizing -> MaterialTheme.colorScheme.tertiary to inputState.message
                    is InputProcessingState.Receiving -> MaterialTheme.colorScheme.secondary to inputState.message
                    else -> MaterialTheme.colorScheme.primary to ""
                }

                var message = baseMessage
                var progressValue = when (inputState) {
                    is InputProcessingState.Processing -> 0.3f
                    is InputProcessingState.Connecting -> 0.6f
                    is InputProcessingState.Summarizing -> 0.05f
                    is InputProcessingState.ToolProgress -> inputState.progress
                    else -> 1f
                }

                if (inputState is InputProcessingState.ExecutingTool) {
                    val event = toolProgressEvent
                    if (event != null && inputState.toolName.contains(event.toolName)) {
                        progressValue = event.progress
                        if (event.message.isNotBlank()) {
                            message = event.message
                        }
                    }
                }

                if (inputState is InputProcessingState.Summarizing) {
                    val event = toolProgressEvent
                    if (event != null && event.toolName == ToolProgressBus.SUMMARY_PROGRESS_TOOL_NAME) {
                        progressValue = event.progress
                        if (event.message.isNotBlank()) {
                            message = event.message
                        }
                    }
                }

                SimpleLinearProgressIndicator(
                    progress = progressValue,
                    modifier = Modifier.fillMaxWidth(),
                    color = progressColor
                )

                if (message.isNotBlank()) {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color =
                            MaterialTheme.colorScheme.onSurface
                                .copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }


            // Attachment chips row - only show if there are attachments
            if (attachments.isNotEmpty()) {
                LazyRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 0.dp,
                                bottom = 6.dp
                            ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    items(attachments) { attachment ->
                        AttachmentChip(
                            attachmentInfo = attachment,
                            onRemove = {
                                onRemoveAttachment(
                                    attachment.filePath
                                )
                            },
                            onInsert = {
                                onInsertAttachment(attachment)
                            }
                        )
                    }
                }
            }

            val classicInputRowHorizontalPadding = if (chatInputFloating) 12.dp else 16.dp
            val classicInputRowVerticalPadding = if (chatInputFloating) 6.dp else 8.dp

            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = classicInputRowHorizontalPadding)
                    .padding(top = classicInputRowVerticalPadding, bottom = classicInputRowVerticalPadding)
                    .wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Input field (保持原有高度)

                val classicInputShape = RoundedCornerShape(22.dp)
                val classicInputEnabled = !isProcessing || allowTextInputWhileProcessing
                val classicInputBorderColor =
                    if (userMessage.text.isNotBlank()) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)
                    }

                BasicTextField(
                    value = userMessage,
                    onValueChange = onUserMessageChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 30.dp)
                        .onPreviewKeyEvent { keyEvent ->
                            if (!enableEnterToSend) {
                                false
                            } else if (
                                keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.key == Key.Enter &&
                                !keyEvent.isShiftPressed
                            ) {
                                handleEnterSendAction()
                                true
                            } else {
                                false
                            }
                        },
                    textStyle = modernTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = mentionVisualTransformation,
                    maxLines = 5,
                    minLines = 1,
                    keyboardOptions =
                    KeyboardOptions(imeAction = if (enableEnterToSend) ImeAction.Send else ImeAction.Default),
                    keyboardActions =
                    if (enableEnterToSend) {
                        KeyboardActions(onSend = { handleEnterSendAction() })
                    } else {
                        KeyboardActions()
                    },
                    enabled = classicInputEnabled,
                    decorationBox = { innerTextField ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = classicInputBorderColor,
                                        shape = classicInputShape,
                                    )
                                    .clip(classicInputShape)
                                    .background(
                                        if (inputLiquidGlassEnabled || inputWaterGlassEnabled) {
                                            Color.Transparent
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(end = 6.dp, top = 7.dp, bottom = 7.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (userMessage.text.isEmpty()) {
                                    Text(
                                        text =
                                            if (isWorkspaceOpen) {
                                                context.getString(R.string.input_question_with_workspace)
                                            } else {
                                                context.getString(R.string.input_question_hint)
                                            },
                                        style = modernTextStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            }

                            IconButton(
                                onClick = { showFullscreenInput.value = true },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = stringResource(R.string.chat_fullscreen_input),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    },
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Attachment button (+ 按钮) - 确保圆形

                Box(
                    modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (showAttachmentPanel)
                                MaterialTheme.colorScheme
                                    .primary
                            else
                                MaterialTheme.colorScheme
                                    .surfaceVariant
                        )
                        .clickable(
                            enabled = true,
                            onClick = {
                                setShowAttachmentPanel(
                                    !showAttachmentPanel
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = context.getString(R.string.add_attachment),
                        tint =
                        if (showAttachmentPanel)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 快捷面板按钮（⚡：清空/压缩/记忆/思考，对齐 reference 输入栏）
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onQuickPanel),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Send button (发送按钮) - 确保圆形
                Box(
                    modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                showCancelAction ->
                                    MaterialTheme
                                        .colorScheme
                                        .error
                                showQueueAction ->
                                    MaterialTheme.colorScheme.tertiary

                                canSendMessage ->
                                    if (isOverTokenLimit)
                                        MaterialTheme.colorScheme.secondary // Warning color
                                    else
                                        MaterialTheme.colorScheme.primary

                                else ->
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            }
                        )
                        .clickable(
                            enabled = sendButtonEnabled,
                            onClick = {
                                when {
                                    showCancelAction ->
                                        onCancelMessage()
                                    showQueueAction -> {
                                        onQueueMessage()
                                        setShowAttachmentPanel(false)
                                    }

                                    canSendMessage -> {
                                        if (isOverTokenLimit) {
                                            showTokenLimitDialog.value = true
                                        } else {
                                            onSendMessage()
                                            // 发送消息后关闭附件面板
                                            setShowAttachmentPanel(
                                                false
                                            )
                                        }
                                    }

                                    else -> {
                                        actualViewModel.onFloatingButtonClick(
                                            FloatingMode.FULLSCREEN,
                                            voicePermissionLauncher,
                                            colorScheme,
                                            typography
                                        )
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconTint =
                        when {
                            showCancelAction -> MaterialTheme.colorScheme.onError
                            showQueueAction -> MaterialTheme.colorScheme.onTertiary
                            canSendMessage ->
                                if (isOverTokenLimit)
                                    MaterialTheme.colorScheme.onSecondary
                                else
                                    MaterialTheme.colorScheme.onPrimary

                            else -> MaterialTheme.colorScheme.onPrimary
                        }
                    Icon(
                        imageVector =
                        when {
                            showCancelAction -> Icons.Default.Close
                            showQueueAction -> Icons.Default.Add
                            canSendMessage -> Icons.AutoMirrored.Filled.Send
                            else -> Icons.Default.Mic
                        },
                        contentDescription =
                        when {
                            showCancelAction -> context.getString(R.string.cancel)
                            showQueueAction -> context.getString(R.string.chat_queue_add_message)
                            canSendMessage -> context.getString(R.string.send)
                            else -> context.getString(R.string.voice_input)
                        },
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

            }



            // Token limit warning
            if (isOverTokenLimit && canSendMessage && !showQueueAction) {
                Text(
                    text =
                    context.getString(R.string.token_limit_exceeded_message, projectedTokens, maxTokens),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 4.dp)
                )
            }

            AttachmentSelectorPanel(
                visible = showAttachmentPanel,
                onAttachImage = { filePath ->
                    onAttachmentRequest(filePath)
                },
                onAttachFile = { filePath ->
                    onAttachmentRequest(filePath)
                },
                onAttachScreenContent = onAttachScreenContent,
                onAttachNotifications = onAttachNotifications,
                onAttachLocation = onAttachLocation,
                onAttachMemory = onAttachMemory,
                onAttachPackage = onAttachPackage,
                onTakePhoto = onTakePhoto,
                userQuery = userMessage.text,
                onDismiss = { setShowAttachmentPanel(false) }
            )

            if (showFullscreenInput.value) {
                FullscreenInputDialog(
                    value = userMessage,
                    onValueChange = onUserMessageChange,
                    onDismiss = { showFullscreenInput.value = false },
                    onConfirm = { showFullscreenInput.value = false }
                )
            }

        }
        }
    }
}
