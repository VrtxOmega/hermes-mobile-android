package org.hermesmobile.client.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HermesSendSafetyTest {
    @Test
    fun diagnosticRedactsPromptText() {
        val prompt = "build a private Android browser test app"
        val visible = "Android app: build a private Android browser test app"
        val result = safeSendDiagnostic(
            message = "prompt.submit failed after sending build a private Android browser test app to Hermes",
            promptText = prompt,
            visibleText = visible
        )

        assertFalse(result.contains(prompt))
        assertEquals("prompt.submit failed after sending [prompt] to Hermes", result)
    }

    @Test
    fun pendingSendPreviewUsesVisibleFirstLine() {
        val pending = PendingHermesSend(
            promptText = "Hermes Mobile Workbench request\n\nfull hidden operational prompt",
            visibleText = "Android app: make the UI breathe\nsecond line",
            source = HermesSendSource.Workbench,
            stage = HermesSendStage.Submit,
            attempts = 2,
            lastError = "Gateway send failed"
        )

        assertEquals("Android app: make the UI breathe", pending.preview)
        assertEquals("prompt.submit", pending.stage.diagnosticName)
    }
}
