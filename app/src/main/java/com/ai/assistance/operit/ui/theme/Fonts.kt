package com.ai.assistance.operit.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ai.assistance.operit.R

/**
 * Operit monospace family (JetBrains Mono).
 *
 * Used for code blocks, terminal output and tool payload previews so the text
 * keeps a consistent, modern look instead of the platform default monospace.
 */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)