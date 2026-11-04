package com.ai.assistance.operit.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * IEXA UI Kit v2 组件移植（来源：iexa-ui-kit/components/IexaComponents.kt）。
 *
 * 与原版的差异（结合 Operit 实际情况）：
 * - 原版用 PathParser 画大脑/灯泡图标，这里换成 Material 图标（Psychology / TipsAndUpdates），
 *   避免依赖 PathParser API 版本，观感一致。
 * - 原版 hover 相关动画（悬浮上移、高光扫过）在触屏上没有意义，这里只保留按下缩放 + 描边变色。
 */

/** IEXA 开关：46×26 圆角轨道 + 白色滑块，开启时轨道变 accent，滑块弹性滑动。 */
@Composable
fun IexaToggle(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
) {
    val p = LocalIexaPalette.current
    val trackColor by
            animateColorAsState(
                    targetValue = if (checked) p.accent else p.border,
                    animationSpec = tween(250),
                    label = "toggleTrack",
            )
    val thumbOffset by
            animateFloatAsState(
                    targetValue = if (checked) 1f else 0f,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "toggleThumb",
            )
    Box(
            modifier =
                    modifier.size(width = 46.dp, height = 26.dp)
                            .clip(CircleShape)
                            .background(trackColor)
                            .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                            ) {
                                onCheckedChange(!checked)
                            }
    ) {
        Box(
                modifier =
                        Modifier.size(20.dp)
                                .offset(x = (3 + thumbOffset * 20).dp, y = 3.dp)
                                .background(Color.White, CircleShape)
        )
    }
}

/** 图标呼吸脉动容器：开启时做 scale 呼吸循环。 */
@Composable
private fun PulseIcon(
        on: Boolean,
        content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by
            transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.12f,
                    animationSpec =
                            infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse,
                            ),
                    label = "pulseScale",
            )
    val finalScale = if (on) pulse else 1f
    Box(modifier = Modifier.graphicsLayer { scaleX = finalScale; scaleY = finalScale }) {
        content()
    }
}

/**
 * IEXA 开关行：图标（可脉动）+ 标题 + 状态文字 + 开关 + 下方提示行，整行可点。
 *
 * @param icon 行图标（记忆=大脑，思考=灯泡）
 * @param hint 提示行文字，null 表示不显示
 */
@Composable
fun IexaToggleRow(
        icon: ImageVector,
        title: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        statusOn: String,
        statusOff: String,
        hint: String? = null,
        modifier: Modifier = Modifier,
) {
    val p = LocalIexaPalette.current
    val iconColor by
            animateColorAsState(
                    targetValue = if (checked) p.accent else p.mutedText,
                    animationSpec = tween(300),
                    label = "rowIconColor",
            )
    val status = if (checked) statusOn else statusOff

    Column(modifier = modifier) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onCheckedChange(!checked) }
                                .padding(vertical = 13.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
        ) {
            PulseIcon(on = checked) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                    text = title,
                    color = p.primaryText,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
            )
            Text(text = status, color = p.mutedText, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            IexaToggle(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (hint != null) {
            Text(
                    text = hint,
                    color = p.mutedText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

/** 「记忆」开关行（大脑图标）。 */
@Composable
fun IexaMemoryToggleRow(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        title: String,
        statusOn: String,
        statusOff: String,
        hint: String? = null,
        modifier: Modifier = Modifier,
) {
    IexaToggleRow(
            icon = Icons.Rounded.Psychology,
            title = title,
            checked = checked,
            onCheckedChange = onCheckedChange,
            statusOn = statusOn,
            statusOff = statusOff,
            hint = hint,
            modifier = modifier,
    )
}

/** 「思考」开关行（灯泡图标）。 */
@Composable
fun IexaThinkToggleRow(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        title: String,
        statusOn: String,
        statusOff: String,
        hint: String? = null,
        modifier: Modifier = Modifier,
) {
    IexaToggleRow(
            icon = Icons.Rounded.TipsAndUpdates,
            title = title,
            checked = checked,
            onCheckedChange = onCheckedChange,
            statusOn = statusOn,
            statusOff = statusOff,
            hint = hint,
            modifier = modifier,
    )
}

/**
 * IEXA 模型选择器：pill 形态，按下缩放 0.97 + 描边变 accent。
 * （原版还有 hover 上浮与高光扫过，触屏无 hover，这里只保留按下反馈。）
 */
@Composable
fun IexaModelSelector(
        label: String,
        value: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val p = LocalIexaPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by
            animateFloatAsState(
                    targetValue = if (pressed) 0.97f else 1f,
                    animationSpec = tween(180),
                    label = "modelScale",
            )
    val borderColor by
            animateColorAsState(
                    targetValue = if (pressed) p.accent else p.border,
                    animationSpec = tween(200),
                    label = "modelBorder",
            )
    val shape = RoundedCornerShape(20.dp)
    Box(
            modifier =
                    modifier.fillMaxWidth()
                            .height(40.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(shape)
                            .background(p.surface)
                            .border(1.dp, borderColor, shape)
                            .clickable(interactionSource = interaction, indication = null) {
                                onClick()
                            },
            contentAlignment = Alignment.Center,
    ) {
        Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = p.mutedText, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                    text = value,
                    color = p.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "\u2304", color = p.mutedText, fontSize = 14.sp)
        }
    }
}

/**
 * IEXA 底部弹层外壳：半透明遮罩 + 20dp 圆角顶部面板 + 标题，整体底部对齐。
 * 用于「快捷设置」面板（模型 / 记忆 / 思考）。
 */
@Composable
fun IexaBottomSheet(
        visible: Boolean,
        title: String,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalIexaPalette.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .clickable(
                                            interactionSource =
                                                    remember { MutableInteractionSource() },
                                            indication = null,
                                    ) {
                                        onDismiss()
                                    }
            )
        }
        AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                    .background(p.surface)
                                    .navigationBarsPadding()
                                    .padding(bottom = 10.dp),
            ) {
                Text(
                        text = title,
                        color = p.primaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                )
                content()
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/** 弹层内的小节标题（12sp 灰、字距 1sp）。 */
@Composable
fun IexaSheetSectionLabel(text: String) {
    val p = LocalIexaPalette.current
    Text(
            text = text,
            color = p.mutedText,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
    )
}
