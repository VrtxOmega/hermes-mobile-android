package org.hermesmobile.client.runtime

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import org.hermesmobile.client.model.GatewayEvent

enum class HermesInteractionKind(val label: String) {
    Approval("Approval"),
    Clarify("Clarify"),
    Sudo("Sudo"),
    Secret("Secret")
}

data class PendingHermesInteraction(
    val kind: HermesInteractionKind,
    val sessionId: String?,
    val requestId: String? = null,
    val title: String,
    val body: String,
    val command: String = "",
    val choices: List<String> = emptyList(),
    val envVar: String = ""
) {
    val stableKey: String
        get() = listOf(kind.name, sessionId.orEmpty(), requestId.orEmpty(), envVar, command).joinToString(":")
}

fun parsePendingInteraction(event: GatewayEvent): PendingHermesInteraction? {
    val payload = event.payload?.jsonObjectOrNull() ?: return null
    return when (event.type) {
        "approval.request" -> {
            val description = payload.stringValue("description").ifBlank { "Hermes needs permission to run a command." }
            PendingHermesInteraction(
                kind = HermesInteractionKind.Approval,
                sessionId = event.sessionId,
                title = "Command approval",
                body = description,
                command = payload.stringValue("command")
            )
        }
        "clarify.request" -> {
            val requestId = payload.stringValue("request_id")
            val question = payload.stringValue("question")
            if (requestId.isBlank() || question.isBlank()) return null
            PendingHermesInteraction(
                kind = HermesInteractionKind.Clarify,
                sessionId = event.sessionId,
                requestId = requestId,
                title = "Clarify",
                body = question,
                choices = payload.stringList("choices")
            )
        }
        "sudo.request" -> {
            val requestId = payload.stringValue("request_id")
            if (requestId.isBlank()) return null
            PendingHermesInteraction(
                kind = HermesInteractionKind.Sudo,
                sessionId = event.sessionId,
                requestId = requestId,
                title = "Sudo requested",
                body = "Hermes requested elevated access on desktop host. Cancel from Android unless you are intentionally completing this trusted flow."
            )
        }
        "secret.request" -> {
            val requestId = payload.stringValue("request_id")
            if (requestId.isBlank()) return null
            val envVar = payload.stringValue("env_var")
            PendingHermesInteraction(
                kind = HermesInteractionKind.Secret,
                sessionId = event.sessionId,
                requestId = requestId,
                title = envVar.ifBlank { "Secret requested" },
                body = payload.stringValue("prompt").ifBlank { "Hermes requested a masked secret value." },
                envVar = envVar
            )
        }
        else -> null
    }
}

fun List<PendingHermesInteraction>.upsertPendingInteraction(next: PendingHermesInteraction): List<PendingHermesInteraction> {
    return (filterNot { existing ->
        existing.kind == next.kind && existing.sessionId == next.sessionId
    } + next).takeLast(12)
}

fun List<PendingHermesInteraction>.clearPendingInteraction(done: PendingHermesInteraction): List<PendingHermesInteraction> {
    return filterNot { existing ->
        existing.kind == done.kind &&
            existing.sessionId == done.sessionId &&
            (done.requestId == null || existing.requestId == done.requestId)
    }
}

fun List<PendingHermesInteraction>.clearSessionInteractions(sessionId: String?): List<PendingHermesInteraction> {
    return filterNot { it.sessionId == sessionId }
}

private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject

private fun JsonObject.stringValue(key: String): String {
    return (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
}

private fun JsonObject.stringList(key: String): List<String> {
    val element = this[key] ?: return emptyList()
    if (element !is JsonArray) return emptyList()
    return element.jsonArray.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
}
