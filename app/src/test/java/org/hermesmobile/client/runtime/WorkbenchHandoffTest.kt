package org.hermesmobile.client.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkbenchHandoffTest {
    @Test
    fun previewHandoffCanOpenOnMobile() {
        val item = WorkbenchEvidenceItem(
            kind = WorkbenchEvidenceKind.Preview,
            value = "http://100.64.0.1:9119/",
            source = WorkbenchEvidenceSource.DebugFixture,
            sourceDetail = "deterministic fixture"
        )

        val handoff = item.toWorkbenchHandoff()

        assertEquals("Browser QA handoff", handoff.title)
        assertEquals(WorkbenchHandoffRisk.MobileSafe, handoff.risk)
        assertTrue(handoff.prompt.contains("http://100.64.0.1:9119/"))
        assertTrue(handoff.prompt.contains("Debug / deterministic fixture"))
    }

    @Test
    fun fileHandoffKeepsRepoInspectionOnDesktopHost() {
        val item = WorkbenchEvidenceItem(
            kind = WorkbenchEvidenceKind.File,
            value = "/home/user/projects/hermes-mobile-android/README.md"
        )

        val handoff = item.toWorkbenchHandoff()

        assertEquals("File review handoff", handoff.title)
        assertEquals(WorkbenchHandoffRisk.DesktopOnly, handoff.risk)
        assertTrue(handoff.nextAction.contains("desktop host"))
        assertTrue(handoff.prompt.contains("Do not reveal secrets"))
    }

    @Test
    fun safeCommandHandoffIsDesktopOnly() {
        val item = WorkbenchEvidenceItem(
            kind = WorkbenchEvidenceKind.Command,
            value = "./gradlew testDebugUnitTest assembleDebug"
        )

        val handoff = item.toWorkbenchHandoff()

        assertEquals("Build/test handoff", handoff.title)
        assertEquals(WorkbenchHandoffRisk.DesktopOnly, handoff.risk)
        assertTrue(handoff.prompt.contains("rerun or diagnose it from desktop host only"))
    }

    @Test
    fun dangerousCommandHandoffRequiresConfirmation() {
        val item = WorkbenchEvidenceItem(
            kind = WorkbenchEvidenceKind.Command,
            value = "sudo systemctl restart hermes-gateway.service"
        )

        val handoff = item.toWorkbenchHandoff()

        assertEquals("Command confirmation handoff", handoff.title)
        assertEquals(WorkbenchHandoffRisk.NeedsConfirmation, handoff.risk)
        assertTrue(handoff.nextAction.contains("explicit confirmation"))
        assertTrue(handoff.prompt.contains("Mobile handoff risk: Needs confirmation"))
    }
}
