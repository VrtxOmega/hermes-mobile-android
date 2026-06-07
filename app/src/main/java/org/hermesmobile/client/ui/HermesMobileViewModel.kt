package org.hermesmobile.client.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hermesmobile.client.data.HermesApiException
import org.hermesmobile.client.data.HermesAuthStore
import org.hermesmobile.client.data.HermesGatewayWsClient
import org.hermesmobile.client.data.HermesRestClient
import org.hermesmobile.client.data.GatewayConnectionState
import org.hermesmobile.client.model.AuthMeResponse
import org.hermesmobile.client.model.AuthProvider
import org.hermesmobile.client.model.GatewayEvent
import org.hermesmobile.client.model.HermesStatus
import org.hermesmobile.client.model.ModelInfoResponse
import org.hermesmobile.client.model.ModelOptionsResponse
import org.hermesmobile.client.model.ProfileInfo
import org.hermesmobile.client.model.SessionInfo
import org.hermesmobile.client.model.extractText
import org.hermesmobile.client.runtime.HermesUrl
import org.hermesmobile.client.runtime.HermesSendSource
import org.hermesmobile.client.runtime.HermesSendStage
import org.hermesmobile.client.runtime.PendingHermesInteraction
import org.hermesmobile.client.runtime.PendingHermesSend
import org.hermesmobile.client.runtime.RouteSnapshot
import org.hermesmobile.client.runtime.WorkbenchEvidenceItem
import org.hermesmobile.client.runtime.WorkbenchEvidenceKind
import org.hermesmobile.client.runtime.clearPendingInteraction
import org.hermesmobile.client.runtime.clearSessionInteractions
import org.hermesmobile.client.runtime.collectRouteSnapshot
import org.hermesmobile.client.runtime.extractPreviewTargets
import org.hermesmobile.client.runtime.parsePendingInteraction
import org.hermesmobile.client.runtime.safeSendDiagnostic
import org.hermesmobile.client.runtime.toWorkbenchHandoff
import org.hermesmobile.client.runtime.upsertPendingInteraction

class HermesMobileViewModel(application: Application) : AndroidViewModel(application) {
    private val store = HermesAuthStore(application)
    private val restClient = HermesRestClient(store)
    private var gatewayClient: HermesGatewayWsClient? = null
    private val debugBuild = (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private val _state = MutableStateFlow(
        HermesUiState(
            backendUrl = restClient.currentBaseUrl(),
            themeName = store.themeName(),
            workbenchCwd = store.workbenchCwd(),
            routeSnapshot = collectRouteSnapshot(application),
            debugBuild = debugBuild
        )
    )
    val state: StateFlow<HermesUiState> = _state

    init {
        refreshStatus()
    }

    fun setTab(tab: HermesTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun setBackendUrl(value: String) {
        _state.update { it.copy(backendUrl = value) }
    }

    fun setUsername(value: String) {
        _state.update { it.copy(username = value) }
    }

    fun setPassword(value: String) {
        _state.update { it.copy(password = value) }
    }

    fun setPrompt(value: String) {
        _state.update { state ->
            val valueTrimmed = value.trim()
            val clearedPending = state.pendingSend?.takeUnless { pending ->
                pending.source == HermesSendSource.Chat && pending.promptText.trim() != valueTrimmed
            }
            state.copy(
                prompt = value,
                pendingSend = clearedPending,
                error = if (clearedPending == null && state.pendingSend != null) null else state.error
            )
        }
    }

    fun setThemeName(value: String) {
        store.saveThemeName(value)
        _state.update { it.copy(themeName = value, lastAction = "Theme set to $value") }
    }

    fun setWorkbenchMode(value: WorkbenchMode) {
        _state.update { it.copy(workbenchMode = value) }
    }

    fun setWorkbenchGoal(value: String) {
        _state.update { it.copy(workbenchGoal = value) }
    }

    fun setWorkbenchCwd(value: String) {
        store.saveWorkbenchCwd(value)
        _state.update { it.copy(workbenchCwd = value) }
    }

    fun setWorkbenchBrowserUrl(value: String) {
        _state.update { it.copy(workbenchBrowserUrl = value) }
    }

    fun selectProfile(profile: String?) {
        _state.update {
            it.copy(
                selectedProfile = profile?.takeIf { name -> name.isNotBlank() },
                selectedStoredSessionId = null,
                activeRuntimeSessionId = null,
                messages = emptyList(),
                lastAction = "Profile set to ${profile ?: "default"} for new Hermes turns"
            )
        }
    }

    fun selectModelProvider(provider: String) {
        val model = _state.value.modelOptions
            ?.providers
            ?.firstOrNull { it.slug == provider }
            ?.models
            ?.firstOrNull()
            .orEmpty()
        _state.update {
            it.copy(
                selectedModelProvider = provider,
                selectedModel = model.ifBlank { it.selectedModel },
                lastAction = "Provider selected: $provider"
            )
        }
    }

    fun selectModel(model: String) {
        _state.update { it.copy(selectedModel = model) }
    }

    fun openPreviewTarget(target: String) {
        _state.update {
            it.copy(
                selectedTab = HermesTab.Workbench,
                workbenchBrowserUrl = target,
                lastAction = "Preview target loaded"
            )
        }
    }

    fun openWorkbenchEvidence() {
        _state.update {
            it.copy(
                selectedTab = HermesTab.Workbench,
                lastAction = "Workbench evidence opened"
            )
        }
    }

    fun openFirstPreviewTarget() {
        val target = extractPreviewTargets(_state.value.messages.joinToString("\n") { it.text }).firstOrNull()
        if (target == null) {
            _state.update { it.copy(lastAction = "No preview target in this chat yet") }
        } else {
            openPreviewTarget(target)
        }
    }

    fun draftEvidenceFollowUp(item: WorkbenchEvidenceItem) {
        val handoff = item.toWorkbenchHandoff()
        val mode = when (item.kind) {
            WorkbenchEvidenceKind.Preview -> WorkbenchMode.BrowserQa
            WorkbenchEvidenceKind.File -> WorkbenchMode.RepoCode
            WorkbenchEvidenceKind.Command -> WorkbenchMode.BuildTest
        }
        _state.update {
            it.copy(
                selectedTab = HermesTab.Workbench,
                workbenchMode = mode,
                workbenchGoal = handoff.prompt,
                workbenchBrowserUrl = if (item.kind == WorkbenchEvidenceKind.Preview) item.value else it.workbenchBrowserUrl,
                lastAction = "${handoff.title} drafted"
            )
        }
    }

    fun loadDemoWorkbenchEvidence() {
        if (!debugBuild) return
        _state.update {
            it.copy(
                selectedTab = HermesTab.Workbench,
                debugWorkbenchEvidence = DemoWorkbenchEvidence,
                lastAction = "Demo evidence loaded"
            )
        }
    }

    fun clearDemoWorkbenchEvidence() {
        if (!debugBuild) return
        _state.update {
            it.copy(
                debugWorkbenchEvidence = "",
                lastAction = "Demo evidence cleared"
            )
        }
    }

    fun refreshRoutes() {
        _state.update { it.copy(routeSnapshot = collectRouteSnapshot(getApplication())) }
    }

    fun refreshStatus() {
        launchBusy("Checking Hermes") {
            val normalized = restClient.setBaseUrl(_state.value.backendUrl)
            val status = restClient.status()
            val providers = runCatching { restClient.providers().providers }.getOrDefault(emptyList())
            val identity = runCatching { restClient.me() }.getOrNull()
            _state.update {
                it.copy(
                    backendUrl = normalized,
                    status = status,
                    providers = providers,
                    identity = identity,
                    error = null,
                    lastAction = "Status refreshed"
                )
            }
        }
    }

    fun login() {
        launchBusy("Logging in") {
            val snapshot = _state.value
            val normalized = restClient.setBaseUrl(snapshot.backendUrl)
            restClient.login(snapshot.username.trim(), snapshot.password)
            val identity = restClient.me()
            val status = restClient.status()
            _state.update {
                it.copy(
                    backendUrl = normalized,
                    password = "",
                    identity = identity,
                    status = status,
                    error = null,
                    lastAction = "Logged in as ${identity.displayName.ifBlank { identity.userId }}"
                )
            }
            refreshSessions()
            refreshSurfaces()
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { restClient.logout() }
            clearLocalSession("Logged out")
        }
    }

    private fun clearLocalSession(message: String) {
        gatewayClient?.close()
        gatewayClient = null
        restClient.cookieJar.clear()
        store.clearSession()
        _state.update {
            it.copy(
                identity = null,
                gatewayState = GatewayConnectionState.Idle,
                activeRuntimeSessionId = null,
                selectedStoredSessionId = null,
                selectedProfile = null,
                messages = emptyList(),
                events = emptyList(),
                pendingInteractions = emptyList(),
                lastAction = message
            )
        }
    }

    fun connectGateway() {
        launchBusy("Connecting gateway") {
            ensureGateway()
            _state.update { it.copy(lastAction = "Gateway connected") }
        }
    }

    fun refreshSessions() {
        launchBusy("Refreshing sessions") {
            val sessions = restClient.sessions().sessions
            _state.update {
                it.copy(
                    sessions = sessions,
                    lastAction = "Loaded ${sessions.size} sessions"
                )
            }
        }
    }

    fun applySelectedModel() {
        val snapshot = _state.value
        val provider = snapshot.selectedModelProvider.trim()
        val model = snapshot.selectedModel.trim()
        if (provider.isBlank() || model.isBlank()) {
            _state.update { it.copy(error = "Choose a provider and model first") }
            return
        }

        launchBusy("Setting model") {
            val result = restClient.setMainModel(provider, model)
            val info = runCatching { restClient.modelInfo() }.getOrNull()
            val options = runCatching { restClient.modelOptions() }.getOrNull()
            _state.update {
                it.copy(
                    modelInfo = info ?: it.modelInfo,
                    modelOptions = options ?: it.modelOptions,
                    selectedModelProvider = result.provider.ifBlank { provider },
                    selectedModel = result.model.ifBlank { model },
                    lastAction = "Model set to ${(result.provider.ifBlank { provider })}/${result.model.ifBlank { model }}"
                )
            }
        }
    }

    fun openSession(session: SessionInfo) {
        launchBusy("Opening session") {
            val id = session.displayId
            val response = restClient.sessionMessages(id, session.profile)
            _state.update {
                it.copy(
                    selectedStoredSessionId = id,
                    selectedProfile = session.profile,
                    activeRuntimeSessionId = null,
                    messages = response.messages.map {
                        ChatLine(role = it.role.ifBlank { "message" }, text = it.displayText())
                    },
                    selectedTab = HermesTab.Chat,
                    lastAction = "Opened ${session.displayTitle}"
                )
            }
        }
    }

    fun sendPrompt() {
        val text = _state.value.prompt.trim()
        if (text.isBlank()) return
        sendHermesPrompt(
            PendingHermesSend(
                promptText = text,
                visibleText = text,
                source = HermesSendSource.Chat
            )
        )
    }

    fun retryPendingSend() {
        val pending = _state.value.pendingSend ?: return
        sendHermesPrompt(
            pending.copy(
                stage = HermesSendStage.Preparing,
                attempts = pending.attempts + 1,
                lastError = null
            )
        )
    }

    fun clearPendingSend() {
        _state.update {
            it.copy(
                pendingSend = null,
                error = null,
                lastAction = "Pending send cleared"
            )
        }
    }

    fun launchWorkbenchPrompt() {
        val snapshot = _state.value
        if (snapshot.workbenchGoal.isBlank()) {
            _state.update { it.copy(error = "Workbench goal is required") }
            return
        }

        sendHermesPrompt(
            PendingHermesSend(
                promptText = buildWorkbenchPrompt(snapshot),
                visibleText = "${snapshot.workbenchMode.label}: ${snapshot.workbenchGoal.trim()}",
                source = HermesSendSource.Workbench,
                cwd = snapshot.workbenchCwd,
                profile = snapshot.selectedProfile
            )
        )
    }

    fun respondApproval(interaction: PendingHermesInteraction, choice: String) {
        val sessionId = interaction.sessionId ?: _state.value.activeRuntimeSessionId
        if (sessionId.isNullOrBlank()) {
            _state.update { it.copy(error = "Approval needs a live session id") }
            return
        }

        launchBusy("Responding approval") {
            ensureGateway().respondApproval(sessionId, choice)
            clearPending(interaction, "Approval sent: $choice")
        }
    }

    fun respondClarify(interaction: PendingHermesInteraction, answer: String) {
        val requestId = interaction.requestId
        val trimmed = answer.trim()
        if (requestId.isNullOrBlank() || trimmed.isBlank()) {
            _state.update { it.copy(error = "Clarify answer is required") }
            return
        }

        launchBusy("Answering clarify") {
            ensureGateway().respondClarify(requestId, trimmed)
            clearPending(interaction, "Clarify answer sent")
        }
    }

    fun cancelSudo(interaction: PendingHermesInteraction) {
        val requestId = interaction.requestId
        if (requestId.isNullOrBlank()) {
            _state.update { it.copy(error = "Sudo request id is missing") }
            return
        }

        launchBusy("Cancelling sudo") {
            ensureGateway().respondSudo(requestId, password = "")
            clearPending(interaction, "Sudo request cancelled from Android")
        }
    }

    fun respondSecret(interaction: PendingHermesInteraction, value: String) {
        val requestId = interaction.requestId
        if (requestId.isNullOrBlank()) {
            _state.update { it.copy(error = "Secret request id is missing") }
            return
        }

        launchBusy("Sending secret") {
            ensureGateway().respondSecret(requestId, value)
            clearPending(interaction, if (value.isBlank()) "Secret request cancelled" else "Secret sent")
        }
    }

    private suspend fun ensureGateway(): HermesGatewayWsClient {
        gatewayClient?.takeIf { _state.value.gatewayState == GatewayConnectionState.Open }?.let { return it }

        val gateway = HermesGatewayWsClient(restClient)
        gatewayClient = gateway
        gateway.connect(
            onState = { next -> _state.update { it.copy(gatewayState = next) } },
            onEvent = ::handleGatewayEvent,
            onLog = { line -> appendEvent(GatewayEventUi(type = "frame", text = line, important = false)) }
        )
        return gateway
    }

    private fun clearPending(interaction: PendingHermesInteraction, message: String) {
        _state.update {
            it.copy(
                pendingInteractions = it.pendingInteractions.clearPendingInteraction(interaction),
                lastAction = message
            )
        }
    }

    fun refreshSurfaces() {
        launchBusy("Refreshing management surfaces") {
            val control = coroutineScope {
                val profiles = async { runCatching { restClient.profiles().profiles }.getOrDefault(emptyList()) }
                val modelInfo = async { runCatching { restClient.modelInfo() }.getOrNull() }
                val modelOptions = async { runCatching { restClient.modelOptions() }.getOrNull() }
                val surfaces = listOf(
                    "Profiles" to "/api/profiles",
                    "Models" to "/api/model/info",
                    "Model options" to "/api/model/options",
                    "Skills" to "/api/skills",
                    "Cron" to "/api/cron/jobs",
                    "Memory" to "/api/memory",
                    "MCP" to "/api/mcp/servers",
                    "Messaging" to "/api/messaging/platforms",
                    "Toolsets" to "/api/tools/toolsets",
                    "Artifacts" to "/api/artifacts",
                    "Agents" to "/api/agents",
                    "Logs" to "/api/logs?lines=80"
                ).map { (label, path) ->
                    async { loadSurface(label, path) }
                }.awaitAll()

                ControlRefreshResult(
                    profiles = profiles.await(),
                    modelInfo = modelInfo.await(),
                    modelOptions = modelOptions.await(),
                    surfaces = surfaces
                )
            }

            _state.update {
                val optionsProvider = control.modelOptions?.provider ?: control.modelInfo?.provider ?: it.selectedModelProvider
                val optionsModel = control.modelOptions?.model ?: control.modelInfo?.model ?: it.selectedModel
                it.copy(
                    profiles = control.profiles,
                    modelInfo = control.modelInfo,
                    modelOptions = control.modelOptions,
                    selectedModelProvider = optionsProvider.orEmpty(),
                    selectedModel = optionsModel.orEmpty(),
                    surfaces = control.surfaces,
                    lastAction = "Management surfaces refreshed"
                )
            }
        }
    }

    private suspend fun ensureRuntimeSession(
        gateway: HermesGatewayWsClient,
        cwd: String? = null,
        profile: String? = null
    ): String {
        _state.value.activeRuntimeSessionId?.let { return it }

        val selectedStored = _state.value.selectedStoredSessionId
        if (!selectedStored.isNullOrBlank()) {
            val resumed = gateway.resumeSession(selectedStored, _state.value.selectedProfile)
            _state.update {
                it.copy(
                    activeRuntimeSessionId = resumed.sessionId,
                    messages = if (resumed.messages.isNotEmpty()) {
                        resumed.messages.map { msg -> ChatLine(role = msg.role, text = msg.displayText()) }
                    } else {
                        it.messages
                    }
                )
            }
            return resumed.sessionId
        }

        val created = gateway.createSession(cwd = cwd, profile = profile)
        _state.update {
            it.copy(
                activeRuntimeSessionId = created.sessionId,
                selectedStoredSessionId = created.storedSessionId ?: created.sessionId,
                selectedProfile = profile?.takeIf { name -> name.isNotBlank() } ?: it.selectedProfile
            )
        }
        return created.sessionId
    }

    private fun buildWorkbenchPrompt(snapshot: HermesUiState): String {
        val route = snapshot.routeSnapshot
        val routeLine = if (route == null) {
            "Phone route: not sampled"
        } else {
            "Phone route: ${route.deviceLabel}; Android ${route.androidLabel}; transports=${route.activeTransports.joinToString()}; vpn=${route.vpnActive}; validated=${route.networkValidated}"
        }
        val browser = snapshot.workbenchBrowserUrl.ifBlank { "not set" }
        val cwd = snapshot.workbenchCwd.ifBlank { "/home/user/projects" }
        return """
            Hermes Mobile Workbench request

            Mode: ${snapshot.workbenchMode.label}
            Target desktop host cwd: $cwd
            Mobile preview/browser target: $browser
            $routeLine

            Goal:
            ${snapshot.workbenchGoal.trim()}

            Mode instructions:
            ${snapshot.workbenchMode.instructions}

            Operating rules:
            - Treat desktop host Hermes as the source of truth for sessions, memory, skills, models, profiles, and tool state.
            - Use Hermes Agent core tools and existing configured providers on desktop host; do not ask the Android app to hold provider secrets.
            - Prefer small, verified changes. Report commands, files changed, and evidence.
            - If you need env reveal, credential edits, gateway restart, hooks, imports, deletes, updates, or other dangerous/admin actions, stop and ask for explicit confirmation first.
            - When producing a visual/app artifact, include a Hermes Desktop preview marker like [Preview: name](#preview/<encoded target>) when possible so Hermes Mobile can open it in its Workbench browser.
        """.trimIndent()
    }

    private suspend fun loadSurface(label: String, path: String): SurfaceSnapshot {
        return runCatching {
            val body = restClient.rawGet(path)
            SurfaceSnapshot(
                label = label,
                endpoint = path,
                state = SurfaceState.Ready,
                preview = restClient.surfaceSummary(body).ifBlank { restClient.pretty(body).take(1200) }
            )
        }.getOrElse { error ->
            SurfaceSnapshot(
                label = label,
                endpoint = path,
                state = SurfaceState.Error,
                preview = error.readableMessage()
            )
        }
    }

    private fun handleGatewayEvent(event: GatewayEvent) {
        val text = event.text()
        appendEvent(
            GatewayEventUi(
                type = event.type,
                sessionId = event.sessionId,
                text = text.ifBlank { event.payload.extractText().take(320) },
                important = event.type.startsWith("tool.") ||
                    event.type in setOf("approval.request", "clarify.request", "sudo.request", "secret.request", "error")
            )
        )

        parsePendingInteraction(event)?.let { pending ->
            _state.update {
                it.copy(
                    pendingInteractions = it.pendingInteractions.upsertPendingInteraction(pending),
                    lastAction = "Hermes needs input: ${pending.title}"
                )
            }
        }

        when (event.type) {
            "session.info" -> {
                event.sessionId?.let { id ->
                    _state.update { it.copy(activeRuntimeSessionId = id) }
                }
            }
            "message.start" -> {
                _state.update { it.copy(messages = it.messages + ChatLine(role = "assistant", text = "", pending = true)) }
            }
            "message.delta", "thinking.delta", "reasoning.delta" -> appendAssistantText(text)
            "message.complete" -> {
                if (text.isNotBlank()) appendAssistantText(text)
                _state.update { state ->
                    state.copy(
                        messages = state.messages.mapIndexed { index, line ->
                            if (index == state.messages.lastIndex && line.role == "assistant") line.copy(pending = false) else line
                        },
                        pendingInteractions = state.pendingInteractions.clearSessionInteractions(event.sessionId)
                    )
                }
            }
            "error" -> {
                _state.update {
                    it.copy(
                        messages = it.messages + ChatLine(role = "system", text = text.ifBlank { "Gateway error" }),
                        pendingInteractions = it.pendingInteractions.clearSessionInteractions(event.sessionId)
                    )
                }
            }
        }
    }

    private fun appendAssistantText(delta: String) {
        if (delta.isBlank()) return
        _state.update { state ->
            val lines = state.messages.toMutableList()
            val last = lines.lastOrNull()
            if (last?.role == "assistant") {
                lines[lines.lastIndex] = last.copy(text = last.text + delta, pending = true)
            } else {
                lines += ChatLine(role = "assistant", text = delta, pending = true)
            }
            state.copy(messages = lines)
        }
    }

    private fun appendEvent(event: GatewayEventUi) {
        _state.update { state ->
            state.copy(events = (listOf(event) + state.events).take(80))
        }
    }

    private fun sendHermesPrompt(draft: PendingHermesSend) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    busy = true,
                    busyLabel = "Sending prompt",
                    error = null,
                    pendingSend = draft.copy(stage = HermesSendStage.Preparing, lastError = null),
                    selectedTab = HermesTab.Chat
                )
            }

            var stage = HermesSendStage.Preparing
            runCatching {
                stage = HermesSendStage.Gateway
                updateSendStage(stage)
                val gateway = ensureGateway()

                stage = HermesSendStage.RuntimeSession
                updateSendStage(stage)
                val runtimeId = ensureRuntimeSession(gateway, cwd = draft.cwd, profile = draft.profile)

                stage = HermesSendStage.Submit
                updateSendStage(stage)
                gateway.submitPrompt(runtimeId, draft.promptText)

                acceptSend(draft, runtimeId)
                runCatching { refreshSessionsInline() }
            }.onFailure { error ->
                failSend(draft, stage, error)
            }

            _state.update { it.copy(busy = false, busyLabel = "") }
        }
    }

    private fun updateSendStage(stage: HermesSendStage) {
        _state.update { state ->
            state.copy(pendingSend = state.pendingSend?.copy(stage = stage, lastError = null))
        }
    }

    private fun acceptSend(draft: PendingHermesSend, runtimeId: String) {
        _state.update { state ->
            val clearChatPrompt = draft.source == HermesSendSource.Chat && state.prompt.trim() == draft.promptText.trim()
            state.copy(
                prompt = if (clearChatPrompt) "" else state.prompt,
                workbenchGoal = if (draft.source == HermesSendSource.Workbench) "" else state.workbenchGoal,
                pendingSend = null,
                activeRuntimeSessionId = runtimeId,
                selectedTab = HermesTab.Chat,
                messages = state.messages + ChatLine(role = "user", text = draft.visibleText),
                error = null,
                lastAction = "Prompt accepted by Hermes"
            )
        }
        appendEvent(
            GatewayEventUi(
                type = "mobile.send.accepted",
                text = "${draft.source.label} prompt accepted after ${draft.attempts} attempt(s)",
                important = false
            )
        )
    }

    private fun failSend(draft: PendingHermesSend, stage: HermesSendStage, error: Throwable) {
        val message = safeSendDiagnostic(error.readableMessage(), draft.promptText, draft.visibleText)
        gatewayClient?.close()
        gatewayClient = null
        _state.update { state ->
            val current = state.pendingSend ?: draft
            state.copy(
                gatewayState = GatewayConnectionState.Error,
                pendingSend = current.copy(stage = stage, lastError = message),
                error = "${stage.label} failed: $message",
                lastAction = "Send failed; prompt kept for retry"
            )
        }
        appendEvent(
            GatewayEventUi(
                type = "mobile.send.failed",
                text = "${stage.diagnosticName} failed: $message; prompt preserved",
                important = true
            )
        )
    }

    private suspend fun refreshSessionsInline() {
        val sessions = restClient.sessions().sessions
        _state.update { it.copy(sessions = sessions) }
    }

    private fun launchBusy(label: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, busyLabel = label, error = null) }
            runCatching { block() }
                .onFailure { error -> _state.update { it.copy(error = error.readableMessage()) } }
            _state.update { it.copy(busy = false, busyLabel = "") }
        }
    }

    private fun Throwable.readableMessage(): String {
        return when (this) {
            is HermesApiException -> "HTTP $code: ${responseBody.take(220)}"
            else -> message ?: javaClass.simpleName
        }
    }
}

data class HermesUiState(
    val backendUrl: String = HermesUrl.DefaultBaseUrl,
    val username: String = "user",
    val password: String = "",
    val themeName: String = "nous",
    val status: HermesStatus? = null,
    val providers: List<AuthProvider> = emptyList(),
    val profiles: List<ProfileInfo> = emptyList(),
    val modelInfo: ModelInfoResponse? = null,
    val modelOptions: ModelOptionsResponse? = null,
    val selectedModelProvider: String = "",
    val selectedModel: String = "",
    val identity: AuthMeResponse? = null,
    val gatewayState: GatewayConnectionState = GatewayConnectionState.Idle,
    val routeSnapshot: RouteSnapshot? = null,
    val sessions: List<SessionInfo> = emptyList(),
    val selectedStoredSessionId: String? = null,
    val selectedProfile: String? = null,
    val activeRuntimeSessionId: String? = null,
    val messages: List<ChatLine> = emptyList(),
    val events: List<GatewayEventUi> = emptyList(),
    val pendingInteractions: List<PendingHermesInteraction> = emptyList(),
    val pendingSend: PendingHermesSend? = null,
    val surfaces: List<SurfaceSnapshot> = emptyList(),
    val parityItems: List<DesktopParityItem> = HermesDesktopParityItems,
    val workbenchMode: WorkbenchMode = WorkbenchMode.AndroidApp,
    val workbenchGoal: String = "",
    val workbenchCwd: String = "/home/user/projects",
    val workbenchBrowserUrl: String = HermesUrl.DefaultBaseUrl,
    val debugBuild: Boolean = false,
    val debugWorkbenchEvidence: String = "",
    val selectedTab: HermesTab = HermesTab.Chat,
    val prompt: String = "",
    val busy: Boolean = false,
    val busyLabel: String = "",
    val error: String? = null,
    val lastAction: String = ""
)

data class ChatLine(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val text: String,
    val pending: Boolean = false
)

data class GatewayEventUi(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val sessionId: String? = null,
    val text: String = "",
    val important: Boolean = false,
    val at: String = Instant.now().toString()
)

data class SurfaceSnapshot(
    val label: String,
    val endpoint: String,
    val state: SurfaceState,
    val preview: String
)

private data class ControlRefreshResult(
    val profiles: List<ProfileInfo>,
    val modelInfo: ModelInfoResponse?,
    val modelOptions: ModelOptionsResponse?,
    val surfaces: List<SurfaceSnapshot>
)

enum class SurfaceState {
    Ready,
    Error
}

enum class WorkbenchMode(val label: String, val instructions: String) {
    AndroidApp(
        "Android app build",
        "Create or modify a native Android app on desktop host, run the Gradle build/tests, and use adb for install/logcat verification when a phone is connected."
    ),
    WebPreview(
        "Web app and preview",
        "Create or modify a web app, start a local or tailnet-reachable dev server, verify the route, and provide a preview URL for the mobile browser."
    ),
    RepoCode(
        "Repo coding task",
        "Work in the selected cwd/repo, inspect current state, make the smallest safe code change, run tests, and summarize the diff."
    ),
    BuildTest(
        "Build and test run",
        "Run the requested build/test/check commands, diagnose failures, and return concise next fixes with command evidence."
    ),
    BrowserQa(
        "Browser QA",
        "Use Hermes browser/web tooling to open the target URL, inspect behavior, test workflows, capture evidence, and report defects or fixes."
    )
}

data class DesktopParityItem(
    val desktopRoute: String,
    val desktopSurface: String,
    val mobileSurface: String,
    val status: ParityStatus,
    val notes: String
)

enum class ParityStatus(val label: String) {
    Ready("Ready"),
    InProgress("In progress"),
    Planned("Planned"),
    Locked("Locked")
}

val HermesDesktopParityItems = listOf(
    DesktopParityItem("/", "Chat", "Chat tab", ParityStatus.InProgress, "Gateway chat, streaming deltas, sessions, tool events, approval controls, clarify answers, secret capture, and sudo cancel are live."),
    DesktopParityItem("/settings", "Settings", "Manage + Diagnostics", ParityStatus.InProgress, "Read-only status/config surfaces are live, main model switching is available, and sensitive edits stay locked."),
    DesktopParityItem("/command-center", "Command Center", "Workbench", ParityStatus.InProgress, "Mobile workbench can launch coding/build/browser lanes through Hermes."),
    DesktopParityItem("/skills", "Skills", "Manage", ParityStatus.Ready, "Read-only skills surface loads from desktop host."),
    DesktopParityItem("/messaging", "Messaging", "Manage", ParityStatus.InProgress, "Platform status is visible; enable/edit flows remain locked."),
    DesktopParityItem("/artifacts", "Artifacts", "Workbench browser", ParityStatus.InProgress, "Preview markers and tailnet URLs can open in the embedded browser."),
    DesktopParityItem("/cron", "Cron", "Manage", ParityStatus.Ready, "Read-only cron/jobs surface loads from desktop host."),
    DesktopParityItem("/profiles", "Profiles", "Sessions + Manage", ParityStatus.InProgress, "Profiles are selectable for new mobile Hermes turns and session resume preserves profile hints."),
    DesktopParityItem("/agents", "Agents", "Workbench + Tool Activity", ParityStatus.Planned, "Subagent tree and ownership controls need a dedicated mobile pass."),
)

enum class HermesTab(val label: String) {
    Chat("Chat"),
    Sessions("Sessions"),
    Workbench("Workbench"),
    Manage("Manage"),
    Diagnostics("Diagnostics")
}

private val DemoWorkbenchEvidence = """
    Hermes Mobile deterministic evidence fixture

    [Preview: Tailnet demo](#preview/http%3A%2F%2F100.64.0.1%3A9119%2F)
    Changed /home/user/projects/hermes-mobile-android/app/src/main/java/org/hermesmobile/client/ui/HermesMobileApp.kt
    Changed app/src/main/java/org/hermesmobile/client/runtime/WorkbenchEvidence.kt

    Commands run:
    ./gradlew testDebugUnitTest assembleDebug
    adb install -r app/build/outputs/apk/debug/app-debug.apk
""".trimIndent()
