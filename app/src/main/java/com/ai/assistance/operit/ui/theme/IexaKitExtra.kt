package com.ai.assistance.operit.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * IEXA UI 补充组件（对应 iexa-fusion-preview.html 里已定稿的几处交互）：
 *
 * - [IexaIconBadge]：列表图标改成独立渐变圆角块 + 白色字形，每项一个配色（按标题哈希稳定分配）
 * - [IexaSegmentedControl]：三态/分段切换的滑块弹性位移
 * - [IexaColorSwatches]：色块选择的选中框滑动
 * - [IexaAttachmentStrip]：输入栏上方的附件条，可逐条移除
 */

/** 12 套渐变，顺序与 preview.html 的 IB 表一致。 */
private val IexaBadgeGradients: List<Pair<Color, Color>> =
        listOf(
                Color(0xFF3B82F6) to Color(0xFF6366F1),
                Color(0xFF6366F1) to Color(0xFF8B5CF6),
                Color(0xFF0EA5E9) to Color(0xFF22D3EE),
                Color(0xFF8B5CF6) to Color(0xFFEC4899),
                Color(0xFF06B6D4) to Color(0xFF3B82F6),
                Color(0xFFF59E0B) to Color(0xFFEF4444),
                Color(0xFF10B981) to Color(0xFF0D9488),
                Color(0xFFF97316) to Color(0xFFFBBF24),
                Color(0xFFEC4899) to Color(0xFFF43F5E),
                Color(0xFF14B8A6) to Color(0xFF0EA5E9),
                Color(0xFF7C3AED) to Color(0xFF4F46E5),
                Color(0xFF22C55E) to Color(0xFF84CC16),
        )

/** 按名称哈希取一组渐变色：同一个功能每次打开颜色都一致，不会乱跳。 */
fun iexaBadgeColors(seed: String): Pair<Color, Color> {
    var h = 0
    val s = seed.ifBlank { "iexa" }
    for (c in s) {
        h = (h * 31 + c.code) % 99991
    }
    if (h < 0) h += 99991
    return IexaBadgeGradients[h % IexaBadgeGradients.size]
}

/** 渐变图标块：30dp 圆角方块 + 白字图标（对应 preview.html 的 .ib）。 */
@Composable
fun IexaIconBadge(
        icon: ImageVector,
        seed: String,
        modifier: Modifier = Modifier,
        size: Dp = 30.dp,
        iconSize: Dp = 16.dp,
) {
    val (c1, c2) = remember(seed) { iexaBadgeColors(seed) }
    Box(
            modifier =
                    modifier.size(size)
                            .clip(RoundedCornerShape(size * 0.3f))
                            .background(Brush.linearGradient(listOf(c1, c2))),
            contentAlignment = Alignment.Center,
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.96f),
                modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * 分段切换（允许 / 询问 / 禁止、深色 / 浅色 / 跟随系统 这类）：
 * 高亮滑块用 spring 在两档之间滑过去，点击有按压缩放。
 */
@Composable
fun IexaSegmentedControl(
        options: List<String>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    val p = LocalIexaPalette.current
    val anim by
            animateFloatAsState(
                    targetValue = selectedIndex.toFloat(),
                    animationSpec =
                            spring(
                                    dampingRatio = 0.72f,
                                    stiffness = Spring.StiffnessMediumLow,
                            ),
                    label = "iexaSegThumb",
            )
    BoxWithConstraints(
            modifier =
                    modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.surface)
                            .padding(4.dp)
    ) {
        val itemWidth = maxWidth / options.size
        Box(
                modifier =
                        Modifier.offset(x = itemWidth * anim)
                                .width(itemWidth)
                                .height(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(p.surfaceAlt)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                Box(
                        modifier =
                                Modifier.weight(1f).height(32.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .clickable { onSelect(i) },
                        contentAlignment = Alignment.Center,
                ) {
                    Text(
                            text = label,
                            fontSize = 13.sp,
                            color = if (i == selectedIndex) p.primaryText else p.mutedText,
                            fontWeight =
                                    if (i == selectedIndex) FontWeight.SemiBold
                                    else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/** 主色色块选择：选中框滑到被选色块上，✓ 缩放淡入。 */
@Composable
fun IexaColorSwatches(
        colors: List<Color>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier = Modifier,
) {
    if (colors.isEmpty()) return
    val p = LocalIexaPalette.current
    val anim by
            animateFloatAsState(
                    targetValue = selectedIndex.toFloat(),
                    animationSpec =
                            spring(
                                    dampingRatio = 0.72f,
                                    stiffness = Spring.StiffnessMediumLow,
                            ),
                    label = "iexaSwatchRing",
            )
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = 12.dp
        val itemWidth = (maxWidth - gap * (colors.size - 1)) / colors.size
        Box(
                modifier =
                        Modifier.offset(x = (itemWidth + gap) * anim)
                                .width(itemWidth)
                                .height(44.dp)
                                .border(2.dp, p.primaryText, RoundedCornerShape(12.dp))
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            colors.forEachIndexed { i, color ->
                Box(
                        modifier =
                                Modifier.width(itemWidth)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color)
                                        .clickable { onSelect(i) },
                        contentAlignment = Alignment.Center,
                ) {
                    if (i == selectedIndex) {
                        Text(
                                text = "✓",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }
    }
}

/** 附件条里的一项（名称 + 类型图标）。 */
data class IexaAttachmentChip(val name: String, val icon: ImageVector)

/**
 * 输入栏上方的附件条：可横向滚动，每条可点 ✕ 移除。
 * 对应 preview.html 的 .attstrip（列表为空时不占高度）。
 */
@Composable
fun IexaAttachmentStrip(
        items: List<IexaAttachmentChip>,
        onRemove: (Int) -> Unit,
        modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val p = LocalIexaPalette.current
    LazyRow(
            modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(items) { index, item ->
            Row(
                    modifier =
                            Modifier.clip(RoundedCornerShape(12.dp))
                                    .background(p.surfaceAlt)
                                    .border(1.dp, p.border, RoundedCornerShape(12.dp))
                                    .padding(start = 7.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                IexaIconBadge(
                        icon = item.icon,
                        seed = item.name,
                        size = 18.dp,
                        iconSize = 11.dp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                        text = item.name,
                        color = p.primaryText,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                        modifier =
                                Modifier.size(18.dp)
                                        .clip(CircleShape)
                                        .background(p.border)
                                        .clickable { onRemove(index) },
                        contentAlignment = Alignment.Center,
                ) {
                    Text(text = "✕", color = p.mutedText, fontSize = 10.sp)
                }
            }
        }
    }
}
