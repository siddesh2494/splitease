package com.splitease.app.ui.common

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

fun formatMoney(minor: Long, currency: String): String = runCatching {
    val c = Currency.getInstance(currency)
    val f = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply { this.currency = c }
    f.format(abs(minor) / 100.0)
}.getOrElse { "$currency ${"%.2f".format(abs(minor) / 100.0)}" }

/** Parses user text like "12.5" into minor units, or null if invalid. */
fun parseMinor(text: String): Long? =
    text.trim().replace(",", "").toBigDecimalOrNull()
        ?.movePointRight(2)?.setScale(0, java.math.RoundingMode.HALF_UP)?.toLong()

val HapticConfirm = HapticFeedbackType.Confirm
val HapticTick = HapticFeedbackType.ContextClick
