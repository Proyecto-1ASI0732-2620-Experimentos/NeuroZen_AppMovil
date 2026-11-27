package com.example.neurozen_front.presentation.worker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.neurozen_front.R
import com.example.neurozen_front.domain.logic.Catalogs
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.domain.logic.Recommendation
import com.example.neurozen_front.domain.logic.RecommendationAction
import com.example.neurozen_front.domain.logic.RecommendationEngine
import com.example.neurozen_front.domain.logic.StressScale
import com.example.neurozen_front.domain.logic.StressSource
import com.example.neurozen_front.domain.logic.StressSummary
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.presentation.components.Pill
import com.example.neurozen_front.presentation.components.SectionTitle
import com.example.neurozen_front.presentation.components.StateContent
import com.example.neurozen_front.presentation.components.StatusPill
import com.example.neurozen_front.presentation.components.UiMappings
import com.example.neurozen_front.presentation.components.stressColor

@Composable
fun HomeScreen(onAction: (RecommendationAction) -> Unit, onNavigate: (String) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        StateContent(state, onRetry = vm::load) { data -> HomeContent(data, onAction, onNavigate, onRefresh = vm::load) }
        ExtendedFloatingActionButton(
            onClick = { onNavigate("zenbot") },
            icon = { Icon(Icons.Outlined.Spa, contentDescription = null) },
            text = { Text(stringResource(R.string.zenbot_name)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}

private val HEADER_IMAGES = listOf(
    "https://images.unsplash.com/photo-1506126613408-eca07ce68773?q=80&w=1000",
    "https://images.unsplash.com/photo-1518199266791-5375a83190b7?q=80&w=1000",
    "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?q=80&w=1000",
    "https://images.unsplash.com/photo-1528715471579-d1bcf0ba5e83?q=80&w=1000"
)

private data class MetricItem(val label: String, val value: String, val progress: Float, val detail: String)

@Composable
private fun homeMetrics(data: HomeData): List<MetricItem> {
    val summary = data.summary
    val unknown = stringResource(R.string.stress_index_unknown)
    val lastTest = summary?.lastAssessment
    val averageIndex = if (data.stats.triggersCount > 0) StressScale.fromTriggerAverage(data.stats.average) else null
    return listOf(
        MetricItem(
            stringResource(R.string.home_metric_current),
            summary?.let { stringResource(R.string.stress_index_value, it.current.index) } ?: unknown,
            (summary?.current?.index ?: 0) / StressScale.MAX.toFloat(),
            summary?.let { stringResource(R.string.stress_level_named, stringResource(UiMappings.stressLevelLabel(it.level))) }
                ?: stringResource(R.string.home_metric_no_records)
        ),
        MetricItem(
            stringResource(R.string.home_metric_average),
            averageIndex?.let { stringResource(R.string.stress_index_value, it) } ?: unknown,
            (averageIndex ?: 0) / StressScale.MAX.toFloat(),
            pluralStringResource(R.plurals.home_metric_records, data.stats.triggersCount, data.stats.triggersCount)
        ),
        MetricItem(
            stringResource(R.string.home_metric_last_test),
            lastTest?.let { stringResource(R.string.stress_index_value, it.index) } ?: unknown,
            (lastTest?.index ?: 0) / StressScale.MAX.toFloat(),
            lastTest?.let { stringResource(R.string.stress_detail_pss, it.rawValue) } ?: stringResource(R.string.home_metric_no_test)
        ),
        MetricItem(stringResource(R.string.home_metric_peak), data.stats.peakHours.substringBefore(" "), 0f, stringResource(R.string.home_metric_peak_detail)),
        MetricItem(
            stringResource(R.string.home_metric_hours),
            stringResource(R.string.home_metric_hours_value, "%.1f".format(data.weeklyHours)),
            (data.weeklyHours / RecommendationEngine.WEEKLY_HOURS_LIMIT).toFloat(),
            stringResource(R.string.home_metric_hours_detail)
        )
    )
}

@Composable
private fun HomeContent(data: HomeData, onAction: (RecommendationAction) -> Unit, onNavigate: (String) -> Unit, onRefresh: () -> Unit) {
    val headerImage = remember { HEADER_IMAGES.random() }
    val metrics = homeMetrics(data)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Greeting (compact so the summary is visible without scrolling on 360x640 dp)
        item { HeroHeader(data.firstName, headerImage, onRefresh) }

        // 2. Stress summary right below the greeting (A1). Without data: welcome state, never sample data.
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val summary = data.summary
                if (summary != null) {
                    StressSummaryCard(summary, onTest = { onNavigate("assessment") }, onRecord = { onNavigate("triggers") })
                    Text(
                        stringResource(R.string.stress_scale_disclaimer),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    WelcomeStartCard(onTest = { onNavigate("assessment") }, onRecord = { onNavigate("triggers") })
                }
            }
        }

        // 3. Next appointment
        data.nextAppointment?.let { next ->
            item { NextAppointmentSummary(next, data.nextAppointmentProfessional, Modifier.padding(horizontal = 20.dp)) }
        }

        // 4. Mood selector -> quick access to the right activity
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(stringResource(R.string.home_mood_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MoodItem(stringResource(R.string.home_mood_calm), "🌿", MaterialTheme.colorScheme.primaryContainer) { onNavigate("breathing") }
                    MoodItem(stringResource(R.string.home_mood_focus), "🎯", MaterialTheme.colorScheme.secondaryContainer) { onNavigate("breaks") }
                    MoodItem(stringResource(R.string.home_mood_sleep), "🌙", MaterialTheme.colorScheme.tertiaryContainer) { onNavigate("symptoms") }
                    MoodItem(stringResource(R.string.home_mood_stress), "⚡", MaterialTheme.colorScheme.errorContainer) { onNavigate("triggers") }
                }
            }
        }

        // 5. "Tu equilibrio" metrics carousel (0-100 scale)
        item {
            Column {
                SectionTitle(stringResource(R.string.home_balance_title), Modifier.padding(horizontal = 20.dp).semantics { heading() })
                Spacer(Modifier.height(12.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(metrics) { MetricCard(it, Modifier.width(160.dp)) }
                }
            }
        }

        // 6. Daily quote
        item { DailyQuoteCard(Modifier.padding(horizontal = 20.dp)) }

        // 7. Active breaks
        item { BreaksCard(data, onNavigate) }

        // 8. Recommended for you
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(stringResource(R.string.home_recommended_title), Modifier.semantics { heading() })
                Row {
                    TextButton(onClick = { onNavigate("assessment") }) { Text(stringResource(R.string.home_recommended_test)) }
                    TextButton(onClick = { onNavigate("triggers") }) { Text(stringResource(R.string.home_recommended_checkin)) }
                }
            }
        }
        items(data.recommendations, key = { it.title }) { rec ->
            Box(Modifier.padding(horizontal = 20.dp)) { RecommendationCard(rec) { onAction(rec.action) } }
        }
        if (data.stats.triggersCount > 0) {
            item {
                Text(
                    stringResource(R.string.home_top_category, Catalogs.categoryLabel(data.stats.topCategory)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroHeader(firstName: String, imageUrl: String, onRefresh: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).background(MaterialTheme.colorScheme.primary)) {
        AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.7f)))
            )
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.Bottom) {
            Text(
                if (firstName.isBlank()) stringResource(R.string.home_greeting_anonymous) else stringResource(R.string.home_greeting, firstName),
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
        }
        FilledIconButton(
            onClick = onRefresh,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.35f))
        ) { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.home_refresh), tint = Color.White) }
    }
}

/** Current 0-100 index, its source, a sparkline of the real evolution and quick actions. */
@Composable
private fun StressSummaryCard(summary: StressSummary, onTest: () -> Unit, onRecord: () -> Unit) {
    val levelLabel = stringResource(UiMappings.stressLevelLabel(summary.level))
    val levelColor = stressColor(summary.level)
    val sourceDate = DateTimeUtils.formatDateTime(summary.current.at)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_summary_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
            val a11y = stringResource(R.string.home_summary_a11y, summary.current.index, levelLabel)
            Row(
                Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = a11y },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.stress_index_value, summary.current.index),
                    style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface
                )
                Pill(levelLabel, levelColor)
            }
            LinearProgressIndicator(
                progress = { summary.current.index / StressScale.MAX.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = levelColor,
                trackColor = levelColor.copy(alpha = 0.15f)
            )
            Text(
                if (summary.current.source == StressSource.ASSESSMENT) stringResource(R.string.home_summary_source_test, sourceDate)
                else stringResource(R.string.home_summary_source_trigger, sourceDate),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val details = listOfNotNull(
                summary.lastAssessment?.let { stringResource(R.string.stress_detail_pss, it.rawValue) },
                summary.lastTrigger?.let { stringResource(R.string.stress_detail_trigger, it.rawValue) }
            )
            if (details.isNotEmpty()) {
                Text(details.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (summary.evolution.size >= 2) {
                Text(stringResource(R.string.home_summary_evolution), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                EvolutionSparkline(summary.evolution.map { it.index }, Modifier.fillMaxWidth().height(48.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onTest, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(stringResource(R.string.home_summary_new_test)) }
                Button(onClick = onRecord, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(stringResource(R.string.home_summary_new_record)) }
            }
        }
    }
}

/** Line chart of the real 0-100 indexes (oldest to newest). Read by TalkBack as a single summary. */
@Composable
private fun EvolutionSparkline(values: List<Int>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant
    val description = pluralStringResource(R.plurals.home_summary_evolution_a11y, values.size, values.size, values.first(), values.last())
    Canvas(modifier.clearAndSetSemantics { contentDescription = description }) {
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        fun y(v: Int) = size.height - (v.coerceIn(0, StressScale.MAX) / StressScale.MAX.toFloat()) * size.height
        drawLine(track, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        val path = Path().apply {
            values.forEachIndexed { i, v -> if (i == 0) moveTo(0f, y(v)) else lineTo(i * stepX, y(v)) }
        }
        drawPath(path, color, style = Stroke(width = 5f, cap = StrokeCap.Round))
        values.forEachIndexed { i, v -> drawCircle(color, radius = 6f, center = Offset(i * stepX, y(v))) }
    }
}

/** Shown in the summary position while the user has no PSS-10 nor trigger. */
@Composable
private fun WelcomeStartCard(onTest: () -> Unit, onRecord: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.home_welcome_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.home_welcome_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Button(onClick = onTest, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.home_welcome_test)) }
            OutlinedButton(onClick = onRecord, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.home_welcome_record)) }
        }
    }
}

@Composable
private fun BreaksCard(data: HomeData, onNavigate: (String) -> Unit) {
    Card(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("🧘", style = MaterialTheme.typography.titleMedium, modifier = Modifier.clearAndSetSemantics { }) }
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_breaks_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    data.nextBreak?.let { stringResource(R.string.home_breaks_next, DateTimeUtils.formatDateTime(it.toInstant())) }
                        ?: stringResource(R.string.home_breaks_none),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { onNavigate("breaks") }) {
                Text(stringResource(if (data.nextBreak == null) R.string.home_breaks_schedule else R.string.home_breaks_adjust))
            }
        }
    }
}

@Composable
private fun DailyQuoteCard(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("✨", style = MaterialTheme.typography.titleMedium, modifier = Modifier.clearAndSetSemantics { }) }
            }
            Text(
                stringResource(R.string.home_quote),
                style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NextAppointmentSummary(appointment: Appointment, professional: String?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_next_appointment), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.home_next_appointment_with, professional ?: stringResource(R.string.home_next_appointment_default_professional)),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )
                Text(DateTimeUtils.formatDateTime(appointment.dateTime), style = MaterialTheme.typography.bodySmall)
            }
            StatusPill(appointment.status)
        }
    }
}

@Composable
private fun MoodItem(name: String, icon: String, containerColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClickLabel = name, onClick = onClick).padding(4.dp)
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(containerColor.copy(alpha = 0.5f))
                .border(2.dp, containerColor, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) { Text(icon, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.clearAndSetSemantics { }) }
        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetricCard(metric: MetricItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) { },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(metric.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(metric.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { metric.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            Text(metric.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

/** Same layout as the original session card ("ProductCard"). */
@Composable
private fun RecommendationCard(rec: Recommendation, onClick: () -> Unit) {
    val (emoji, tag) = when (rec.action) {
        RecommendationAction.BREATHING -> "🌬️" to R.string.home_rec_breathing
        RecommendationAction.BREAKS -> "🧘" to R.string.home_rec_breaks
        RecommendationAction.LIBRARY -> "📚" to R.string.home_rec_library
        RecommendationAction.PROFESSIONALS -> "🩺" to R.string.home_rec_professionals
        RecommendationAction.ASSESSMENT -> "📝" to R.string.home_rec_assessment
        RecommendationAction.WORKLOAD -> "⏱️" to R.string.home_rec_workload
        RecommendationAction.TRIGGERS -> "📍" to R.string.home_rec_triggers
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics { }) }
                }
                Text(stringResource(tag), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(rec.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(rec.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onClick) { Text(stringResource(R.string.home_rec_detail)) }
        }
    }
}
