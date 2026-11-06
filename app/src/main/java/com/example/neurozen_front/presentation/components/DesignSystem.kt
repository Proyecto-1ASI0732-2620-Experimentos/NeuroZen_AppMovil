package com.example.neurozen_front.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.presentation.theme.Emerald600
import com.example.neurozen_front.presentation.theme.Forest700
import com.example.neurozen_front.presentation.theme.Forest900
import java.time.Instant
import java.time.format.TextStyle

/** Initials of a full name ("Ana María Pérez" -> "AP"). */
fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "NZ"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

/** Circular avatar: remote photo (https) or initials on the brand gradient, with an optional white ring. */
@Composable
fun Avatar(name: String, size: Int = 56, imageUrl: String? = null, ring: Boolean = false) {
    val ringModifier = if (ring) Modifier.border(3.dp, Color.White, CircleShape) else Modifier
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl, contentDescription = "Foto de $name", contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(CircleShape).then(ringModifier)
        )
    } else {
        Box(
            Modifier.size(size.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Forest700, Emerald600)))
                .then(ringModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(initialsOf(name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size / 2.6).sp)
        }
    }
}

/** Icon inside a tinted rounded square (list leading element). */
@Composable
fun IconBadge(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.55).dp)) }
}

/** Settings / navigation row: icon badge, title, optional subtitle, trailing value and chevron. */
@Composable
fun ListRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    destructive: Boolean = false,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else tint
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBadge(icon, color)
        Column(Modifier.weight(1f)) {
            Text(
                title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        trailingText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (onClick != null && showChevron) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

/** Grouped card of rows with a small caps title. Separate rows with [GroupDivider]. */
@Composable
fun SettingsGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Text(
            title.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) { Column(content = content) }
    }
}

@Composable
fun GroupDivider() = HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outlineVariant)

/** Compact metric tile: icon, big value and label. */
@Composable
fun StatTile(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            IconBadge(icon, tint, size = 32)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Translucent stat used on top of gradient headers. */
@Composable
fun HeaderStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.14f)).padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Calendar-like block with the day number and short month (Lima time). */
@Composable
fun DateBlock(instant: Instant, highlighted: Boolean = true) {
    val date = DateTimeUtils.toLima(instant)
    val bg = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.width(56.dp).clip(RoundedCornerShape(14.dp)).background(bg).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            date.month.getDisplayName(TextStyle.SHORT, DateTimeUtils.SPANISH).uppercase().removeSuffix("."),
            style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold
        )
        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = fg)
        Text("%02d:%02d".format(date.hour, date.minute), style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.85f))
    }
}

/** Section title with an optional action on the right ("Ver todo"). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

/** Progress card for profile completeness. */
@Composable
fun CompletionCard(title: String, done: Int, total: Int, hint: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("$done de $total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else done.toFloat() / total },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Big tappable tile for hubs (icon, title, subtitle) on a soft tinted background. */
@Composable
fun FeatureTile(icon: ImageVector, title: String, subtitle: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(icon, tint, size = 44)
            Spacer(Modifier.height(2.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Small rounded tag (duration, type, price…). */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

/** Brand colour accents for categories/tiles. */
object Accent {
    val Green = Forest700
    val Emerald = Emerald600
    val Teal = Color(0xFF00796B)
    val Blue = Color(0xFF3D6FB6)
    val Amber = Color(0xFFB7791F)
    val Coral = Color(0xFFC65D4A)
    val Purple = Color(0xFF6B5CA5)
    val Deep = Forest900
}

@Composable
fun CardColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = SectionCard(modifier, content)
