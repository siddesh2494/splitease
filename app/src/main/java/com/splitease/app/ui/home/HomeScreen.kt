package com.splitease.app.ui.home

import com.splitease.app.ui.theme.Radius
import com.splitease.app.ui.theme.Spacing
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.splitease.app.data.repository.GroupSummary
import com.splitease.app.domain.model.GroupType
import com.splitease.app.ui.common.*
import com.splitease.app.ui.theme.LocalDebtColors

private val POPULAR_CURRENCIES = listOf("INR", "USD", "EUR", "GBP", "AED", "THB", "SGD", "JPY")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenGroup: (String) -> Unit,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
) {
    val groups by viewModel.groups.collectAsState()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<GroupSummary?>(null) }
    val haptics = LocalHapticFeedback.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick); showCreate = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New group") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = Spacing.lg, end = Spacing.lg,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + Spacing.fabClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "header") {
                Column {
                    Text("Welcome back", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("SplitEase", style = MaterialTheme.typography.headlineLarge)
                }
            }
            item(key = "hero") { OverviewCard(groups) }
            item(key = "label") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your groups", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (groups.isNotEmpty()) Text(
                        "${groups.size}", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (groups.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        Icons.Default.Groups, "No groups yet",
                        "Create a group for a trip, your flat or a project, and start splitting bills.",
                    )
                }
            }
            items(groups, key = { it.id }) { g ->
                GroupCard(
                    g = g, onOpen = onOpenGroup, onDelete = { toDelete = g },
                    sharedScope = sharedScope, animatedScope = animatedScope,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (showCreate) CreateGroupSheet(
        onDismiss = { showCreate = false },
        onCreate = { name, type, cur, members ->
            viewModel.createGroup(name, type, cur, members); showCreate = false
        },
    )

    toDelete?.let { g ->
        ConfirmDeleteDialog(
            title = "Delete \"${g.name}\"?",
            message = "This permanently removes the group with its ${g.expenseCount} expense" +
                "${if (g.expenseCount == 1) "" else "s"}, payments and activity. This can't be undone.",
            confirmLabel = "Delete group",
            onConfirm = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.deleteGroup(g.id); toDelete = null
            },
            onDismiss = { toDelete = null },
        )
    }
}

/** Gradient hero: total you're owed / you owe across all groups (per currency). */
@Composable
private fun OverviewCard(groups: List<GroupSummary>) {
    val byCurrency = groups.groupBy { it.currency }
    // Most-used currency first; extra currencies are summarised in one line.
    val ordered = byCurrency.entries.sortedByDescending { it.value.size }
    val primary = ordered.firstOrNull()
    val owed = primary?.value?.sumOf { maxOf(it.myBalance, 0L) } ?: 0L
    val owe = primary?.value?.sumOf { maxOf(-it.myBalance, 0L) } ?: 0L
    val cur = primary?.key ?: "INR"
    val net = owed - owe

    BrandCard {
        Text("Overall balance", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(Spacing.xxs))
        if (groups.isEmpty() || net == 0L) {
            Text(
                if (groups.isEmpty()) "No groups yet" else "All settled up",
                color = Color.White, style = MaterialTheme.typography.headlineMedium,
            )
        } else {
            AnimatedMoney(
                net, cur, style = MaterialTheme.typography.displaySmall, color = Color.White,
            )
            Text(
                if (net > 0) "overall, you are owed" else "overall, you owe",
                color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatPill("You are owed", formatMoney(owed, cur), Icons.Default.ArrowDownward, Modifier.weight(1f))
            StatPill("You owe", formatMoney(owe, cur), Icons.Default.ArrowUpward, Modifier.weight(1f))
        }
        if (ordered.size > 1) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Also active in ${ordered.drop(1).joinToString { it.key }}",
                color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(Radius.tile)).background(Color.White.copy(alpha = 0.16f)).padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.xs))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun GroupCard(
    g: GroupSummary,
    onOpen: (String) -> Unit,
    onDelete: () -> Unit,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val debt = LocalDebtColors.current
    var menuOpen by remember { mutableStateOf(false) }

    with(sharedScope) {
        Surface(
            onClick = { onOpen(g.id) },
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radius.card),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
        ) {
            Row(Modifier.padding(start = Spacing.md, top = Spacing.md, bottom = Spacing.md, end = Spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
                GradientIconBox(g.type.icon(), g.type.gradient(), size = 52.dp)
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        g.name, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState("title-${g.id}"), animatedScope,
                        ),
                    )
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        "${g.memberNames.size} members · ${g.type.label()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    AvatarStack(g.memberNames, ring = MaterialTheme.colorScheme.surfaceContainerLow, size = 24.dp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    when {
                        g.myBalance > 0 -> {
                            Text("you're owed", style = MaterialTheme.typography.labelSmall, color = debt.owed)
                            Text(formatMoney(g.myBalance, g.currency), fontWeight = FontWeight.Bold, color = debt.owed)
                        }
                        g.myBalance < 0 -> {
                            Text("you owe", style = MaterialTheme.typography.labelSmall, color = debt.owe)
                            Text(formatMoney(g.myBalance, g.currency), fontWeight = FontWeight.Bold, color = debt.owe)
                        }
                        else -> Text(
                            "settled up", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "More options") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete group", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreateGroupSheet(
    onDismiss: () -> Unit,
    onCreate: (String, GroupType, String, List<String>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(GroupType.TRIP) }
    var currency by remember { mutableStateOf("INR") }
    var members by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg)
                .navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("New group", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                name, { name = it }, label = { Text("Group name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )

            SectionLabel("Type")
            GroupTypePicker(selected = type, onSelect = { type = it })

            SectionLabel("Currency")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                POPULAR_CURRENCIES.forEach {
                    FilterChip(currency == it, { currency = it }, label = { Text(it) })
                }
            }

            OutlinedTextField(
                members, { members = it }, label = { Text("Other members (comma separated)") },
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                supportingText = { Text("You're added automatically. You can share an invite link later.") },
            )

            Button(
                onClick = { onCreate(name, type, currency, members.split(",")) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text("Create group") }
            Spacer(Modifier.height(Spacing.md))
        }
    }
}
