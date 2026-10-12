package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary

@Composable
fun MarkdownViewer(
    markdownText: String,
    modifier: Modifier = Modifier,
    onCheckboxToggle: ((lineIndex: Int, isChecked: Boolean) -> Unit)? = null
) {
    val lines = markdownText.lines()
    var inCodeBlock = false
    val codeBlockBuilder = StringBuilder()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (markdownText.isBlank()) {
            Text(
                text = "Nota vacía. Toca el modo de edición para comenzar a escribir.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontStyle = FontStyle.Italic
            )
            return@Column
        }

        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()

            // Code Block boundaries (```)
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // End code block
                    CodeBlock(code = codeBlockBuilder.toString().trimEnd())
                    codeBlockBuilder.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                return@forEachIndexed
            }

            if (inCodeBlock) {
                codeBlockBuilder.append(line).append("\n")
                return@forEachIndexed
            }

            // Horizontal Rule
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
                return@forEachIndexed
            }

            // Headings
            when {
                trimmed.startsWith("#### ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("#### ")),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("### ")),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("## ")),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                    )
                }
                // Checklists
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                    val taskText = trimmed.removePrefix("- [ ] ").removePrefix("* [ ] ")
                    ChecklistItem(
                        checked = false,
                        text = taskText,
                        onToggle = { onCheckboxToggle?.invoke(index, true) }
                    )
                }
                trimmed.startsWith("- [x] ") || trimmed.startsWith("* [x] ") ||
                        trimmed.startsWith("- [X] ") || trimmed.startsWith("* [X] ") -> {
                    val taskText = trimmed.substring(6)
                    ChecklistItem(
                        checked = true,
                        text = taskText,
                        onToggle = { onCheckboxToggle?.invoke(index, false) }
                    )
                }
                // Bullet points
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(trimmed.substring(2)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                // Numbered lists
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val dotIdx = trimmed.indexOf('.')
                    val num = trimmed.substring(0, dotIdx + 1)
                    val content = trimmed.substring(dotIdx + 1).trim()
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = num,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(content),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                // Blockquotes
                trimmed.startsWith("> ") -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(28.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = parseInlineMarkdown(trimmed.removePrefix("> ")),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
                // Regular Paragraph
                trimmed.isNotEmpty() -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        if (inCodeBlock && codeBlockBuilder.isNotEmpty()) {
            CodeBlock(code = codeBlockBuilder.toString().trimEnd())
        }
    }
}

@Composable
private fun ChecklistItem(
    checked: Boolean,
    text: String,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = if (checked) "Completado" else "Pendiente",
            tint = if (checked) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = parseInlineMarkdown(text),
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = code,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = EmeraldLight
            )
        )
    }
}

/**
 * Parses inline formatting: **bold**, *italic*, `code`, #tag, ~~strikethrough~~
 */
fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Inline Code: `code`
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x33334155),
                                color = EmeraldLight
                            )
                        )
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Bold: **bold**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else {
                        append("**")
                        i += 2
                    }
                }
                // Strikethrough: ~~strike~~
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else {
                        append("~~")
                        i += 2
                    }
                }
                // Italic: *italic*
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1 && end > i + 1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Markdown Tag: #tag
                text[i] == '#' && (i == 0 || text[i - 1].isWhitespace()) -> {
                    val end = text.indexOfAny(charArrayOf(' ', '\n', '\t', ',', '.'), i + 1).takeIf { it != -1 } ?: text.length
                    val tagWord = text.substring(i, end)
                    if (tagWord.length > 1 && !tagWord.startsWith("##")) {
                        pushStyle(
                            SpanStyle(
                                color = CyanAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        append(tagWord)
                        pop()
                        i = end
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
