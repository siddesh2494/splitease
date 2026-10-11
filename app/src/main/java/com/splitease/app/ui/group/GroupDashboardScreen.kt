package com.splitease.app.ui.group

import com.splitease.app.ui.theme.Radius
import com.splitease.app.ui.theme.Spacing
import android.content.Intent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.splitease.app.data.repository.GroupState
import com.splitease.app.domain.model.*
import com.splitease.app.ui.common.*
import com.splitease.app.ui.theme.LocalDebtColors
import kotlinx.coroutines.launch
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
    var showDeleteGroup by remember { mutableStateOf(false) }
    var showManage by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Expense?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Keep showing the last known state while the group is being deleted (avoids a loader flash).
    val lastKnown = remember { arrayOfNulls<GroupState>(1) }
    state?.let { lastKnown[0] = it }
    val s = state ?: lastKnown[0]

    fun invite(g: GroupState) {
        val link = "https://splitease.app/join/${g.group.inviteCode}"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Join \"${g.group.name}\" on SplitEase: $link (code ${g.group.inviteCode})")
        }
        ctx.startActivity(Intent.createChooser(send, "Invite"))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    with(sharedScope) {
                        Text(
                            s?.group?.name ?: "",
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState("title-${viewModel.groupId}"), animatedScope,
                            ),
                        )
                    }
                },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (s != null) {
                        IconButton(onClick = { invite(s) }) { Icon(Icons.Default.PersonAdd, "Invite") }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "More options") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Edit group & members") },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = { menuOpen = false; showManage = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Share invite") },
                                    leadingIcon = { Icon(Icons.Default.Share, null) },
                                    onClick = { menuOpen = false; invite(s) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete group", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { menuOpen = false; showDeleteGroup = true },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            if (s != null) ExtendedFloatingActionButton(
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick); showAdd = true },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add expense") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        if (s == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val myId = me?.id
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.lg, end = Spacing.lg,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + Spacing.fabClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "hero") { Hero(s, myId, onSettle) }
            item(key = "tabs") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = Spacing.xxs)) {
                    listOf("Expenses", "Balances", "Activity").forEachIndexed { i, t ->
                        SegmentedButton(
                            selected = tab == i, onClick = { tab = i },
                            shape = SegmentedButtonDefaults.itemShape(i, 3),
                            icon = {},
                        ) { Text(t) }
                    }
                }
            }
            when (tab) {
                0 -> expenseSection(
                    s, myId,
                    onEdit = { editing = it },
                    onDelete = { e ->
                        viewModel.deleteExpense(e)
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val r = snackbar.showSnackbar("Deleted \"${e.title}\"", actionLabel = "Undo", duration = SnackbarDuration.Short)
                            if (r == SnackbarResult.ActionPerformed) viewModel.restoreExpense(e)
                        }
                    },
                )
                1 -> balanceSection(s, myId)
                else -> activitySection(s)
            }
        }
    }

    if ((showAdd || editing != null) && s != null) {
        val target = editing
        AddExpenseSheet(
            group = s.group,
            myId = me?.id,
            initial = target,
            onDismiss = { showAdd = false; editing = null },
            suggestRate = { cur -> viewModel.suggestedRate(cur, s.group.baseCurrency) },
            onAddMember = { name -> viewModel.addMember(name) },
            onSave = { title, amt, cur, rate, cat, paidBy, type, inputs, rec ->
                viewModel.saveExpense(title, amt, cur, rate, cat, paidBy, type, inputs, rec, s.group.baseCurrency, target)
            },
        )
    }

    if (showManage && s != null) {
        ManageGroupSheet(
            group = s.group,
            myId = me?.id,
            onDismiss = { showManage = false },
            onSaveDetails = { name, type -> viewModel.updateGroupDetails(name, type) },
            onAddMember = { name -> viewModel.addMember(name) },
            onRename = { m, name -> viewModel.renameMember(m, name) },
            onRemove = { m -> viewModel.removeMember(m) },
        )
    }

    if (showDeleteGroup && s != null) {
        ConfirmDeleteDialog(
            title = "Delete \"${s.group.name}\"?",
            message = "This permanently removes the group with its ${s.expenses.size} expense" +
                "${if (s.expenses.size == 1) "" else "s"}, payments and activity. This can't be undone.",
            confirmLabel = "Delete group",
            onConfirm = {
                showDeleteGroup = false
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.deleteGroup()
                onBack()
            },
            onDismiss = { showDeleteGroup = false },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Hero
// ---------------------------------------------------------------------------------------------

@Composable
private fun Hero(s: GroupState, myId: String?, onSettle: () -> Unit) {
    val my = s.balances[myId] ?: 0L
    val cur = s.group.baseCurrency
    val total = s.expenses.sumOf { Math.round(it.amount * it.rateToBase) }

    BrandCard {
        Text(
            when { my > 0 -> "You are owed"; my < 0 -> "You owe"; else -> "Your balance" },
            color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(Spacing.xxs))
        if (my == 0L) {
            Text("All settled up", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        } else {
            AnimatedMoney(my, cur, style = MaterialTheme.typography.displaySmall, color = Color.White)
        }
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            "${formatMoney(total, cur)} spent · ${s.expenses.size} expense${if (s.expenses.size == 1) "" else "s"}",
            color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarStack(
                s.group.members.map { it.name }, ring = Color(0xFF2B6F8E), size = 34.dp,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onSettle,
                enabled = s.transfers.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, contentColor = Color(0xFF0F766E),
                    disabledContainerColor = Color.White.copy(alpha = 0.25f), disabledContentColor = Color.White.copy(alpha = 0.7f),
                ),
            ) {
                Icon(Icons.Default.Paid, null, Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Settle up")
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Expenses
// ---------------------------------------------------------------------------------------------

private fun LazyListScope.expenseSection(
    s: GroupState, myId: String?, onEdit: (Expense) -> Unit, onDelete: (Expense) -> Unit,
) {
    if (s.expenses.isEmpty()) {
        item(key = "empty-expenses") {
            EmptyState(Icons.Default.Receipt, "No expenses yet", "Tap “Add expense” to log the first one.")
        }
        return
    }
    val names = s.group.members.associate { it.id to it.name }
    // Expenses arrive newest-first, so groupBy keeps the day buckets in the right order.
    s.expenses.groupBy { dayLabel(it.createdAt) }.forEach { (day, list) ->
        item(key = "day-$day") {
            SectionLabel(day, Modifier.padding(top = Spacing.xs))
        }
        items(list, key = { it.id }) { e ->
            ExpenseRow(e, myId, names, onEdit, onDelete, Modifier.animateItem())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseRow(
    e: Expense, myId: String?, names: Map<String, String>,
    onEdit: (Expense) -> Unit, onDelete: (Expense) -> Unit, modifier: Modifier = Modifier,
) {
    val debt = LocalDebtColors.current
    val dismiss = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        if (v == SwipeToDismissBoxValue.EndToStart) { onDelete(e); true } else false
    })
    val myShare = e.splits.firstOrNull { it.memberId == myId }?.amount ?: 0L
    val lent = e.amount - myShare

    SwipeToDismissBox(
        state = dismiss,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(Radius.row))
                    .background(MaterialTheme.colorScheme.errorContainer).padding(end = Spacing.xl),
                contentAlignment = Alignment.CenterEnd,
            ) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onErrorContainer) }
        },
    ) {
        Surface(
            onClick = { onEdit(e) },
            shape = RoundedCornerShape(Radius.row),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(e.category)
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(e.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val payer = if (e.paidBy == myId) "You" else names[e.paidBy] ?: "Someone"
                    Text(
                        "$payer paid ${formatMoney(e.amount, e.currency)}" +
                            if (e.recurrence != Recurrence.NONE) " · ${e.recurrence.name.lowercase()}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(Spacing.xs))
                Column(horizontalAlignment = Alignment.End) {
                    when {
                        e.paidBy == myId && lent > 0 -> {
                            Text("you lent", style = MaterialTheme.typography.labelSmall, color = debt.owed)
                            Text(formatMoney(lent, e.currency), fontWeight = FontWeight.Bold, color = debt.owed)
                        }
                        e.paidBy != myId && myShare > 0 -> {
                            Text("you owe", style = MaterialTheme.typography.labelSmall, color = debt.owe)
                            Text(formatMoney(myShare, e.currency), fontWeight = FontWeight.Bold, color = debt.owe)
                        }
                        else -> Text(
                            "not involved", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Balances
// ---------------------------------------------------------------------------------------------

private fun LazyListScope.balanceSection(s: GroupState, myId: String?) {
    val names = s.group.members.associate { it.id to it.name }
    val cur = s.group.baseCurrency

    item(key = "bal-label") { SectionLabel("Who owes what", Modifier.padding(top = Spacing.xs)) }
    items(s.group.members, key = { "m-${it.id}" }) { m ->
        val b = s.balances[m.id] ?: 0L
        val debt = LocalDebtColors.current
        Surface(
            shape = RoundedCornerShape(Radius.row),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().animateItem(),
        ) {
            Row(Modifier.padding(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Avatar(m.name, size = 42.dp)
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(if (m.id == myId) "You" else m.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        when { b > 0 -> "gets back"; b < 0 -> "owes"; else -> "settled up" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (b != 0L) Text(
                    formatMoney(b, cur), fontWeight = FontWeight.Bold,
                    color = if (b > 0) debt.owed else debt.owe,
                )
            }
        }
    }

    if (s.transfers.isNotEmpty()) {
        item(key = "pay-label") { SectionLabel("Simplified payments", Modifier.padding(top = Spacing.sm)) }
        items(s.transfers, key = { "t-${it.from}-${it.to}" }) { t ->
            Surface(
                shape = RoundedCornerShape(Radius.row),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().animateItem(),
            ) {
                Row(Modifier.padding(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(names[t.from] ?: "?", size = 36.dp)
                    Icon(Icons.Default.ArrowForward, null, Modifier.padding(horizontal = Spacing.xs).size(18.dp))
                    Avatar(names[t.to] ?: "?", size = 36.dp)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "${if (t.from == myId) "You" else names[t.from]} pay${if (t.from == myId) "" else "s"} " +
                            "${if (t.to == myId) "you" else names[t.to]}",
                        Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(formatMoney(t.amount, cur), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Activity
// ---------------------------------------------------------------------------------------------

private fun LazyListScope.activitySection(s: GroupState) {
    if (s.activity.isEmpty()) {
        item(key = "empty-activity") {
            EmptyState(Icons.Default.Receipt, "No activity yet", "Everything that happens in this group shows up here.")
        }
        return
    }
    items(s.activity, key = { "a-${it.id}" }) { a ->
        val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        val (icon, tint) = when (a.type) {
            ActivityType.SETTLED -> Icons.Default.Paid to Color(0xFF16A34A)
            ActivityType.EXPENSE_DELETED -> Icons.Default.Delete to Color(0xFFDC2626)
            ActivityType.MEMBER_JOINED -> Icons.Default.PersonAdd to Color(0xFF0284C7)
            ActivityType.GROUP_CREATED -> Icons.Default.Groups to Color(0xFF7C3AED)
            ActivityType.MEMBER_REMOVED -> Icons.Default.Delete to Color(0xFFDC2626)
            ActivityType.GROUP_EDITED, ActivityType.EXPENSE_EDITED -> Icons.Default.Edit to Color(0xFFEA580C)
            else -> Icons.Default.Receipt to Color(0xFF0F766E)
        }
        Row(Modifier.fillMaxWidth().animateItem().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Text("${a.actorName} ${a.description}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    df.format(Date(a.createdAt)), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
