package org.hermesmobile.client.runtime

enum class HermesSendSource(val label: String) {
    Chat("Chat"),
    Workbench("Workbench")
}

enum class HermesSendStage(val label: String, val diagnosticName: String) {
    Preparing("Preparing", "prepare"),
    Gateway("Connecting gateway", "ensureGateway"),
    RuntimeSession("Opening session", "session.resume"),
    Submit("Submitting prompt", "prompt.submit")
}

data class PendingHermesSend(
    val promptText: String,
    val visibleText: String,
    val source: HermesSendSource,
    val cwd: String? = null,
    val profile: String? = null,
    val stage: HermesSendStage = HermesSendStage.Preparing,
    val attempts: Int = 1,
    val lastError: String? = null
) {
    val preview: String
        get() = visibleText.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().take(160)
}

fun safeSendDiagnostic(message: String, promptText: String, visibleText: String = ""): String {
    val redacted = listOf(promptText, visibleText)
        .filter { it.length >= 8 }
        .fold(message) { current, secret -> current.replace(secret, "[prompt]") }
        .replace(Regex("\\s+"), " ")
        .trim()
    return redacted.ifBlank { "Unknown send failure" }.take(220)
}
