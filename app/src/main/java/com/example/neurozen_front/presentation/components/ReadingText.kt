package com.example.neurozen_front.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neurozen_front.domain.logic.ReadingBlock
import com.example.neurozen_front.domain.logic.ReadingContent

/**
 * Text of readings and exercises with subtitles, lists and paragraphs, in a comfortable reading size.
 * Compose Text always shows plain text: the content is never interpreted as HTML.
 */
@Composable
fun ReadingText(content: String, modifier: Modifier = Modifier) {
    val blocks = remember(content) { ReadingContent.parse(content) }
    val body = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 27.sp)
    Column(modifier.testTag("reading_content"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        blocks.forEach { block ->
            when (block) {
                is ReadingBlock.Heading -> Text(
                    block.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp).semantics { heading() }
                )
                is ReadingBlock.Paragraph -> Text(block.text, style = body, color = MaterialTheme.colorScheme.onSurface)
                is ReadingBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    block.items.forEachIndexed { index, item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                if (block.ordered) "${index + 1}." else "•", style = body, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, modifier = Modifier.widthIn(min = 22.dp)
                            )
                            Text(item, style = body, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
