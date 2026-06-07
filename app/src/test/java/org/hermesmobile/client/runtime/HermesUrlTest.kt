package org.hermesmobile.client.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class HermesUrlTest {
    @Test
    fun normalizesTailnetHostWithoutScheme() {
        assertEquals("http://100.64.0.1:9119", HermesUrl.normalizeBaseUrl("100.64.0.1:9119/"))
    }

    @Test
    fun preservesHttpsAndPath() {
        assertEquals("https://hermes-host.tailnet:9119/hermes", HermesUrl.normalizeBaseUrl("https://hermes-host.tailnet:9119/hermes/"))
    }

    @Test
    fun buildsTicketedWebSocketUrl() {
        assertEquals(
            "ws://100.64.0.1:9119/api/ws?ticket=abc123",
            HermesUrl.websocketUrl("http://100.64.0.1:9119", ticket = "abc123")
        )
    }
}
