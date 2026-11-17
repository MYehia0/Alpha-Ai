package com.example.alpha_ai.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.alpha_ai.core.designsystem.R

/** IBM Plex Sans Arabic: one family drawn for both Arabic and Latin letters (bundled, OFL). */
internal val PlexSansArabic = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold),
)

private fun style(size: TextUnit, lineHeight: TextUnit, weight: FontWeight) = TextStyle(
    fontFamily = PlexSansArabic,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
)

/**
 * The Material 3 type scale in `sp`, so text follows the system font size. Body and label
 * lines are about 1.4 times the size because Arabic needs more vertical room than Latin.
 */
internal val AlphaTypography = Typography(
    displayLarge = style(57.sp, 64.sp, FontWeight.Normal),
    displayMedium = style(45.sp, 52.sp, FontWeight.Normal),
    displaySmall = style(36.sp, 44.sp, FontWeight.Normal),
    headlineLarge = style(32.sp, 40.sp, FontWeight.Normal),
    headlineMedium = style(28.sp, 36.sp, FontWeight.Normal),
    headlineSmall = style(24.sp, 32.sp, FontWeight.Normal),
    titleLarge = style(22.sp, 30.sp, FontWeight.Medium),
    titleMedium = style(16.sp, 24.sp, FontWeight.Medium),
    titleSmall = style(14.sp, 20.sp, FontWeight.Medium),
    bodyLarge = style(16.sp, 24.sp, FontWeight.Normal),
    bodyMedium = style(14.sp, 20.sp, FontWeight.Normal),
    bodySmall = style(12.sp, 17.sp, FontWeight.Normal),
    labelLarge = style(14.sp, 20.sp, FontWeight.Medium),
    labelMedium = style(12.sp, 17.sp, FontWeight.Medium),
    labelSmall = style(11.sp, 16.sp, FontWeight.Medium),
)
