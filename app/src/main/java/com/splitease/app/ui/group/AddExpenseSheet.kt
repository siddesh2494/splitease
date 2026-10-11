package com.splitease.app.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.splitease.app.domain.model.*
import com.splitease.app.ui.common.parseMinor
import kotlinx.coroutines.launch

private val CURRENCIES = listOf("INR", "USD", "EUR", "GBP", "AED", "THB", "JPY", "SGD", "AUD", "CAD")

/**
 * Add-expense bottom sheet. [onSave] returns an error message or null on success.
 * Parameters: title, amountMinor, currency, rateToBase, category, paidBy, splitType, rawInputs, recurrence.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseSheet(
    group: Group,
    myId: String?,
    onDismiss: () -> Unit,
    suggestRate: suspend (String) -> Double,
    onSave: suspend (String, Long, String, Double, ExpenseCategory, String, SplitType, Map<String, Long>, Recurrence) -> String?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(group.baseCurrency) }
    var rateText by remember { mutableStateOf("1") }
    var category by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var paidBy by remember { mutableStateOf(myId ?: group.members.first().id) }
    var splitType by remember { mutableStateOf(SplitType.EQUAL) }
    var recurrence by remember { mutableStateOf(Recurrence.NONE) }
    var selected by remember { mutableStateOf(group.members.map { it.id }.toSet()) }
    var raw by remember { mutableStateOf(mapOf<String, String>()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currency) {
        rateText = if (currency == group.baseCurrency) "1" else suggestRate(currency).toString()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                .navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add expense", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(title, { title = it }, label = { Text("What was it for?") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    amountText, { amountText = it }, label = { Text("Amount") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                )
                var open by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { open = true }, modifier = Modifier.padding(top = 8.dp)) { Text(currency) }
                    DropdownMenu(open, { open = false }) {
                        (listOf(group.baseCurrency) + CURRENCIES).distinct().forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { currency = it; open = false })
                        }
                    }
                }
            }
            if (currency != group.baseCurrency) {
                OutlinedTextField(
                    rateText, { rateText = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text("1 $currency = ? ${group.baseCurrency} (editable, saved offline)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ExpenseCategory.entries.forEach {
                    FilterChip(category == it, { category = it }, label = { Text(it.name.lowercase().replaceFirstChar(Char::uppercase)) })
                }
            }

            Text("Paid by", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                group.members.forEach {
                    FilterChip(paidBy == it.id, { paidBy = it.id }, label = { Text(if (it.id == myId) "You" else it.name) })
                }
            }

            Text("Split", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val labels = listOf("Equal", "Exact", "%", "Shares")
                SplitType.entries.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = splitType == t, onClick = { splitType = t; raw = emptyMap() },
                        shape = SegmentedButtonDefaults.itemShape(i, SplitType.entries.size),
                    ) { Text(labels[i]) }
                }
            }
            group.members.forEach { m ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    if (splitType == SplitType.EQUAL) {
                        Checkbox(m.id in selected, { c -> selected = if (c) selected + m.id else selected - m.id })
                        Text(if (m.id == myId) "You" else m.name)
                    } else {
                        Text(if (m.id == myId) "You" else m.name, Modifier.weight(1f))
                        OutlinedTextField(
                            raw[m.id] ?: "", { raw = raw + (m.id to it) }, singleLine = true, modifier = Modifier.width(120.dp),
                            suffix = { Text(when (splitType) { SplitType.PERCENT -> "%"; SplitType.SHARES -> "sh"; else -> currency }) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                }
            }

            Text("Repeat", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Recurrence.entries.forEach {
                    FilterChip(recurrence == it, { recurrence = it }, label = { Text(it.name.lowercase().replaceFirstChar(Char::uppercase)) })
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val amount = parseMinor(amountText)
                    val rate = rateText.toDoubleOrNull()
                    if (amount == null || amount <= 0) { error = "Enter a valid amount"; return@Button }
                    if (rate == null) { error = "Enter a valid exchange rate"; return@Button }
                    // Translate UI text into raw inputs for the split engine.
                    val inputs: Map<String, Long> = when (splitType) {
                        SplitType.EQUAL -> selected.associateWith { 1L }
                        SplitType.EXACT -> raw.mapNotNull { (k, v) -> parseMinor(v)?.takeIf { it > 0 }?.let { k to it } }.toMap()
                        SplitType.PERCENT -> raw.mapNotNull { (k, v) -> parseMinor(v)?.takeIf { it > 0 }?.let { k to it } }.toMap() // "50.25" -> 5025
                        SplitType.SHARES -> raw.mapNotNull { (k, v) -> v.toLongOrNull()?.takeIf { it > 0 }?.let { k to it } }.toMap()
                    }
                    scope.launch {
                        val err = onSave(title, amount, currency, rate, category, paidBy, splitType, inputs, recurrence)
                        if (err == null) {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            sheetState.hide(); onDismiss()
                        } else error = err
                    }
                },
            ) { Text("Save expense") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
