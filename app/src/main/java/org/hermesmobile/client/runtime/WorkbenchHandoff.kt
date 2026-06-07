package org.hermesmobile.client.runtime

data class WorkbenchHandoff(
    val title: String,
    val risk: WorkbenchHandoffRisk,
    val nextAction: String,
    val prompt: String
)

enum class WorkbenchHandoffRisk(val label: String) {
    MobileSafe("Mobile safe"),
    DesktopOnly("Desktop host only"),
    NeedsConfirmation("Needs confirmation")
}

fun WorkbenchEvidenceItem.toWorkbenchHandoff(): WorkbenchHandoff {
    return when (kind) {
        WorkbenchEvidenceKind.Preview -> previewHandoff()
        WorkbenchEvidenceKind.File -> fileHandoff()
        WorkbenchEvidenceKind.Command -> commandHandoff()
    }
}

private fun WorkbenchEvidenceItem.previewHandoff(): WorkbenchHandoff {
    return WorkbenchHandoff(
        title = "Browser QA handoff",
        risk = WorkbenchHandoffRisk.MobileSafe,
        nextAction = "Open on Android or ask desktop host Hermes to verify the workflow.",
        prompt = """
            Review this mobile Workbench preview from Android:
            $value

            Source: $sourceDescription

            Open it through Hermes/browser tooling from desktop host, verify the user-facing workflow, check console/log errors if available, and report any fixes needed with evidence.
        """.trimIndent()
    )
}

private fun WorkbenchEvidenceItem.fileHandoff(): WorkbenchHandoff {
    return WorkbenchHandoff(
        title = "File review handoff",
        risk = WorkbenchHandoffRisk.DesktopOnly,
        nextAction = "Inspect the file on desktop host; Android should not fetch repo contents directly.",
        prompt = """
            Review this file from the current Hermes Mobile Workbench evidence:
            $value

            Source: $sourceDescription

            Inspect the file on desktop host, summarize what changed or what matters, call out risks, and recommend the smallest verified next change. Do not reveal secrets or unrelated file contents.
        """.trimIndent()
    )
}

private fun WorkbenchEvidenceItem.commandHandoff(): WorkbenchHandoff {
    val risky = value.isDangerousCommandEvidence()
    return WorkbenchHandoff(
        title = if (risky) "Command confirmation handoff" else "Build/test handoff",
        risk = if (risky) WorkbenchHandoffRisk.NeedsConfirmation else WorkbenchHandoffRisk.DesktopOnly,
        nextAction = if (risky) {
            "Stop for explicit confirmation before any privileged, destructive, secret, or service-changing action."
        } else {
            "Review or rerun from desktop host only; Android records evidence and drafts the follow-up."
        },
        prompt = """
            Review this command/build evidence from Hermes Mobile:
            $value

            Source: $sourceDescription
            Mobile handoff risk: ${if (risky) WorkbenchHandoffRisk.NeedsConfirmation.label else WorkbenchHandoffRisk.DesktopOnly.label}

            Explain what the command proves, rerun or diagnose it from desktop host only if needed, and return concise next steps with command evidence. If this command would reveal secrets, modify credentials, restart services, delete/import/update state, or require elevated privileges, stop and ask for explicit confirmation first.
        """.trimIndent()
    )
}

private fun String.isDangerousCommandEvidence(): Boolean {
    val lower = lowercase()
    return listOf(
        "sudo ",
        " su ",
        "rm -rf",
        "systemctl restart",
        "systemctl stop",
        "systemctl disable",
        "journalctl --vacuum",
        "passwd",
        "chown ",
        "chmod 777",
        "chmod -r",
        "curl ",
        "| sh",
        "| bash",
        "env",
        "printenv",
        "secret",
        "token",
        "password",
        "credential",
        "delete",
        "import",
        "update"
    ).any { marker -> lower.contains(marker) }
}
