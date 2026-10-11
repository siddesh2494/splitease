package com.splitease.app.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.splitease.app.domain.model.Group
import com.splitease.app.domain.model.GroupType
import com.splitease.app.domain.model.Member
import com.splitease.app.ui.common.*
import com.splitease.app.ui.theme.Radius
import com.splitease.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Edit group name/type and manage members.
 * Member changes apply immediately; name/type apply with "Save details".
 * [onRemove] returns an explanation if the person can't be removed yet (still in expenses), else null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageGroupSheet(
    group: Group,
    myId: String?,
    onDismiss: () -> Unit,
    onSaveDetails: (String, GroupType) -> Unit,
    onAddMember: suspend (String) -> String?,
    onRename: (Member, String) -> Unit,
    onRemove: suspend (Member) -> String?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(group.name) }
    var type by remember { mutableStateOf(group.type) }
    var newMember by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<Member?>(null) }
    var removing by remember { mutableStateOf<Member?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val detailsChanged = name.trim() != group.name || type != group.type

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg)
                .navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Edit group", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                name, { name = it }, label = { Text("Group name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
            )
            GroupTypePicker(selected = type, onSelect = { type = it })
            FilledTonalButton(
                onClick = { onSaveDetails(name, type) },
                enabled = detailsChanged && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save details") }

            HorizontalDivider()

            SectionLabel("Members (${group.members.size})")
            Surface(shape = RoundedCornerShape(Radius.row), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs)) {
                    group.members.forEach { m ->
                        Row(Modifier.padding(vertical = Spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(m.name, size = 36.dp)
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                if (m.id == myId) "${m.name} (you)" else m.name,
                                Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                            )
                            IconButton(onClick = { renaming = m }) { Icon(Icons.Default.Edit, "Rename ${m.name}") }
                            if (m.id != myId) {
                                IconButton(onClick = { message = null; removing = m }) {
                                    Icon(Icons.Default.Delete, "Remove ${m.name}", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            message?.let {
                Surface(shape = RoundedCornerShape(Radius.sm), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        it, Modifier.padding(Spacing.sm), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    newMember, { newMember = it }, singleLine = true, modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium, label = { Text("Add a member") },
                )
                Spacer(Modifier.width(Spacing.xs))
                FilledTonalIconButton(
                    enabled = newMember.isNotBlank(),
                    onClick = {
                        val n = newMember
                        scope.launch { onAddMember(n); newMember = ""; message = null }
                    },
                ) { Icon(Icons.Default.PersonAdd, "Add member") }
            }
            Spacer(Modifier.height(Spacing.md))
        }
    }

    renaming?.let { m ->
        TextInputDialog(
            title = "Rename member", initial = m.name, label = "Name",
            onConfirm = { onRename(m, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }

    removing?.let { m ->
        ConfirmDeleteDialog(
            title = "Remove ${m.name}?",
            message = "They'll be taken out of this group. People who are still part of an expense or payment can't be removed until those are edited.",
            confirmLabel = "Remove",
            onConfirm = {
                removing = null
                scope.launch { message = onRemove(m) }
            },
            onDismiss = { removing = null },
        )
    }
}
