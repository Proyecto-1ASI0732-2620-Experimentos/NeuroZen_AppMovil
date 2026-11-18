package com.example.neurozen_front.presentation.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.example.neurozen_front.domain.logic.YouTube
import com.example.neurozen_front.domain.model.LibraryResource
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.isPlanRequired
import com.example.neurozen_front.domain.repository.LibraryRepository
import com.example.neurozen_front.domain.repository.SubscriptionRepository
import com.example.neurozen_front.presentation.components.Accent
import com.example.neurozen_front.presentation.components.ErrorView
import com.example.neurozen_front.presentation.components.IconBadge
import com.example.neurozen_front.presentation.components.LoadingView
import com.example.neurozen_front.presentation.components.NzTopBar
import com.example.neurozen_front.presentation.components.PrimaryButton
import com.example.neurozen_front.presentation.components.ReadingText
import com.example.neurozen_front.presentation.components.SectionCard
import com.example.neurozen_front.presentation.components.StateContent
import com.example.neurozen_front.presentation.components.UiState
import com.example.neurozen_front.presentation.components.YouTubePlayer
import com.example.neurozen_front.presentation.components.nzFieldColors
import com.example.neurozen_front.presentation.components.toUiState
import com.example.neurozen_front.presentation.theme.Emerald600
import com.example.neurozen_front.presentation.theme.Forest700
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Resource kinds as the user sees them (backend resourceType: Video, Audio, Article, Exercise). */
enum class ResourceKind(val label: String, val plural: String, val icon: ImageVector, val color: Color) {
    VIDEO("Video", "Videos", Icons.Outlined.PlayCircle, Accent.Coral),
    AUDIO("Audio", "Audios", Icons.Outlined.Headphones, Accent.Purple),
    READING("Lectura", "Lecturas", Icons.AutoMirrored.Outlined.MenuBook, Accent.Blue),
    EXERCISE("Ejercicio", "Ejercicios", Icons.Outlined.SelfImprovement, Accent.Teal);

    companion object {
        fun of(type: String): ResourceKind = when (type.lowercase()) {
            "video" -> VIDEO
            "audio", "podcast", "meditation", "guided_meditation", "music" -> AUDIO
            "exercise", "exercises", "physical" -> EXERCISE
            else -> READING
        }
    }
}

internal fun typeLabel(type: String) = ResourceKind.of(type).label

/** "5 min de lectura" for readings; "6 min" for the rest. */
fun durationLabel(r: LibraryResource): String =
    if (ResourceKind.of(r.type) == ResourceKind.READING) "${r.durationMinutes} min de lectura" else "${r.durationMinutes} min"

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val subscriptions: SubscriptionRepository
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<LibraryResource>>>(UiState.Loading)
    val state = _state.asStateFlow()

    /** true = no active plan or Básico (planId 1). null = unknown (the banner is not shown). */
    private val _canUpgrade = MutableStateFlow<Boolean?>(null)
    val canUpgrade = _canUpgrade.asStateFlow()

    fun load() = viewModelScope.launch {
        if (_state.value !is UiState.Success) _state.value = UiState.Loading
        _state.value = library.getAll().toUiState()
        _canUpgrade.value = when (val r = subscriptions.getMine()) {
            is Outcome.Success -> r.data?.takeIf { it.isActive }?.planId.let { it == null || it <= 1 }
            is Outcome.Failure -> null
        }
    }
}

@Composable
fun LibraryScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, onUpgrade: () -> Unit = {}, vm: LibraryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val canUpgrade by vm.canUpgrade.collectAsState()
    // No cache: the plan or the admin's assignment may have changed (refreshes when coming back from Suscripción).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.load() }
    Scaffold(topBar = { NzTopBar("Biblioteca", onBack) }) { padding ->
        Column(Modifier.padding(padding)) {
            StateContent(state, onRetry = { vm.load() }, emptyMessage = "La biblioteca aún no tiene recursos.") { all ->
                LibraryContent(all, canUpgrade == true, onOpen, onUpgrade)
            }
        }
    }
}

/** Stateless list (also used by the screenshot tests). */
@Composable
fun LibraryContent(all: List<LibraryResource>, showUpgrade: Boolean, onOpen: (Long) -> Unit, onUpgrade: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf<ResourceKind?>(null) }
    val filtered = all.filter { r ->
        (kind == null || ResourceKind.of(r.type) == kind) &&
            (query.isBlank() || r.title.contains(query, true) || r.tags.any { it.contains(query, true) })
    }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                Text("Recursos para tu bienestar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                Text("${all.size} recursos disponibles en tu plan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (showUpgrade) item { UpgradeBanner(onUpgrade) }
        item { SearchField(query) { query = it } }
        item { KindChips(all, kind) { kind = it } }
        if (filtered.isEmpty()) item {
            Text("No hay resultados con esos filtros.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center)
        }
        items(filtered, key = { it.id }) { r ->
            val k = ResourceKind.of(r.type)
            if ((k == ResourceKind.VIDEO || k == ResourceKind.AUDIO) && r.thumbnail != null) MediaCard(r) { onOpen(r.id) }
            else CompactCard(r) { onOpen(r.id) }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query, onValueChange = onChange, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("library_search"),
        placeholder = { Text("Buscar por título o etiqueta") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Outlined.Close, contentDescription = "Borrar búsqueda") } },
        shape = RoundedCornerShape(16.dp), colors = nzFieldColors()
    )
}

@Composable
private fun KindChips(all: List<LibraryResource>, selected: ResourceKind?, onSelect: (ResourceKind?) -> Unit) {
    val counts = all.groupingBy { ResourceKind.of(it.type) }.eachCount()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Todos · ${all.size}") },
                leadingIcon = { Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = chipColors())
        }
        items(ResourceKind.entries.filter { (counts[it] ?: 0) > 0 }) { k ->
            FilterChip(selected = selected == k, onClick = { onSelect(if (selected == k) null else k) }, label = { Text("${k.plural} · ${counts[k] ?: 0}") },
                leadingIcon = { Icon(k.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }, colors = chipColors(),
                modifier = Modifier.testTag("chip_${k.name}"))
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White
)

@Composable
private fun UpgradeBanner(onUpgrade: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().testTag("upgrade_banner"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            Modifier.background(Brush.linearGradient(listOf(Forest700, Emerald600))).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.LockOpen, contentDescription = null, tint = Color.White)
                Text("Desbloquea toda la biblioteca", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text("Con el plan Avanzado accedes a todos los videos, audios, lecturas y ejercicios.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            TextButton(onClick = onUpgrade, modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White)) {
                Text("Mejorar plan", color = Forest700, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResourceCard(onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) { content() }
}

@Composable
private fun TypeTag(kind: ResourceKind, onDark: Boolean = false) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (onDark) Color.White.copy(alpha = 0.95f) else kind.color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(kind.icon, contentDescription = null, tint = kind.color, modifier = Modifier.size(14.dp))
        Text(kind.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = kind.color)
    }
}

/** Video or audio with thumbnail: 16:9 image, type tag and duration on top. */
@Composable
private fun MediaCard(r: LibraryResource, onClick: () -> Unit) {
    val kind = ResourceKind.of(r.type)
    ResourceCard(onClick) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(kind.color.copy(alpha = 0.12f))) {
                AsyncImage(r.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.padding(10.dp).align(Alignment.TopStart)) { TypeTag(kind, onDark = true) }
                Text(
                    durationLabel(r), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (r.description.isNotBlank()) Text(r.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Readings, exercises (and media without thumbnail): compact row with a type badge. */
@Composable
private fun CompactCard(r: LibraryResource, onClick: () -> Unit) {
    val kind = ResourceKind.of(r.type)
    ResourceCard(onClick) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(kind.icon, kind.color, size = 48)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (r.description.isNotBlank()) Text(r.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(kind.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = kind.color)
                    Text("·", color = MaterialTheme.colorScheme.outline)
                    Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Text(durationLabel(r), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Detail state: content, resource outside the plan (403 with the backend message) or another error. */
sealed interface ResourceDetailState {
    data object Loading : ResourceDetailState
    data class Ready(val resource: LibraryResource) : ResourceDetailState
    data class PlanRequired(val message: String) : ResourceDetailState
    data class Error(val message: String) : ResourceDetailState
}

@HiltViewModel
class LibraryDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val library: LibraryRepository
) : ViewModel() {
    private val id: Long = savedState["id"] ?: 0L
    private val _state = MutableStateFlow<ResourceDetailState>(ResourceDetailState.Loading)
    val state = _state.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch {
        _state.value = ResourceDetailState.Loading
        _state.value = when (val r = library.getById(id)) {
            is Outcome.Success -> ResourceDetailState.Ready(r.data)
            // «Este recurso está disponible desde el plan Avanzado»: not a generic error, the user can upgrade.
            is Outcome.Failure -> if (r.error.isPlanRequired()) ResourceDetailState.PlanRequired(r.error.message)
            else ResourceDetailState.Error(r.error.message)
        }
    }
}

@Composable
fun LibraryDetailScreen(onBack: () -> Unit, onUpgrade: () -> Unit = {}, vm: LibraryDetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val uri = LocalUriHandler.current
    val title = (state as? ResourceDetailState.Ready)?.resource?.let { typeLabel(it.type) } ?: "Recurso"
    Scaffold(topBar = { NzTopBar(title, onBack) }) { padding ->
        Column(Modifier.padding(padding)) {
            when (val s = state) {
                ResourceDetailState.Loading -> LoadingView()
                is ResourceDetailState.Error -> ErrorView(s.message) { vm.load() }
                is ResourceDetailState.PlanRequired -> PlanRequiredView(s.message, onUpgrade, onBack)
                is ResourceDetailState.Ready -> ResourceContent(s.resource, onOpenExternal = { uri.openUri(it) })
            }
        }
    }
}

@Composable
private fun PlanRequiredView(message: String, onUpgrade: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp).testTag("plan_required"),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(Icons.Outlined.Lock, MaterialTheme.colorScheme.primary, size = 72)
        Text("Recurso no incluido en tu plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        PrimaryButton("Mejorar plan", onUpgrade)
        TextButton(onClick = onBack) { Text("Volver a la biblioteca") }
    }
}

/** Stateless detail: reading page for readings/exercises; player on top for YouTube videos, audios and exercises. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResourceContent(r: LibraryResource, onOpenExternal: (String) -> Unit, showPlayer: Boolean = true) {
    val kind = ResourceKind.of(r.type)
    val videoId = YouTube.videoId(r.contentUrl)
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (videoId != null) {
            if (showPlayer) YouTubePlayer(videoId, r.title)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿No se reproduce aquí?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { onOpenExternal(YouTube.watchUrl(videoId)) }, modifier = Modifier.testTag("open_youtube")) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Abrir en YouTube")
                }
            }
        } else if (r.thumbnail != null && (kind == ResourceKind.VIDEO || kind == ResourceKind.AUDIO)) {
            AsyncImage(r.thumbnail, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)))
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TypeTag(kind)
            Text(r.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(durationLabel(r), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("resource_duration"))
                r.author?.let { Text("· Por $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (r.tags.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                r.tags.forEach { tag ->
                    Text("#$tag", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)).padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }

        // Full description: it may end with the clinical notice sent by the backend
        if (r.description.isNotBlank()) {
            Text(r.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("resource_description"))
        }
        r.content?.takeIf { it.isNotBlank() }?.let { content ->
            SectionCard {
                Text(if (kind == ResourceKind.EXERCISE) "Pasos" else "Lectura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ReadingText(content)
            }
        }
        if (videoId == null) r.contentUrl?.let { url ->
            PrimaryButton(if (kind == ResourceKind.READING) "Abrir enlace" else "Reproducir ${kind.label.lowercase()}", onClick = { onOpenExternal(url) })
        }
    }
}
