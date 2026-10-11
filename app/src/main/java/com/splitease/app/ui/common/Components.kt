package com.splitease.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitease.app.domain.model.ExpenseCategory
import com.splitease.app.domain.model.GroupType
import com.splitease.app.ui.theme.LocalBrandBrush
import com.splitease.app.ui.theme.Radius
import com.splitease.app.ui.theme.Spacing

// ---------- Domain → visual mappings ----------

fun GroupType.icon(): ImageVector = when (this) {
    GroupType.TRIP -> Icons.Default.Flight
    GroupType.HOME -> Icons.Default.Home
    GroupType.COUPLE -> Icons.Default.Favorite
    GroupType.PROJECT -> Icons.Default.Work
}

fun GroupType.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

fun GroupType.gradient(): Brush = when (this) {
    GroupType.TRIP -> Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)))
    GroupType.HOME -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF0D9488)))
    GroupType.COUPLE -> Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFF97316)))
    GroupType.PROJECT -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)))
}

fun ExpenseCategory.icon(): ImageVector = when (this) {
    ExpenseCategory.FOOD -> Icons.Default.Restaurant
    ExpenseCategory.TRAVEL -> Icons.Default.Flight
    ExpenseCategory.UTILITIES -> Icons.Default.Bolt
    ExpenseCategory.ENTERTAINMENT -> Icons.Default.Movie
    ExpenseCategory.RENT -> Icons.Default.Home
    ExpenseCategory.SHOPPING -> Icons.Default.ShoppingBag
    ExpenseCategory.OTHER -> Icons.Default.Receipt
}

fun ExpenseCategory.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

fun ExpenseCategory.tint(): Color = when (this) {
    ExpenseCategory.FOOD -> Color(0xFFF97316)
    ExpenseCategory.TRAVEL -> Color(0xFF0EA5E9)
    ExpenseCategory.UTILITIES -> Color(0xFFEAB308)
    ExpenseCategory.ENTERTAINMENT -> Color(0xFFA855F7)
    ExpenseCategory.RENT -> Color(0xFF10B981)
    ExpenseCategory.SHOPPING -> Color(0xFFEC4899)
    ExpenseCategory.OTHER -> Color(0xFF64748B)
}

// ---------- Building blocks ----------

private val AvatarPalette = listOf(
    Color(0xFF0F766E), Color(0xFF4F46E5), Color(0xFFDB2777), Color(0xFFEA580C),
    Color(0xFF0284C7), Color(0xFF7C3AED), Color(0xFF16A34A), Color(0xFFCA8A04),
)

/** Round initial avatar; color is derived from the name so a person always looks the same. */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val color = AvatarPalette[(name.hashCode() and Int.MAX_VALUE) % AvatarPalette.size]
    Box(
        modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** Overlapping avatars, e.g. group members. [ring] should match the background behind them. */
@Composable
fun AvatarStack(
    names: List<String>, ring: Color, modifier: Modifier = Modifier,
    max: Int = 4, size: Dp = 32.dp,
) {
    val shown = names.take(max)
    val extra = names.size - shown.size
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((-(size.value * 0.3f)).dp)) {
        shown.forEach { Avatar(it, Modifier.border(2.dp, ring, CircleShape), size) }
        if (extra > 0) {
            Box(
                Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, ring, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("+$extra", fontSize = (size.value * 0.34f).sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Rounded square with a gradient and a white icon. */
@Composable
fun GradientIconBox(icon: ImageVector, brush: Brush, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size / 3)).background(brush),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
fun CategoryBadge(category: ExpenseCategory, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val tint = category.tint()
    Box(
        modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) { Icon(category.icon(), null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

/** Hero container with the signature gradient. Text inside should be white. */
@Composable
fun BrandCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.hero))
            .background(LocalBrandBrush.current).padding(Spacing.xl),
        content = content,
    )
}

/** Money text that counts up to its value, so balance changes feel alive. */
@Composable
fun AnimatedMoney(
    minor: Long, currency: String, style: TextStyle, color: Color, modifier: Modifier = Modifier,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(minor) { anim.animateTo(minor.toFloat(), tween(800)) }
    Text(formatMoney(anim.value.toLong(), currency), modifier = modifier, style = style, color = color, maxLines = 1)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier, style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = Spacing.xxl, horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(Spacing.md))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Four selectable tiles (trip / home / couple / project), shared by create and edit group. */
@Composable
fun GroupTypePicker(selected: GroupType, onSelect: (GroupType) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        GroupType.entries.forEach { t ->
            val isSelected = selected == t
            val shape = RoundedCornerShape(Radius.tile)
            Column(
                Modifier.weight(1f).clip(shape)
                    .border(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape,
                    )
                    .clickable { onSelect(t) }.padding(vertical = Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GradientIconBox(t.icon(), t.gradient(), size = 40.dp)
                Spacer(Modifier.height(Spacing.xs))
                Text(t.label(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Single-field dialog, used for renaming. */
@Composable
fun TextInputDialog(
    title: String, initial: String, label: String,
    onConfirm: (String) -> Unit, onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                text, { text = it }, label = { Text(label) }, singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String, message: String, confirmLabel: String = "Delete",
    onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
