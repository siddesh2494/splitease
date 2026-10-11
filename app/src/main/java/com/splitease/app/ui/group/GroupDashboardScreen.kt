package com.splitease.app.ui.group

import android.content.Intent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.splitease.app.data.repository.GroupState
import com.splitease.app.domain.model.*
import com.splitease.app.ui.common.formatMoney
import com.splitease.app.ui.theme.LocalDebtColors
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GroupDashboardScreen(
    onBack: () -> Unit,
    onSettle: () -> Unit,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
    viewModel: GroupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val me by viewModel.me.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val ctx = LocalContext.current

    val s = state
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    with(sharedScope) {
                        Text(
                            s?.group?.name ?: "",
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState("title-${viewModel.groupId}"), animatedScope,
                            ),
                        )
                    }
                },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (s != null) IconButton(onClick = {
                        // Zero-friction invite: share a deep link + code.
                        val link = "https://splitease.app/join/${s.group.inviteCode}"
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Join \"${s.group.name}\" on SplitEase: $link (code ${s.group.inviteCode})")
                        }
                        ctx.startActivity(Intent.createChooser(send, "Invite"))
                    }) { Icon(Icons.Default.PersonAdd, "Invite") }
                },
            )
        },
        floatingActionButton = {
            if (s != null) ExtendedFloatingActionButton(
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick); showAdd = true },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add expense") },
            )
        },
    ) { padding ->
        if (s == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(Modifier.padding(top = padding.calculateTopPadding())) {
            BalanceSummary(s, me?.id, onSettle)
            TabRow(selectedTabIndex = tab) {
                listOf("Expenses", "Balances", "Activity").forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                }
            }
            val bottom = padding.calculateBottomPadding() + 96.dp
            when (tab) {
                0 -> ExpenseList(s, bottom) { viewModel.deleteExpense(it) }
                1 -> BalanceList(s, me?.id, bottom)
                else -> ActivityList(s, bottom)
            }
        }
    }

    if (showAdd && s != null) {
        AddExpenseSheet(
            group = s.group,
            myId = me?.id,
            onDismiss = { showAdd = false },
            suggestRate = { cur -> viewModel.suggestedRate(cur, s.group.baseCurrency) },
            onSave = { title, amt, cur, rate, cat, paidBy, type, inputs, rec ->
                viewModel.addExpense(title, amt, cur, rate, cat, paidBy, type, inputs, rec, s.group.baseCurrency)
            },
        )
    }
}

@Composable
private fun BalanceSummary(s: GroupState, myId: String?, onSettle: () -> Unit) {
    val my = s.balances[myId] ?: 0L
    val colors = LocalDebtColors.current
    val cur = s.group.baseCurrency
    ElevatedCard(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    when { my > 0 -> "You are owed"; my < 0 -> "You owe"; else -> "You're all settled up" },
                    style = MaterialTheme.typography.labelLarge,
                )
                if (my != 0L) Text(
                    formatMoney(my, cur),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = if (my > 0) colors.owed else colors.owe,
                )
            }
            FilledTonalButton(onClick = onSettle, enabled = s.transfers.isNotEmpty()) { Text("Settle up") }
        }
    }
}

@Composable
private fun ExpenseList(s: GroupState, bottom: Dp, onDelete: (Expense) -> Unit) {
    val names = s.group.members.associate { it.id to it.name }
    if (s.expenses.isEmpty()) return EmptyHint("No expenses yet. Tap “Add expense”.")
    LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
        items(s.expenses, key = { it.id }) { e ->
            ListItem(
                headlineContent = { Text(e.title) },
                supportingContent = { Text("${names[e.paidBy] ?: "?"} paid · ${e.category.name.lowercase()}" + if (e.recurrence != Recurrence.NONE) " · ${e.recurrence.name.lowercase()}" else "") },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMoney(e.amount, e.currency), fontWeight = FontWeight.SemiBold)
                        IconButton({ onDelete(e) }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun BalanceList(s: GroupState, myId: String?, bottom: Dp) {
    val colors = LocalDebtColors.current
    val names = s.group.members.associate { it.id to it.name }
    LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
        items(s.group.members, key = { it.id }) { m ->
            val b = s.balances[m.id] ?: 0L
            ListItem(
                headlineContent = { Text(if (m.id == myId) "You" else m.name) },
                supportingContent = {
                    Text(when { b > 0 -> "gets back"; b < 0 -> "owes"; else -> "settled" })
                },
                trailingContent = {
                    if (b != 0L) Text(
                        formatMoney(b, s.group.baseCurrency), fontWeight = FontWeight.SemiBold,
                        color = if (b > 0) colors.owed else colors.owe,
                    )
                },
            )
        }
        item {
            if (s.transfers.isNotEmpty()) {
                Text("Simplified payments", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp))
                s.transfers.forEach { t ->
                    ListItem(
                        headlineContent = { Text("${names[t.from]} → ${names[t.to]}") },
                        trailingContent = { Text(formatMoney(t.amount, s.group.baseCurrency)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityList(s: GroupState, bottom: Dp) {
    if (s.activity.isEmpty()) return EmptyHint("No activity yet.")
    val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
        items(s.activity, key = { it.id }) { a ->
            ListItem(
                headlineContent = { Text("${a.actorName} ${a.description}") },
                supportingContent = { Text(df.format(Date(a.createdAt))) },
                leadingContent = {
                    Icon(
                        when (a.type) {
                            ActivityType.SETTLED -> Icons.Default.Paid
                            ActivityType.EXPENSE_DELETED -> Icons.Default.Delete
                            ActivityType.MEMBER_JOINED -> Icons.Default.PersonAdd
                            else -> Icons.Default.ReceiptLong
                        }, null,
                    )
                },
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center)
    }
}
