package com.example.neurozen_front.presentation.worker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.example.neurozen_front.domain.model.LibraryResource
import com.example.neurozen_front.domain.repository.LibraryRepository
import com.example.neurozen_front.presentation.components.NzTextField
import com.example.neurozen_front.presentation.components.NzTopBar
import com.example.neurozen_front.presentation.components.Pill
import com.example.neurozen_front.presentation.components.PrimaryButton
import com.example.neurozen_front.presentation.components.SectionCard
import com.example.neurozen_front.presentation.components.StateContent
import com.example.neurozen_front.presentation.components.UiState
import com.example.neurozen_front.presentation.components.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun typeLabel(type: String) = when (type.lowercase()) {
    "video" -> "Video"
    "audio", "podcast", "meditation" -> "Audio"
    else -> "Lectura"
}

@HiltViewModel
class LibraryViewModel @Inject constructor(private val library: LibraryRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<LibraryResource>>>(UiState.Loading)
    val state = _state.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch { _state.value = UiState.Loading; _state.value = library.getAll().toUiState() }
}

@Composable
fun LibraryScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, vm: LibraryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { NzTopBar("Biblioteca", onBack) }) { padding ->
        Column(Modifier.padding(padding)) {
            StateContent(state, onRetry = { vm.load() }, emptyMessage = "La biblioteca aún no tiene recursos.") { all ->
                val filtered = all.filter { r ->
                    (type == null || typeLabel(r.type) == type) &&
                        (query.isBlank() || r.title.contains(query, true) || r.tags.any { it.contains(query, true) })
                }
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { NzTextField(query, { query = it }, "Buscar por título o etiqueta") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Lectura", "Audio", "Video")) { t ->
                                FilterChip(selected = type == t, onClick = { type = if (type == t) null else t }, label = { Text(t) })
                            }
                        }
                    }
                    if (filtered.isEmpty()) item { Text("No hay resultados con esos filtros.") }
                    items(filtered, key = { it.id }) { r ->
                        SectionCard(Modifier.clickable { onOpen(r.id) }) {
                            r.thumbnail?.let {
                                AsyncImage(it, contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)))
                            }
                            Text(r.title, style = MaterialTheme.typography.titleMedium)
                            Text(r.description, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Pill("${typeLabel(r.type)} · ${r.durationMinutes} min", MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@HiltViewModel
class LibraryDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val library: LibraryRepository
) : ViewModel() {
    private val id: Long = savedState["id"] ?: 0L
    private val _state = MutableStateFlow<UiState<LibraryResource>>(UiState.Loading)
    val state = _state.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch { _state.value = UiState.Loading; _state.value = library.getById(id).toUiState { false } }
}

@Composable
fun LibraryDetailScreen(onBack: () -> Unit, vm: LibraryDetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val uri = LocalUriHandler.current
    Scaffold(topBar = { NzTopBar("Recurso", onBack) }) { padding ->
        Column(Modifier.padding(padding)) {
            StateContent(state, onRetry = { vm.load() }) { r ->
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    r.thumbnail?.let {
                        AsyncImage(it, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)))
                    }
                    Text(r.title, style = MaterialTheme.typography.headlineSmall)
                    Pill("${typeLabel(r.type)} · ${r.durationMinutes} min", MaterialTheme.colorScheme.secondary)
                    r.author?.let { Text("Por $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(r.description)
                    r.content?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    r.contentUrl?.let { url ->
                        PrimaryButton(if (typeLabel(r.type) == "Lectura") "Abrir enlace" else "Reproducir ${typeLabel(r.type).lowercase()}", onClick = { uri.openUri(url) })
                    }
                    if (r.tags.isNotEmpty()) Text(r.tags.joinToString("  ") { "#$it" }, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
