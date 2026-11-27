package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalRiftPalette

private sealed interface CoachAdviceBlock {
  data class Heading(val text: String) : CoachAdviceBlock

  data class Bullet(val marker: String?, val text: String) : CoachAdviceBlock

  data class Paragraph(val text: String) : CoachAdviceBlock
}

@Composable
fun CoachFormattedAdviceBody(
  rawText: String,
  accentColor: Color,
  compact: Boolean,
  modifier: Modifier = Modifier,
) {
  val palette = LocalRiftPalette.current
  val baseBodyStyle =
    if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
  val scaledBodyStyle = baseBodyStyle.copy(lineHeight = baseBodyStyle.fontSize * 1.46f)
  val blocks = remember(rawText) { parseCoachAdviceBlocks(rawText) }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp),
  ) {
    blocks.forEach { block ->
      when (block) {
        is CoachAdviceBlock.Heading -> {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = accentColor.copy(alpha = 0.14f),
            modifier = Modifier.padding(top = 2.dp),
          ) {
            Text(
              text = highlightKeywords(block.text, accentColor),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = accentColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
        is CoachAdviceBlock.Bullet -> {
          Row(
            modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (block.marker != null) {
              Box(
                modifier =
                  Modifier.padding(top = 3.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = block.marker,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = accentColor,
                )
              }
            } else {
              Box(
                modifier =
                  Modifier.padding(top = 8.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
              )
            }
            Text(
              text = highlightKeywords(block.text, palette.primaryBright),
              style = scaledBodyStyle,
              color = palette.textPrimary,
              modifier = Modifier.weight(1f),
            )
          }
        }
        is CoachAdviceBlock.Paragraph -> {
          Text(
            text = highlightKeywords(block.text, palette.primaryBright),
            style = scaledBodyStyle,
            color = palette.textPrimary,
          )
        }
      }
    }
  }
}

private fun parseCoachAdviceBlocks(raw: String): List<CoachAdviceBlock> {
  val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
  if (lines.isEmpty()) return listOf(CoachAdviceBlock.Paragraph(raw))

  val numberedRegex = Regex("""^(\d{1,2})[.)]\s+(.+)$""")
  val bulletRegex = Regex("""^(?:[-*•▪▸]+)\s+(.+)$""")
  val headingColonRegex = Regex("""^([^:]{3,36}):\s*(.+)?$""")

  return buildList {
    lines.forEach { line ->
      val cleanMarkdownHeading = line.trimStart('#').trim()
      when {
        line.startsWith("#") && cleanMarkdownHeading.isNotEmpty() -> {
          add(CoachAdviceBlock.Heading(cleanMarkdownHeading.replace("**", "")))
        }
        numberedRegex.matches(line) -> {
          val m = numberedRegex.find(line)!!
          add(CoachAdviceBlock.Bullet(marker = m.groupValues[1], text = m.groupValues[2]))
        }
        bulletRegex.matches(line) -> {
          val m = bulletRegex.find(line)!!
          add(CoachAdviceBlock.Bullet(marker = null, text = m.groupValues[1]))
        }
        line.startsWith("**") && line.endsWith("**") && line.length in 6..52 -> {
          add(CoachAdviceBlock.Heading(line.removeSurrounding("**").trim(':')))
        }
        headingColonRegex.matches(line) && !line.contains("http") -> {
          val m = headingColonRegex.find(line)!!
          val head = m.groupValues[1].replace("**", "").trim()
          val rest = m.groupValues[2].trim()
          if (head.length <= 28 && !head.contains(" ")) {
            add(CoachAdviceBlock.Heading(head))
            if (rest.isNotEmpty()) add(CoachAdviceBlock.Paragraph(rest))
          } else if (rest.isEmpty() && head.length <= 36) {
            add(CoachAdviceBlock.Heading(head))
          } else {
            add(CoachAdviceBlock.Paragraph(line))
          }
        }
        else -> add(CoachAdviceBlock.Paragraph(line))
      }
    }
  }
}

private fun highlightKeywords(text: String, highlightColor: Color): AnnotatedString =
  buildAnnotatedString {
    val boldRegex = Regex("""\*\*(.+?)\*\*""")
    var lastIdx = 0
    for (match in boldRegex.findAll(text)) {
      if (match.range.first > lastIdx) {
        append(text.substring(lastIdx, match.range.first))
      }
      withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor)) {
        append(match.groupValues[1])
      }
      lastIdx = match.range.last + 1
    }
    if (lastIdx < text.length) {
      append(text.substring(lastIdx))
    }
  }
