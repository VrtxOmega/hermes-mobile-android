package org.hermesmobile.client.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewTargetsTest {
    @Test
    fun extractsUniquePreviewTargets() {
        val text = """
            [Preview: demo.html](#preview/%2Ftmp%2Fdemo.html)
            body
            [Preview: demo.html](#preview:%2Ftmp%2Fdemo.html)
            [Preview: app](#preview/http%3A%2F%2F100.64.0.1%3A3000)
        """.trimIndent()

        assertEquals(
            listOf("/tmp/demo.html", "http://100.64.0.1:3000"),
            extractPreviewTargets(text)
        )
    }

    @Test
    fun namesUrlsAndPaths() {
        assertEquals("demo.html", previewName("/tmp/demo.html"))
        assertEquals("app", previewName("http://100.64.0.1:3000/app"))
    }
}
