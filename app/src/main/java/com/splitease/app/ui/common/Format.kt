package com.splitease.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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

/** "Today", "Yesterday" or a medium date, used to group the expense list. */
fun dayLabel(timestamp: Long): String {
    val d = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when (d) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

val HapticConfirm = HapticFeedbackType.Confirm
val HapticTick = HapticFeedbackType.ContextClick
