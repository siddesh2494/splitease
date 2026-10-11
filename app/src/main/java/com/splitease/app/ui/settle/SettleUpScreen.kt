package com.splitease.app.ui.settle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.splitease.app.domain.model.Transfer
import com.splitease.app.ui.common.formatMoney
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
        topBar = {
            TopAppBar(
                title = { Text("Settle up") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        val s = state ?: return@Scaffold
        val names = s.group.members.associate { it.id to it.name }
        val cur = s.group.baseCurrency
        if (s.transfers.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding), Arrangement.Center, Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = LocalDebtColors.current.owed, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Everyone is settled up", style = MaterialTheme.typography.titleMedium)
            }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(
                16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "${s.transfers.size} payment${if (s.transfers.size == 1) "" else "s"} settle everything",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(s.transfers, key = { it.from + it.to }) { t ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (t.from == me?.id) "You" else names[t.from] ?: "?", fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ArrowForward, null, Modifier.padding(horizontal = 8.dp).size(16.dp))
                                Text(if (t.to == me?.id) "You" else names[t.to] ?: "?", fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                formatMoney(t.amount, cur), style = MaterialTheme.typography.titleLarge,
                                color = if (t.from == me?.id) LocalDebtColors.current.owe else LocalDebtColors.current.owed,
                            )
                        }
                        FilledTonalButton(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick); pending = t
                        }) { Text("Record") }
                    }
                }
            }
        }

        pending?.let { t ->
            AlertDialog(
                onDismissRequest = { pending = null },
                title = { Text("Record payment") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${names[t.from]} paid ${names[t.to]} ${formatMoney(t.amount, cur)}")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
