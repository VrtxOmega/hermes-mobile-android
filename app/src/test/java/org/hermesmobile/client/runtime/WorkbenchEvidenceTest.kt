package org.hermesmobile.client.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkbenchEvidenceTest {
    @Test
    fun extractsPreviewUrlsPathsAndCommands() {
        val text = """
            Done.
            [Preview: mobile](#preview/http%3A%2F%2F100.64.0.1%3A3000%2Fapp)
            Preview is also at http://100.64.0.1:5173/dashboard.
            Changed /home/user/projects/demo/app/src/main/java/MainActivity.kt:42
            Updated app/build.gradle.kts and README.md.

            Commands run:
            $ ./gradlew testDebugUnitTest assembleDebug
            adb install -r app/build/outputs/apk/debug/app-debug.apk
        """.trimIndent()

        val evidence = extractWorkbenchEvidence(text)

        assertEquals(
            listOf("http://100.64.0.1:3000/app", "http://100.64.0.1:5173/dashboard"),
            evidence.previews
        )
        assertEquals(
            listOf(
                "/home/user/projects/demo/app/src/main/java/MainActivity.kt",
                "app/build.gradle.kts",
                "README.md",
                "app/build/outputs/apk/debug/app-debug.apk"
            ),
            evidence.filePaths
        )
        assertEquals(
            listOf(
                "./gradlew testDebugUnitTest assembleDebug",
                "adb install -r app/build/outputs/apk/debug/app-debug.apk"
            ),
            evidence.commands
        )
    }

    @Test
    fun deduplicatesEvidence() {
        val text = """
            /home/user/projects/demo/README.md
            /home/user/projects/demo/README.md
            node --test test/watchlist.test.mjs
            node --test test/watchlist.test.mjs
        """.trimIndent()

        val evidence = extractWorkbenchEvidence(text)

        assertEquals(listOf("/home/user/projects/demo/README.md", "test/watchlist.test.mjs"), evidence.filePaths)
        assertEquals(listOf("node --test test/watchlist.test.mjs"), evidence.commands)
    }

    @Test
    fun exposesTypedItemsInMobileReviewOrder() {
        val evidence = WorkbenchEvidence(
            previews = listOf("http://100.64.0.1:3000"),
            filePaths = listOf("/home/user/projects/demo/app/src/main/java/MainActivity.kt"),
            commands = listOf("./gradlew assembleDebug")
        )

        assertEquals(
            listOf("Preview", "File", "Run"),
            evidence.items.map { it.label }
        )
        assertEquals(
            listOf(
                "http://100.64.0.1:3000",
                "/home/user/projects/demo/app/src/main/java/MainActivity.kt",
                "./gradlew assembleDebug"
            ),
            evidence.items.map { it.value }
        )
    }

    @Test
    fun preservesSourceContextFromGatewayChunks() {
        val evidence = extractWorkbenchEvidence(
            listOf(
                WorkbenchEvidenceChunk(
                    source = WorkbenchEvidenceSource.Chat,
                    sourceDetail = "assistant",
                    text = "Preview: http://100.64.0.1:3000/app"
                ),
                WorkbenchEvidenceChunk(
                    source = WorkbenchEvidenceSource.GatewayEvent,
                    sourceDetail = "tool.output",
                    sessionId = "abcdef123456",
                    text = """
                        Changed /home/user/projects/demo/app/src/main/java/MainActivity.kt
                        ./gradlew testDebugUnitTest assembleDebug
                    """.trimIndent()
                )
            )
        )

        assertEquals(
            listOf("Chat / assistant", "Gateway / tool.output / abcdef12", "Gateway / tool.output / abcdef12"),
            evidence.items.map { it.sourceDescription }
        )
        assertEquals(
            listOf(
                "http://100.64.0.1:3000/app",
                "/home/user/projects/demo/app/src/main/java/MainActivity.kt",
                "./gradlew testDebugUnitTest assembleDebug"
            ),
            evidence.items.map { it.value }
        )
    }

    @Test
    fun keepsFirstSourceWhenDuplicateEvidenceStreamsIn() {
        val evidence = extractWorkbenchEvidence(
            listOf(
                WorkbenchEvidenceChunk(
                    source = WorkbenchEvidenceSource.GatewayEvent,
                    sourceDetail = "message.delta",
                    sessionId = "first-session",
                    text = "adb install -r app/build/outputs/apk/debug/app-debug.apk"
                ),
                WorkbenchEvidenceChunk(
                    source = WorkbenchEvidenceSource.DebugFixture,
                    sourceDetail = "deterministic fixture",
                    text = "adb install -r app/build/outputs/apk/debug/app-debug.apk"
                )
            )
        )

        val commandItem = evidence.items.first { it.kind == WorkbenchEvidenceKind.Command }

        assertEquals(WorkbenchEvidenceSource.GatewayEvent, commandItem.source)
        assertEquals("Gateway / message.delta / first-se", commandItem.sourceDescription)
    }
}
