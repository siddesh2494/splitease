package com.splitease.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E), onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1), onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF4F46E5), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF), onSecondaryContainer = Color(0xFF1E1B4B),
    tertiary = Color(0xFFF59E0B), onTertiary = Color.White,
    background = Color(0xFFF3F6F5), onBackground = Color(0xFF0F1A19),
    surface = Color(0xFFF3F6F5), onSurface = Color(0xFF0F1A19),
    surfaceVariant = Color(0xFFE3EAE8), onSurfaceVariant = Color(0xFF4A5856),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEDF2F1), surfaceContainerHigh = Color(0xFFE6ECEA),
    surfaceContainerHighest = Color(0xFFDFE6E4),
    outline = Color(0xFF8A9895), outlineVariant = Color(0xFFD3DCDA),
    error = Color(0xFFDC2626), errorContainer = Color(0xFFFEE2E2), onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4), onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF134E4A), onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFA5B4FC), onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF312E81), onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFFFBBF24), onTertiary = Color(0xFF1C1300),
    background = Color(0xFF0B1211), onBackground = Color(0xFFE3ECEA),
    surface = Color(0xFF0B1211), onSurface = Color(0xFFE3ECEA),
    surfaceVariant = Color(0xFF26302E), onSurfaceVariant = Color(0xFFA9B8B5),
    surfaceContainerLowest = Color(0xFF070D0C), surfaceContainerLow = Color(0xFF131C1B),
    surfaceContainer = Color(0xFF182120), surfaceContainerHigh = Color(0xFF202A29),
    surfaceContainerHighest = Color(0xFF2A3534),
    outline = Color(0xFF6B7A77), outlineVariant = Color(0xFF2F3B39),
    error = Color(0xFFF87171), errorContainer = Color(0xFF5B1A1A), onErrorContainer = Color(0xFFFECACA),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Semantic debt colors: green = you are owed, red = you owe. Kept outside the base palette on purpose. */
@Immutable
data class DebtColors(val owed: Color, val owe: Color)

val LocalDebtColors = staticCompositionLocalOf { DebtColors(Color(0xFF16A34A), Color(0xFFDC2626)) }

/** Signature teal → indigo gradient used by hero cards and avatars of the app. */
val LocalBrandBrush = staticCompositionLocalOf {
    Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF4F46E5)))
}

@Composable
fun SplitEaseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the app keeps its own look; flip to true to follow the wallpaper colors.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val debt = if (darkTheme) DebtColors(Color(0xFF4ADE80), Color(0xFFF87171))
    else DebtColors(Color(0xFF16A34A), Color(0xFFDC2626))
    val brand = if (darkTheme) Brush.linearGradient(listOf(Color(0xFF115E59), Color(0xFF3730A3)))
    else Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF4F46E5)))

    CompositionLocalProvider(LocalDebtColors provides debt, LocalBrandBrush provides brand) {
        MaterialTheme(colorScheme = scheme, shapes = AppShapes, typography = AppTypography, content = content)
    }
}
