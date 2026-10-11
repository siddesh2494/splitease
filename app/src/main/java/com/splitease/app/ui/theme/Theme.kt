package com.splitease.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6B5A), onPrimary = Color.White,
    primaryContainer = Color(0xFFA6F2DC), secondary = Color(0xFF4A635B),
    background = Color(0xFFF6FAF8), surface = Color(0xFFF6FAF8),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD5C0), onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF00513F), secondary = Color(0xFFB1CCC1),
    background = Color(0xFF0E1512), surface = Color(0xFF0E1512),
)

/** Semantic debt colors: green = you are owed, red = you owe. Kept outside dynamic color on purpose. */
@Immutable
data class DebtColors(val owed: Color, val owe: Color)

val LocalDebtColors = staticCompositionLocalOf { DebtColors(Color(0xFF1E8E3E), Color(0xFFC62828)) }

@Composable
fun SplitEaseTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val debt = if (darkTheme) DebtColors(Color(0xFF81C995), Color(0xFFF28B82))
    else DebtColors(Color(0xFF1E8E3E), Color(0xFFC62828))
    CompositionLocalProvider(LocalDebtColors provides debt) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
