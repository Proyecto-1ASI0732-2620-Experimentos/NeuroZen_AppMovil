package com.example.neurozen_front.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.StressLevel
import com.example.neurozen_front.presentation.theme.NzColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NzTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun LoadingView(message: String = "Cargando…") {
    Column(
        Modifier.fillMaxSize().padding(32.dp).semantics { contentDescription = message },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Text(
            "Si es el primer intento del día, el servidor puede tardar hasta un minuto en despertar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun ErrorView(message: String, onRetry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

@Composable
fun EmptyView(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Renders the right view for each state; [content] only runs on success. */
@Composable
fun <T> StateContent(
    state: UiState<T>,
    onRetry: () -> Unit,
    emptyMessage: String = "Aún no hay nada por aquí.",
    emptyAction: Pair<String, () -> Unit>? = null,
    content: @Composable (T) -> Unit
) {
    when (state) {
        UiState.Loading -> LoadingView()
        UiState.Empty -> EmptyView(emptyMessage, emptyAction?.first, emptyAction?.second)
        is UiState.Error -> ErrorView(state.message, onRetry)
        is UiState.Success -> content(state.data)
    }
}

@Composable
fun NzTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    supporting: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = nzFieldColors()
    )
}

@Composable
fun nzFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
)

@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String = "Contraseña", error: String? = null) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = nzFieldColors()
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

fun stressColor(level: StressLevel?): Color = when (level) {
    StressLevel.LOW -> NzColors.Low
    StressLevel.MODERATE -> NzColors.Moderate
    StressLevel.HIGH -> NzColors.High
    null -> NzColors.Stone
}

fun statusColor(status: AppointmentStatus): Color = when (status) {
    AppointmentStatus.PENDING -> NzColors.Moderate
    AppointmentStatus.CONFIRMED -> NzColors.Turquoise
    AppointmentStatus.COMPLETED -> NzColors.Low
    AppointmentStatus.CANCELLED, AppointmentStatus.UNKNOWN -> NzColors.Stone
}

@Composable
fun StatusPill(status: AppointmentStatus) = Pill(status.label, statusColor(status))

@Composable
fun InfoBanner(text: String, color: Color = MaterialTheme.colorScheme.secondary) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.12f)).padding(12.dp)
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Original NeuroZen error box (errorContainer, rounded 12). */
@Composable
fun ErrorBanner(text: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth()) {
        Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/** Original NeuroZen success box. */
@Composable
fun SuccessBanner(text: String, modifier: Modifier = Modifier) {
    Surface(color = NzColors.SuccessBg, shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth()) {
        Text(text, color = NzColors.SuccessText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/** Bottom bar item colors from the original app (worker uses primary, psychologist secondary). */
@Composable
fun nzNavItemColors(usePrimary: Boolean = true): NavigationBarItemColors = NavigationBarItemDefaults.colors(
    selectedIconColor = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
    selectedTextColor = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    indicatorColor = if (usePrimary) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
)

/** Emoji inside a soft circle, as used in the original cards. */
@Composable
fun EmojiCircle(emoji: String, size: Int = 48, color: Color = MaterialTheme.colorScheme.secondaryContainer) {
    Surface(modifier = Modifier.size(size.dp), shape = CircleShape, color = color) {
        Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = (size / 2.4).sp) }
    }
}

/** Small stat card with emoji (original "StatBadge"). */
@Composable
fun StatBadge(label: String, value: String, icon: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 20.sp)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Section title used in the original home ("Tu equilibrio", "Recomendado para ti"). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

/** Brand header: deep-green gradient with rounded bottom corners and an optional trailing slot. */
@Composable
fun GradientHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
    bottom: @Composable ColumnScope.() -> Unit = {}
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(com.example.neurozen_front.presentation.theme.Forest900, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 26.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.82f))
                }
                trailing()
            }
            bottom()
        }
    }
}
