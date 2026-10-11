package com.splitease.app.ui.group

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.splitease.app.domain.model.*
import com.splitease.app.ui.common.*
import com.splitease.app.ui.theme.Radius
import com.splitease.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.math.BigDecimal

private val CURRENCIES = listOf("INR", "USD", "EUR", "GBP", "AED", "THB", "JPY", "SGD", "AUD", "CAD")

private fun minorToText(minor: Long): String = BigDecimal.valueOf(minor, 2).toPlainString()

/**
 * Add / edit expense bottom sheet.
 *
 * [initial] != null puts the sheet in edit mode with every field prefilled. Stored percent/shares
 * splits are shown as exact amounts when editing (the originals aren't kept, only the result).
 * [onSave] returns an error message or null on success.
 * [onAddMember] adds a new person to the group and returns their id (null if the name was blank).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseSheet(
    group: Group,
    myId: String?,
    initial: Expense?,
    onDismiss: () -> Unit,
    suggestRate: suspend (String) -> Double,
    onAddMember: suspend (String) -> String?,
    onSave: suspend (String, Long, String, Double, ExpenseCategory, String, SplitType, Map<String, Long>, Recurrence) -> String?,
) {
    val isEdit = initial != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amountText by remember { mutableStateOf(initial?.let { minorToText(it.amount) } ?: "") }
    var currency by remember { mutableStateOf(initial?.currency ?: group.baseCurrency) }
    var rateText by remember { mutableStateOf(initial?.rateToBase?.toString() ?: "1") }
    var category by remember { mutableStateOf(initial?.category ?: ExpenseCategory.FOOD) }
    var paidBy by remember { mutableStateOf(initial?.paidBy ?: myId ?: group.members.first().id) }
    var recurrence by remember { mutableStateOf(initial?.recurrence ?: Recurrence.NONE) }
    var splitType by remember {
        mutableStateOf(if (initial == null || initial.splitType == SplitType.EQUAL) SplitType.EQUAL else SplitType.EXACT)
    }
    var selected by remember {
        mutableStateOf<Set<String>>(
            initial?.splits?.map { it.memberId }?.toSet() ?: group.members.map { it.id }.toSet()
        )
    }
    // Explicit type: with only `mapOf()` in the else-branch Kotlin can't infer the map's types.
    var raw by remember {
        mutableStateOf<Map<String, String>>(
            if (initial != null && initial.splitType != SplitType.EQUAL)
                initial.splits.associate { it.memberId to minorToText(it.amount) }
            else emptyMap()
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    var newPerson by remember { mutableStateOf("") }

    LaunchedEffect(currency) {
        rateText = when {
            currency == group.baseCurrency -> "1"
            initial != null && currency == initial.currency -> initial.rateToBase.toString()
            else -> suggestRate(currency).toString()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg)
                .navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(if (isEdit) "Edit expense" else "Add expense", style = MaterialTheme.typography.headlineSmall)

            // Big amount entry
            Surface(shape = RoundedCornerShape(Radius.card), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
                    var open by remember { mutableStateOf(false) }
                    Box {
                        AssistChip(onClick = { open = true }, label = { Text(currency) })
                        DropdownMenu(open, { open = false }) {
                            (listOf(group.baseCurrency) + CURRENCIES).distinct().forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { currency = it; open = false })
                            }
                        }
                    }
                    TextField(
                        value = amountText, onValueChange = { amountText = it },
                        placeholder = {
                            Text("0.00", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.displaySmall)
                        },
                        textStyle = MaterialTheme.typography.displaySmall.copy(textAlign = TextAlign.Center),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            OutlinedTextField(
                title, { title = it }, label = { Text("What was it for?") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
            )

            if (currency != group.baseCurrency) {
                OutlinedTextField(
                    rateText, { rateText = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    label = { Text("1 $currency = ? ${group.baseCurrency} (editable, saved offline)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            SectionLabel("Category")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                ExpenseCategory.entries.forEach {
                    FilterChip(
                        category == it, { category = it }, label = { Text(it.label()) },
                        leadingIcon = { Icon(it.icon(), null, Modifier.size(18.dp)) },
                    )
                }
            }

            SectionLabel("Paid by")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                group.members.forEach {
                    FilterChip(
                        paidBy == it.id, { paidBy = it.id },
                        label = { Text(if (it.id == myId) "You" else it.name) },
                        leadingIcon = { Avatar(it.name, size = 22.dp) },
                    )
                }
            }

            SectionLabel("Split between")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val labels = listOf("Equal", "Exact", "%", "Shares")
                SplitType.entries.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = splitType == t, onClick = { splitType = t; raw = emptyMap() },
                        shape = SegmentedButtonDefaults.itemShape(i, SplitType.entries.size),
                        icon = {},
                    ) { Text(labels[i]) }
                }
            }
            Surface(shape = RoundedCornerShape(Radius.row), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs)) {
                    group.members.forEach { m ->
                        Row(Modifier.padding(vertical = Spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(m.name, size = 32.dp)
                            Spacer(Modifier.width(Spacing.sm))
                            Text(if (m.id == myId) "You" else m.name, Modifier.weight(1f))
                            if (splitType == SplitType.EQUAL) {
                                Checkbox(m.id in selected, { c -> selected = if (c) selected + m.id else selected - m.id })
                            } else {
                                OutlinedTextField(
                                    raw[m.id] ?: "", { raw = raw + (m.id to it) }, singleLine = true,
                                    modifier = Modifier.width(120.dp), shape = MaterialTheme.shapes.small,
                                    suffix = { Text(when (splitType) { SplitType.PERCENT -> "%"; SplitType.SHARES -> "sh"; else -> currency }) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                )
                            }
                        }
                    }
                    // Add someone new without leaving the sheet; they're included in the split right away.
                    Row(Modifier.padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            newPerson, { newPerson = it }, singleLine = true, modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small, placeholder = { Text("Add a new person") },
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        FilledTonalIconButton(
                            enabled = newPerson.isNotBlank(),
                            onClick = {
                                val name = newPerson
                                scope.launch {
                                    val id = onAddMember(name)
                                    if (id != null) { selected = selected + id; newPerson = "" }
                                }
                            },
                        ) { Icon(Icons.Default.PersonAdd, "Add person") }
                    }
                    if (splitType == SplitType.EQUAL) Text(
                        "Untick someone to remove them from this expense.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                }
            }

            SectionLabel("Repeat")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Recurrence.entries.forEach {
                    FilterChip(
                        recurrence == it, { recurrence = it },
                        label = { Text(if (it == Recurrence.NONE) "Never" else it.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    )
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
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
            ) { Text(if (isEdit) "Save changes" else "Save expense", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(Spacing.md))
        }
    }
}
