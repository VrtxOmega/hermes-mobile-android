package org.hermesmobile.client.runtime

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.hermesmobile.client.model.GatewayEvent

class HermesInteractionsTest {
    @Test
    fun parsesApprovalRequest() {
        val event = GatewayEvent(
            type = "approval.request",
            sessionId = "s1",
            payload = buildJsonObject {
                put("command", "ls -la")
                put("description", "Run shell command")
            }
        )

        val parsed = parsePendingInteraction(event)

        assertEquals(HermesInteractionKind.Approval, parsed?.kind)
        assertEquals("s1", parsed?.sessionId)
        assertEquals("ls -la", parsed?.command)
        assertEquals("Run shell command", parsed?.body)
    }

    @Test
    fun parsesClarifyChoices() {
        val event = GatewayEvent(
            type = "clarify.request",
            sessionId = "s2",
            payload = buildJsonObject {
                put("request_id", "rid-1")
                put("question", "Which target?")
                putJsonArray("choices") {
                    add(JsonPrimitive("Android"))
                    add(JsonPrimitive("Web"))
                }
            }
        )

        val parsed = parsePendingInteraction(event)

        assertEquals(HermesInteractionKind.Clarify, parsed?.kind)
        assertEquals("rid-1", parsed?.requestId)
        assertEquals(listOf("Android", "Web"), parsed?.choices)
    }

    @Test
    fun ignoresIncompleteClarifyRequest() {
        val event = GatewayEvent(
            type = "clarify.request",
            payload = buildJsonObject {
                put("question", "Missing request id")
            }
        )

        assertNull(parsePendingInteraction(event))
    }

    @Test
    fun upsertsPerSessionPrompt() {
        val first = PendingHermesInteraction(
            kind = HermesInteractionKind.Secret,
            sessionId = "s1",
            requestId = "old",
            title = "A",
            body = "A"
        )
        val next = first.copy(requestId = "new", body = "B")

        val result = listOf(first).upsertPendingInteraction(next)

        assertEquals(1, result.size)
        assertEquals("new", result.single().requestId)
    }
}
