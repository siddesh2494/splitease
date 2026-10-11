package com.splitease.app.ui.settle

import com.splitease.app.ui.theme.Radius
import com.splitease.app.ui.theme.Spacing
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.splitease.app.domain.model.Transfer
import com.splitease.app.ui.common.*
import com.splitease.app.ui.group.GroupViewModel
import com.splitease.app.ui.theme.LocalDebtColors

private val METHODS = listOf("CASH", "UPI", "BANK", "OTHER")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettleUpScreen(onBack: () -> Unit, viewModel: GroupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val me by viewModel.me.collectAsState()
    val haptics = LocalHapticFeedback.current
    var pending by remember { mutableStateOf<Transfer?>(null) }
    var method by remember { mutableStateOf("UPI") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settle up") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        val s = state ?: return@Scaffold
        val names = s.group.members.associate { it.id to it.name }
        val cur = s.group.baseCurrency
        val debt = LocalDebtColors.current

        if (s.transfers.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding), Arrangement.Center, Alignment.CenterHorizontally,
            ) {
                EmptyState(
                    Icons.Default.CheckCircle, "Everyone is settled up",
                    "No payments are pending in this group. Nice and tidy!",
                )
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                Spacing.lg, padding.calculateTopPadding() + 4.dp, Spacing.lg, padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "hero") {
                BrandCard {
                    Text("To clear every balance", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        "${s.transfers.size} payment${if (s.transfers.size == 1) "" else "s"}",
                        color = Color.White, style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        "Simplified from ${s.expenses.size} expense${if (s.expenses.size == 1) "" else "s"}",
                        color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            items(s.transfers, key = { it.from + it.to }) { t ->
                val iPay = t.from == me?.id
                Surface(
                    shape = RoundedCornerShape(Radius.card),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().animateItem(),
                ) {
                    Column(Modifier.padding(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(names[t.from] ?: "?", size = 44.dp)
                            Icon(Icons.Default.ArrowForward, null, Modifier.padding(horizontal = Spacing.sm).size(20.dp))
                            Avatar(names[t.to] ?: "?", size = 44.dp)
                            Spacer(Modifier.weight(1f))
                            Text(
                                formatMoney(t.amount, cur), style = MaterialTheme.typography.titleLarge,
                                color = if (iPay) debt.owe else debt.owed,
                            )
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            "${if (iPay) "You" else names[t.from] ?: "?"} pay${if (iPay) "" else "s"} " +
                                (if (t.to == me?.id) "you" else names[t.to] ?: "?"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        FilledTonalButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick); pending = t
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Paid, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(Spacing.xs))
                            Text("Record payment")
                        }
                    }
                }
            }
        }

        pending?.let { t ->
            AlertDialog(
                onDismissRequest = { pending = null },
                title = { Text("Record payment") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            "${names[t.from]} paid ${names[t.to]} ${formatMoney(t.amount, cur)}",
                            fontWeight = FontWeight.SemiBold,
                        )
                        SectionLabel("How was it paid?")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            METHODS.forEach { m ->
                                FilterChip(method == m, { method = m }, label = { Text(m.lowercase().replaceFirstChar(Char::uppercase)) })
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.settle(t.from, t.to, t.amount, method)
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        pending = null
                    }) { Text("Confirm") }
                },
                dismissButton = { TextButton({ pending = null }) { Text("Cancel") } },
            )
        }
    }
}
