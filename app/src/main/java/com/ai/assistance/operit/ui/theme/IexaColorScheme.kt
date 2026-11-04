package com.ai.assistance.operit.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * IEXA 配色方案（原样复刻）。
 *
 * 数值来自 IEXA 手机端的 M3 `ColorScheme` 常量，经 smali `<clinit>` 逐寄存器回放还原：
 * 浅色 primary = #2E8B8B（青绿），底 = #F2F2F7，卡片 = #FFFFFF；
 * 深色 primary = #4DD9D9，底 = #000000（纯黑），容器 = #1C1C1E，高容器 = #2C2C2E。
 *
 * 用途：设置里打开「IEXA 配色」即可整套套用，不走系统动态取色。
 */
private fun iexaLight(): ColorScheme =
    lightColorScheme(
        primary = Color(0xFF2E8B8B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB2DFDB),
        onPrimaryContainer = Color(0xFF00332F),
        inversePrimary = Color(0xFFD0BCFF),
        secondary = Color(0xFF4A6360),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCE8E4),
        onSecondaryContainer = Color(0xFF05201D),
        tertiary = Color(0xFF46617A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFCDE5FF),
        onTertiaryContainer = Color(0xFF001D32),
        background = Color(0xFFF2F2F7),
        onBackground = Color(0xFF171D1C),
        surface = Color(0xFFF2F2F7),
        onSurface = Color(0xFF171D1C),
        surfaceVariant = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFF3F4947),
        surfaceTint = Color(0xFF2E8B8B),
        inverseSurface = Color(0xFF322F35),
        inverseOnSurface = Color(0xFFF5EFF7),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        outline = Color(0xFFD1D1D6),
        outlineVariant = Color(0xFFD1D1D6),
        scrim = Color(0xFF000000),
    )

private fun iexaDark(): ColorScheme =
    darkColorScheme(
        primary = Color(0xFF4DD9D9),
        onPrimary = Color(0xFF003737),
        primaryContainer = Color(0xFF1A6B6B),
        onPrimaryContainer = Color(0xFFB2DFDB),
        inversePrimary = Color(0xFF6750A4),
        secondary = Color(0xFFB1CCC8),
        onSecondary = Color(0xFF1C3532),
        secondaryContainer = Color(0xFF334B48),
        onSecondaryContainer = Color(0xFFCCE8E4),
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
        background = Color(0xFF000000),
        onBackground = Color(0xFFDEE4E2),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFDEE4E2),
        surfaceVariant = Color(0xFF1C1C1E),
        onSurfaceVariant = Color(0xFFBEC9C6),
        surfaceTint = Color(0xFF4DD9D9),
        inverseSurface = Color(0xFFE6E0E9),
        inverseOnSurface = Color(0xFF322F35),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
        outline = Color(0xFF38383A),
        outlineVariant = Color(0xFF38383A),
        scrim = Color(0xFF000000),
    )

/** 复用同一实例，避免每次重组都新建配色对象。 */
internal val IexaLightColorScheme: ColorScheme = iexaLight()
internal val IexaDarkColorScheme: ColorScheme = iexaDark()
