package com.ai.assistance.operit.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * IEXA 手机端设计规格（来源：手机端参考工程 app/iexa/ui/theme/IexaColors.kt、Screens.kt、
 * components/IexaIcons.kt）。
 *
 * 与 [IexaColorScheme] 的区别：那一套是 M3 ColorScheme（36 字段），这一套是 IEXA 自己的 9 个
 * 语义色 + 布局规格（8dp 卡片 / 16dp 内边距 / 大写分组小标题 / 圆头描边汉堡图标）。
 */

/** IEXA 语义色板，字段与参考工程 IexaPalette 一一对应。 */
data class IexaPalette(
        val pageBackground: Color,
        val surface: Color,
        val surfaceAlt: Color,
        val primaryText: Color,
        val mutedText: Color,
        val accent: Color,
        val onAccent: Color,
        val accentDark: Color,
        val border: Color,
        val danger: Color,
)

val IexaLightPalette =
        IexaPalette(
                pageBackground = Color(0xFFF2F2F7),
                surface = Color(0xFFFFFFFF),
                surfaceAlt = Color(0xFFF7F7FA),
                primaryText = Color(0xFF171D1C),
                mutedText = Color(0xFF3F4947),
                accent = Color(0xFF2E8B8B),
                onAccent = Color(0xFFFFFFFF),
                accentDark = Color(0xFFB2DFDB),
                border = Color(0xFFD1D1D6),
                danger = Color(0xFFB3261E),
        )

val IexaDarkPalette =
        IexaPalette(
                pageBackground = Color(0xFF000000),
                surface = Color(0xFF1C1C1E),
                surfaceAlt = Color(0xFF2C2C2E),
                primaryText = Color(0xFFDEE4E2),
                mutedText = Color(0xFFBEC9C6),
                accent = Color(0xFF4DD9D9),
                onAccent = Color(0xFF003737),
                accentDark = Color(0xFF1A6B6B),
                border = Color(0xFF38383A),
                danger = Color(0xFFF2B8B5),
        )

/**
 * 生成当前生效的语义色板。
 *
 * [exact] 为 true（用户开启「IEXA 配色」）时返回参考工程里的精确值；否则按 IEXA 注释里给出的
 * 字段映射从当前 M3 ColorScheme 推导（pageBackground<-background、surface<-surfaceContainer、
 * surfaceAlt<-surfaceContainerHigh、primaryText<-onBackground、mutedText<-onSurfaceVariant、
 * accent<-primary、accentDark<-primaryContainer、border<-outline、danger<-error），
 * 这样所有界面组件都能统一读取，不必区分主题模式。
 */
fun iexaPaletteOf(colorScheme: ColorScheme, exact: Boolean, dark: Boolean): IexaPalette {
    if (exact) return if (dark) IexaDarkPalette else IexaLightPalette
    return IexaPalette(
            pageBackground = colorScheme.background,
            surface = colorScheme.surfaceContainer,
            surfaceAlt = colorScheme.surfaceContainerHigh,
            primaryText = colorScheme.onBackground,
            mutedText = colorScheme.onSurfaceVariant,
            accent = colorScheme.primary,
            onAccent = colorScheme.onPrimary,
            accentDark = colorScheme.primaryContainer,
            border = colorScheme.outline,
            danger = colorScheme.error,
    )
}

val LocalIexaPalette = staticCompositionLocalOf { IexaLightPalette }

/** IEXA 圆角规格：卡片统一 8dp。 */
val IexaShapes: Shapes
    get() =
            Shapes(
                    extraSmall = RoundedCornerShape(4.dp),
                    small = RoundedCornerShape(8.dp),
                    medium = RoundedCornerShape(8.dp),
                    large = RoundedCornerShape(12.dp),
                    extraLarge = RoundedCornerShape(16.dp),
            )

/** IEXA 卡片：8dp 圆角 + 16dp 内边距。 */
@Composable
fun IexaCard(
        modifier: Modifier = Modifier,
        alt: Boolean = false,
        contentPadding: Dp = 16.dp,
        content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalIexaPalette.current
    Column(
            modifier =
                    modifier.fillMaxWidth()
                            .background(
                                    color = if (alt) palette.surfaceAlt else palette.surface,
                                    shape = RoundedCornerShape(8.dp),
                            )
                            .padding(contentPadding),
            content = content,
    )
}

/**
 * 分组小标题（对应参考工程 SectionLabel）：全大写、12sp、字距 1sp、mutedText，上下 12dp。
 * [trailing] 可放右侧的辅助操作。
 */
@Composable
fun IexaSectionLabel(
        text: String,
        modifier: Modifier = Modifier,
        trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val palette = LocalIexaPalette.current
    Row(
            modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
                text = text.uppercase(),
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = palette.mutedText,
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * 圆头描边汉堡图标（复刻参考工程 iexa_rounded_menu.xml：24x24 视框，上笔 M3,8→L21,8，
 * 下笔 M3,16→L15,16，3dp 圆头描边）。描边色默认跟随 primaryText。
 */
@Composable
fun IexaRoundedMenuIcon(
        modifier: Modifier = Modifier,
        tint: Color = LocalIexaPalette.current.primaryText,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 3.dp.toPx().coerceAtMost(minOf(w, h) * 0.16f)
        drawLine(
                color = tint,
                start = Offset(w * 3f / 24f, h * 8f / 24f),
                end = Offset(w * 21f / 24f, h * 8f / 24f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
        )
        drawLine(
                color = tint,
                start = Offset(w * 3f / 24f, h * 16f / 24f),
                end = Offset(w * 15f / 24f, h * 16f / 24f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
        )
    }
}

/**
 * 顶栏模型选择器 pill（复刻 ui-kit preview.html 的 .model-selector）：
 * 高 40dp、全圆角、surface 底 + border 描边、居中、12sp 灰标签 + 14sp 模型名 + 下拉箭头。
 */
@Composable
fun IexaModelSelectorPill(
        label: String,
        modelName: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val palette = LocalIexaPalette.current
    val shape = RoundedCornerShape(20.dp)
    Row(
            modifier =
                    modifier.fillMaxWidth()
                            .height(40.dp)
                            .clip(shape)
                            .background(palette.surface)
                            .border(1.dp, palette.border, shape)
                            .clickable(onClick = onClick)
                            .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
    ) {
        Text(
                text = label,
                fontSize = 12.sp,
                color = palette.mutedText,
                maxLines = 1,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
                text = modelName,
                fontSize = 14.sp,
                color = palette.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = palette.mutedText,
                modifier = Modifier.size(14.dp),
        )
    }
}

/**
 * 空会话欢迎态（复刻 preview.html 的 .empty）：主色圆角 logo 块 + 主文案 + 副文案。
 */
@Composable
fun IexaEmptyChatState(
        title: String,
        subtitle: String,
        modifier: Modifier = Modifier,
        logoText: String = "AI",
        titleFontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
) {
    val palette = LocalIexaPalette.current
    Column(
            modifier = modifier.padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
                modifier =
                        Modifier.size(72.dp)
                                .background(color = palette.accent, shape = RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
        ) {
            Text(
                    text = logoText,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
                text = title,
                color = palette.primaryText,
                fontSize = titleFontSize,
                fontWeight = FontWeight.SemiBold,
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    text = subtitle,
                    color = palette.mutedText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
