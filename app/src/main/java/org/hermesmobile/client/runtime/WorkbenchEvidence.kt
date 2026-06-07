package org.hermesmobile.client.runtime

data class WorkbenchEvidence(
    val previews: List<String>,
    val filePaths: List<String>,
    val commands: List<String>,
    val items: List<WorkbenchEvidenceItem> = defaultEvidenceItems(previews, filePaths, commands)
) {
    val isEmpty: Boolean
        get() = previews.isEmpty() && filePaths.isEmpty() && commands.isEmpty()

    companion object {
        fun fromItems(items: List<WorkbenchEvidenceItem>): WorkbenchEvidence {
            val normalized = items.distinctBy { "${it.kind.name}:${it.value}" }
            val previews = normalized
                .filter { it.kind == WorkbenchEvidenceKind.Preview }
                .map { it.value }
                .take(8)
            val filePaths = normalized
                .filter { it.kind == WorkbenchEvidenceKind.File }
                .map { it.value }
                .take(12)
            val commands = normalized
                .filter { it.kind == WorkbenchEvidenceKind.Command }
                .map { it.value }
                .take(12)
            val selectedKeys = (
                previews.map { "${WorkbenchEvidenceKind.Preview.name}:$it" } +
                    filePaths.map { "${WorkbenchEvidenceKind.File.name}:$it" } +
                    commands.map { "${WorkbenchEvidenceKind.Command.name}:$it" }
                ).toSet()
            return WorkbenchEvidence(
                previews = previews,
                filePaths = filePaths,
                commands = commands,
                items = normalized.filter { "${it.kind.name}:${it.value}" in selectedKeys }
            )
        }
    }
}

data class WorkbenchEvidenceItem(
    val kind: WorkbenchEvidenceKind,
    val value: String,
    val source: WorkbenchEvidenceSource = WorkbenchEvidenceSource.Chat,
    val sourceDetail: String = "",
    val sessionId: String? = null,
    val eventType: String? = null,
    val at: String? = null
) {
    val label: String
        get() = kind.label

    val sourceDescription: String
        get() = buildList {
            add(source.label)
            val detail = sourceDetail.ifBlank { eventType.orEmpty() }
            if (detail.isNotBlank()) add(detail)
            sessionId?.takeIf { it.isNotBlank() }?.let { add(it.take(8)) }
        }.joinToString(" / ")
}

enum class WorkbenchEvidenceKind(val label: String) {
    Preview("Preview"),
    File("File"),
    Command("Run")
}

enum class WorkbenchEvidenceSource(val label: String) {
    Chat("Chat"),
    GatewayEvent("Gateway"),
    DebugFixture("Debug")
}

data class WorkbenchEvidenceChunk(
    val source: WorkbenchEvidenceSource,
    val text: String,
    val sourceDetail: String = "",
    val sessionId: String? = null,
    val eventType: String? = null,
    val at: String? = null
)

private val UrlRegex = Regex("""https?://[^\s`"')\]}<>]+""")
private val AbsolutePathRegex =
    Regex("""(?<![\w./-])/(?:home|tmp|var|etc|opt|usr|sdcard|mnt)/[^\s`"')\]}<>]+""")
private val RelativePathRegex =
    Regex("""(?<![\w./-])(?:app|src|lib|test|tests|docs|gradle|build|README|settings\.gradle|build\.gradle|gradle\.properties)[A-Za-z0-9._/\-]*\.[A-Za-z0-9][A-Za-z0-9._-]*""")

private val CommandPrefixes = listOf(
    "./gradlew",
    "gradle ",
    "adb ",
    "npm ",
    "pnpm ",
    "yarn ",
    "bun ",
    "cargo ",
    "pytest",
    "python ",
    "python3 ",
    "node ",
    "curl ",
    "systemctl ",
    "journalctl ",
    "git ",
    "rg "
)

fun extractWorkbenchEvidence(text: String): WorkbenchEvidence {
    val previews = linkedSetOf<String>()
    previews += extractPreviewTargets(text)
    UrlRegex.findAll(text).forEach { match ->
        previews += cleanToken(match.value)
    }

    val paths = linkedSetOf<String>()
    AbsolutePathRegex.findAll(text).forEach { match ->
        paths += cleanPathToken(match.value)
    }
    RelativePathRegex.findAll(text).forEach { match ->
        paths += cleanPathToken(match.value)
    }

    val commands = linkedSetOf<String>()
    text.lineSequence()
        .map(::cleanCommandLine)
        .filter { line -> CommandPrefixes.any { prefix -> line.startsWith(prefix) } }
        .forEach { commands += it.take(180) }

    return WorkbenchEvidence(
        previews = previews.filter(String::isNotBlank).take(8),
        filePaths = paths.filter(String::isNotBlank).take(12),
        commands = commands.filter(String::isNotBlank).take(12)
    )
}

fun extractWorkbenchEvidence(chunks: List<WorkbenchEvidenceChunk>): WorkbenchEvidence {
    val items = mutableListOf<WorkbenchEvidenceItem>()
    chunks
        .filter { it.text.isNotBlank() }
        .forEach { chunk ->
            val parsed = extractWorkbenchEvidence(chunk.text)
            parsed.previews.forEach { value ->
                items += chunk.toItem(WorkbenchEvidenceKind.Preview, value)
            }
            parsed.filePaths.forEach { value ->
                items += chunk.toItem(WorkbenchEvidenceKind.File, value)
            }
            parsed.commands.forEach { value ->
                items += chunk.toItem(WorkbenchEvidenceKind.Command, value)
            }
        }
    return WorkbenchEvidence.fromItems(items)
}

private fun defaultEvidenceItems(
    previews: List<String>,
    filePaths: List<String>,
    commands: List<String>
): List<WorkbenchEvidenceItem> {
    return previews.map { WorkbenchEvidenceItem(WorkbenchEvidenceKind.Preview, it) } +
        filePaths.map { WorkbenchEvidenceItem(WorkbenchEvidenceKind.File, it) } +
        commands.map { WorkbenchEvidenceItem(WorkbenchEvidenceKind.Command, it) }
}

private fun WorkbenchEvidenceChunk.toItem(kind: WorkbenchEvidenceKind, value: String): WorkbenchEvidenceItem {
    return WorkbenchEvidenceItem(
        kind = kind,
        value = value,
        source = source,
        sourceDetail = sourceDetail,
        sessionId = sessionId,
        eventType = eventType,
        at = at
    )
}

private fun cleanToken(value: String): String {
    return value.trim().trimEnd('.', ',', ';')
}

private fun cleanPathToken(value: String): String {
    return cleanToken(value)
        .replace(Regex(""":[0-9]+$"""), "")
}

private fun cleanCommandLine(value: String): String {
    return value.trim()
        .removePrefix("$")
        .removePrefix("#")
        .trim()
        .trim('`')
}
