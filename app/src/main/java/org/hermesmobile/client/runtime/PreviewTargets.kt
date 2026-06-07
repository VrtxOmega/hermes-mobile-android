package org.hermesmobile.client.runtime

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private val PreviewMarkdownRegex =
    Regex("""\[Preview:[^\]]+]\(#preview[:/]([^)]+)\)""", RegexOption.IGNORE_CASE)

fun extractPreviewTargets(text: String): List<String> {
    val seen = linkedSetOf<String>()
    PreviewMarkdownRegex.findAll(text).forEach { match ->
        val encoded = match.groupValues.getOrNull(1).orEmpty()
        val decoded = runCatching {
            URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
        }.getOrNull()
        if (!decoded.isNullOrBlank()) {
            seen += decoded
        }
    }
    return seen.toList()
}

fun previewName(target: String): String {
    return runCatching {
        val uri = java.net.URI(target)
        val path = uri.path?.takeIf { it.isNotBlank() } ?: target
        path.split('/').filter(String::isNotBlank).lastOrNull() ?: target
    }.getOrElse {
        target.split('/').filter(String::isNotBlank).lastOrNull() ?: target
    }
}
