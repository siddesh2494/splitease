package com.splitease.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout tokens. These map 1:1 to Figma auto-layout values:
 *   Spacing.*  -> "gap between items" and "padding"
 *   Radius.*   -> "corner radius"
 * Screens never use raw dp numbers for gaps or card corners; they pick a token.
 */
object Spacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    /** Horizontal screen gutter. */
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    /** Space reserved under lists so the floating action button never covers content. */
    val fabClearance: Dp = 110.dp
}

object Radius {
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    /** Selectable tiles, stat pills. */
    val tile: Dp = 18.dp
    /** List rows (expense, member, activity). */
    val row: Dp = 20.dp
    /** Cards (group card, settle card). */
    val card: Dp = 24.dp
    /** Hero gradient cards. */
    val hero: Dp = 28.dp
}
