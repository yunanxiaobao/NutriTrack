package com.songshawn.nutritrack.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.delay

/**
 * Markdown-capable text component that renders markdown formatting
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    // Parse markdown text to AnnotatedString
    val annotatedString = buildAnnotatedString {
        // Handle bold text
        val boldRegex = """\*\*(.*?)\*\*""".toRegex()
        var lastIndex = 0

        val matches = boldRegex.findAll(markdown)
        if (matches.count() > 0) {
            for (match in matches) {
                // Add normal text
                append(markdown.substring(lastIndex, match.range.first))

                // Add bold text
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[1]) // Get text between **
                }

                lastIndex = match.range.last + 1
            }
            // Add remaining text
            if (lastIndex < markdown.length) {
                append(markdown.substring(lastIndex))
            }
        } else {
            // If no matches, add original text
            append(markdown)
        }
    }

    // Create text style
    val style = textAlign?.let {
        TextStyle(
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = it,
            lineHeight = lineHeight
        )
    }

    // Display formatted text
    if (style != null) {
        Text(
            text = annotatedString,
            modifier = modifier,
            style = style,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines
        )
    } else {
        Text(
            text = annotatedString,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines
        )
    }
}

/**
 * Typewriter effect for Markdown text that gradually reveals characters
 */
@Composable
fun TypewriterMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    typingDelay: Long = 10 // Delay in ms per character
) {
    var visibleText by remember { mutableStateOf("") }
    var currentIndex by remember { mutableIntStateOf(0) }

    // Reset effect when text changes
    LaunchedEffect(text) {
        visibleText = ""
        currentIndex = 0
    }

    // Typewriter effect
    LaunchedEffect(currentIndex, text) {
        if (currentIndex < text.length) {
            delay(typingDelay)
            visibleText = text.substring(0, currentIndex + 1)
            currentIndex++
        }
    }

    // Use custom Markdown renderer to display text
    MarkdownText(
        markdown = visibleText,
        modifier = modifier,
        color = textStyle.color,
        fontSize = textStyle.fontSize,
        fontFamily = textStyle.fontFamily,
        fontWeight = textStyle.fontWeight,
        fontStyle = textStyle.fontStyle,
        letterSpacing = textStyle.letterSpacing,
        textDecoration = textStyle.textDecoration,
        textAlign = textStyle.textAlign,
        lineHeight = textStyle.lineHeight
    )
}