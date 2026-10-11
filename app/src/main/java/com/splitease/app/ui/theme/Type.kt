package com.splitease.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.splitease.app.R

/**
 * Plus Jakarta Sans (SIL Open Font License, free for commercial use) bundled as one variable font.
 * Each weight below picks a point on the weight axis, so a single 170 KB file gives 5 weights.
 */
@OptIn(ExperimentalTextApi::class)
val AppFontFamily = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(
            resId = R.font.plusjakartasans,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    }
)

private fun TextStyle.app(weight: FontWeight? = null, spacing: Float? = null): TextStyle = copy(
    fontFamily = AppFontFamily,
    fontWeight = weight ?: fontWeight,
    letterSpacing = spacing?.sp ?: letterSpacing,
)

/** Material 3 type scale re-skinned with the app font. Hierarchy: display/headline = bold, title = semibold. */
val AppTypography: Typography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.app(FontWeight.ExtraBold, -1f),
        displayMedium = t.displayMedium.app(FontWeight.ExtraBold, -0.75f),
        displaySmall = t.displaySmall.app(FontWeight.ExtraBold, -0.5f),
        headlineLarge = t.headlineLarge.app(FontWeight.ExtraBold, -0.5f),
        headlineMedium = t.headlineMedium.app(FontWeight.Bold, -0.3f),
        headlineSmall = t.headlineSmall.app(FontWeight.Bold),
        titleLarge = t.titleLarge.app(FontWeight.Bold),
        titleMedium = t.titleMedium.app(FontWeight.SemiBold),
        titleSmall = t.titleSmall.app(FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.app(FontWeight.Normal),
        bodyMedium = t.bodyMedium.app(FontWeight.Normal),
        bodySmall = t.bodySmall.app(FontWeight.Normal),
        labelLarge = t.labelLarge.app(FontWeight.SemiBold),
        labelMedium = t.labelMedium.app(FontWeight.SemiBold),
        labelSmall = t.labelSmall.app(FontWeight.Medium),
    )
}
