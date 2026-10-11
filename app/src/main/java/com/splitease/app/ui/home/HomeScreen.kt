package com.splitease.app.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.splitease.app.data.local.entity.GroupEntity
import com.splitease.app.domain.model.GroupType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenGroup: (String) -> Unit,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
) {
    val groups by viewModel.groups.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("Your groups") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick); showCreate = true },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("New group") },
            )
        },
        // Scaffold applies system-bar insets to innerPadding -> edge-to-edge friendly.
    ) { padding ->
        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Create a group to start splitting", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(groups, key = { it.id }) { g ->
                    GroupCard(g, onOpenGroup, sharedScope, animatedScope)
                }
            }
        }
    }

    if (showCreate) CreateGroupDialog(
        onDismiss = { showCreate = false },
        onCreate = { name, type, cur, members ->
            viewModel.createGroup(name, type, cur, members); showCreate = false
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun GroupCard(
    g: GroupEntity,
    onOpen: (String) -> Unit,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
) {
    val icon = when (GroupType.valueOf(g.type)) {
        GroupType.TRIP -> Icons.Default.Flight
        GroupType.HOME -> Icons.Default.Home
        GroupType.COUPLE -> Icons.Default.Favorite
        GroupType.PROJECT -> Icons.Default.Work
    }
    with(sharedScope) {
        ElevatedCard(onClick = { onOpen(g.id) }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        g.name, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState("title-${g.id}"), animatedScope,
                        ),
                    )
                    Text(
                        "${g.type.lowercase().replaceFirstChar { it.uppercase() }} · ${g.baseCurrency}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(Icons.Default.ChevronRight, null)
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String, GroupType, String, List<String>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(GroupType.TRIP) }
    var currency by remember { mutableStateOf("INR") }
    var members by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    name, { name = it }, label = { Text("Group name") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GroupType.entries.forEach {
                        FilterChip(type == it, { type = it }, label = { Text(it.name.lowercase().replaceFirstChar(Char::uppercase)) })
                    }
                }
                OutlinedTextField(
                    currency, { currency = it.uppercase().take(3) }, label = { Text("Currency (ISO code)") }, singleLine = true,
                )
                OutlinedTextField(
                    members, { members = it }, label = { Text("Other members (comma separated)") },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && currency.length == 3,
                onClick = { onCreate(name, type, currency, members.split(",")) },
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}
