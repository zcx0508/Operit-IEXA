package com.ai.assistance.operit.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * IEXA 底部输入栏（对齐 iexa-ui-kit/preview.html 的 .inputbar）：
 * 一个 #232527 圆角胶囊，内部从左到右：
 *   [+] 44dp 圆形 → 附件菜单
 *   [⚡] 44dp 圆形 → 闪电菜单（清空 / 压缩 / 记忆 / 思考）
 *   [询问 IEXA] 自适应文本域
 *   [●] 48dp 主色圆 → 语音 / 发送
 *
 * 与 Operit 原生 Agent / Classic 输入栏并列，仅在 IEXA 主题下启用。
 */
@Composable
fun IexaChatInputBar(
        value: TextFieldValue,
        onValueChange: (TextFieldValue) -> Unit,
        placeholder: String,
        isProcessing: Boolean,
        onSend: () -> Unit,
        onCancel: () -> Unit,
        onAttach: () -> Unit,
        onQuickPanel: () -> Unit,
        attachmentCount: Int = 0,
        modifier: Modifier = Modifier,
) {
    val palette = LocalIexaPalette.current
    val canSend = value.text.isNotBlank()

    Row(
            modifier =
                    modifier.fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .background(palette.surfaceAlt, RoundedCornerShape(28.dp))
                            .padding(8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ＋ 附件
        Box(
                modifier =
                        Modifier.size(44.dp)
                                .background(palette.surface, CircleShape)
                                .border(1.dp, palette.border, CircleShape)
                                .clickable(onClick = onAttach),
                contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", color = palette.primaryText, fontSize = 24.sp)
            if (attachmentCount > 0) {
                Box(
                        modifier =
                                Modifier.align(Alignment.TopEnd)
                                        .size(16.dp)
                                        .background(palette.accent, CircleShape),
                        contentAlignment = Alignment.Center,
                ) {
                    Text(
                            text = attachmentCount.toString(),
                            color = palette.onAccent,
                            fontSize = 10.sp,
                    )
                }
            }
        }

        // ⚡ 闪电菜单
        Box(
                modifier =
                        Modifier.size(44.dp)
                                .background(palette.surface, CircleShape)
                                .border(1.dp, palette.border, CircleShape)
                                .clickable(onClick = onQuickPanel),
                contentAlignment = Alignment.Center,
        ) {
            Text(text = "⚡", color = Color(0xFF6D7CF5), fontSize = 20.sp)
        }

        // 文本域
        Box(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                contentAlignment = Alignment.CenterStart,
        ) {
            if (value.text.isEmpty()) {
                Text(
                        text = placeholder,
                        color = palette.mutedText.copy(alpha = 0.75f),
                        fontSize = 17.sp,
                )
            }
            BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle =
                            TextStyle(
                                    color = palette.primaryText,
                                    fontSize = 17.sp,
                            ),
                    cursorBrush = SolidColor(palette.accent),
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
            )
        }

        // ● 语音 / 发送 / 停止
        Box(
                modifier =
                        Modifier.size(48.dp)
                                .background(
                                        if (isProcessing) palette.danger else palette.accent,
                                        CircleShape,
                                )
                                .clickable {
                                    when {
                                        isProcessing -> onCancel()
                                        canSend -> onSend()
                                    }
                                },
                contentAlignment = Alignment.Center,
        ) {
            when {
                isProcessing ->
                        Box(
                                modifier =
                                        Modifier.size(14.dp)
                                                .background(palette.onAccent, RoundedCornerShape(3.dp))
                        )
                canSend ->
                        Text(
                                text = "↑",
                                color = palette.onAccent,
                                fontSize = 22.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        )
                else -> WaveGlyph(color = palette.onAccent)
            }
        }
    }
}

/** 语音图标（参考 .wave 的声波），五根竖条。 */@Composable
private fun WaveGlyph(color: Color) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
    ) {
        listOf(8.dp, 13.dp, 17.dp, 13.dp, 8.dp).forEach { barHeight ->
            Box(
                    modifier =
                            Modifier.width(2.5.dp)
                                    .size(width = 2.5.dp, height = barHeight)
                                    .background(color, CircleShape)
            )
        }
    }
}
