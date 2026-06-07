package org.hermesmobile.client.runtime

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object HermesUrl {
    const val DefaultBaseUrl = "http://100.64.0.1:9119"

    fun normalizeBaseUrl(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        require(trimmed.isNotBlank()) { "Backend URL is required" }

        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }

        val uri = URI(withScheme)
        require(uri.scheme == "http" || uri.scheme == "https") { "Use http or https" }
        require(!uri.host.isNullOrBlank()) { "Backend URL needs a host" }

        val port = if (uri.port >= 0) ":${uri.port}" else ""
        val path = uri.rawPath?.takeIf { it.isNotBlank() && it != "/" }?.trimEnd('/').orEmpty()
        return "${uri.scheme}://${uri.host}$port$path"
    }

    fun websocketUrl(baseUrl: String, path: String = "/api/ws", ticket: String): String {
        val normalized = normalizeBaseUrl(baseUrl)
        val wsBase = when {
            normalized.startsWith("https://") -> "wss://" + normalized.removePrefix("https://")
            normalized.startsWith("http://") -> "ws://" + normalized.removePrefix("http://")
            else -> error("Unsupported backend scheme")
        }
        val encodedTicket = URLEncoder.encode(ticket, StandardCharsets.UTF_8.name())
        return "$wsBase$path?ticket=$encodedTicket"
    }
}
