package com.example.neurozen_front.domain.logic

/** Block of a reading or exercise (plain text from the backend; it is never interpreted as HTML). */
sealed interface ReadingBlock {
    data class Heading(val text: String) : ReadingBlock
    data class Paragraph(val text: String) : ReadingBlock
    data class Bullets(val ordered: Boolean, val items: List<String>) : ReadingBlock
}

/**
 * Turns the `content` of readings and exercises into blocks (same rules as the web):
 *  - blocks are separated by blank lines;
 *  - a single short line without a final period is a subtitle;
 *  - lines starting with "- " or "1. " form lists (bulleted or numbered);
 *  - the rest are paragraphs (their line breaks are kept).
 */
object ReadingContent {
    private const val HEADING_MAX = 80
    private val BULLET_PREFIX = Regex("""^[-•*]\s""")
    private val NUMBER_PREFIX = Regex("""^[0-9]{1,3}[.)]\s""")
    private val SENTENCE_END = setOf('.', ',', ';', ':', '!', '?', '…', ')', '"', '»')

    private data class Item(val ordered: Boolean, val text: String)

    private fun listItem(line: String): Item? {
        if (BULLET_PREFIX.containsMatchIn(line)) return Item(false, line.drop(2).trim())
        val number = NUMBER_PREFIX.find(line) ?: return null
        return Item(true, line.drop(number.value.length).trim())
    }

    private fun isHeading(lines: List<String>): Boolean =
        lines.size == 1 && lines[0].length <= HEADING_MAX && lines[0].last() !in SENTENCE_END && listItem(lines[0]) == null

    private fun splitChunk(lines: List<String>): List<ReadingBlock> {
        val blocks = mutableListOf<ReadingBlock>()
        val paragraph = mutableListOf<String>()
        fun flush() {
            if (paragraph.isNotEmpty()) blocks += ReadingBlock.Paragraph(paragraph.joinToString("\n"))
            paragraph.clear()
        }
        for (line in lines) {
            val item = listItem(line)
            if (item == null) { paragraph += line; continue }
            flush()
            val last = blocks.lastOrNull()
            if (last is ReadingBlock.Bullets && last.ordered == item.ordered) blocks[blocks.lastIndex] = last.copy(items = last.items + item.text)
            else blocks += ReadingBlock.Bullets(item.ordered, listOf(item.text))
        }
        flush()
        return blocks
    }

    fun parse(text: String?): List<ReadingBlock> {
        val normalized = text.orEmpty().replace("\r\n", "\n").replace('\r', '\n').trim()
        if (normalized.isEmpty()) return emptyList()
        return normalized.split(Regex("""\n[ \t]*\n""")).flatMap { chunk ->
            val lines = chunk.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            when {
                lines.isEmpty() -> emptyList()
                isHeading(lines) -> listOf(ReadingBlock.Heading(lines[0]))
                else -> splitChunk(lines)
            }
        }
    }
}
