package org.hermesmobile.client.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import org.hermesmobile.client.data.GatewayConnectionState
import org.hermesmobile.client.model.ModelOptionProvider
import org.hermesmobile.client.model.SessionInfo
import org.hermesmobile.client.runtime.HermesInteractionKind
import org.hermesmobile.client.runtime.HermesSendSource
import org.hermesmobile.client.runtime.PendingHermesInteraction
import org.hermesmobile.client.runtime.PendingHermesSend
import org.hermesmobile.client.runtime.WorkbenchEvidence
import org.hermesmobile.client.runtime.WorkbenchEvidenceChunk
import org.hermesmobile.client.runtime.WorkbenchEvidenceItem
import org.hermesmobile.client.runtime.WorkbenchEvidenceKind
import org.hermesmobile.client.runtime.WorkbenchEvidenceSource
import org.hermesmobile.client.runtime.extractWorkbenchEvidence
import org.hermesmobile.client.runtime.previewName
import org.hermesmobile.client.runtime.toWorkbenchHandoff

@Composable
fun HermesMobileApp(viewModel: HermesMobileViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val deviceDensity = LocalDensity.current
    val appDensity = remember(deviceDensity) {
        val densityScale = if (deviceDensity.density >= 3.4f) 0.84f else 1f
        Density(
            density = deviceDensity.density * densityScale,
            fontScale = deviceDensity.fontScale.coerceAtMost(1.05f)
        )
    }

    HermesMobileTheme(state.themeName) {
        CompositionLocalProvider(LocalDensity provides appDensity) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = { HermesTopBar(state, viewModel) },
                bottomBar = { HermesBottomBar(state, viewModel) }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (state.error != null) {
                        StatusBanner(text = state.error, error = true)
                    } else if (state.lastAction.isNotBlank()) {
                        StatusBanner(text = state.lastAction, error = false)
                    }

                    when {
                        state.identity == null -> LoginScreen(state, viewModel)
                        state.selectedTab == HermesTab.Chat -> ChatScreen(state, viewModel)
                        state.selectedTab == HermesTab.Sessions -> SessionsScreen(state, viewModel)
                        state.selectedTab == HermesTab.Workbench -> WorkbenchScreen(state, viewModel)
                        state.selectedTab == HermesTab.Manage -> ManageScreen(state, viewModel)
                        state.selectedTab == HermesTab.Diagnostics -> DiagnosticsScreen(state, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun HermesTopBar(state: HermesUiState, viewModel: HermesMobileViewModel) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("H", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Hermes Agent", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(
                    text = state.status?.let { "Android peer / v${it.version} / ${state.themeName}" }
                        ?: state.backendUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (state.busy) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            if (state.identity != null) {
                IconButtonLike(icon = Icons.Outlined.Logout, label = "Logout", onClick = viewModel::logout)
            }
        }
    }
}

@Composable
private fun HermesBottomBar(state: HermesUiState, viewModel: HermesMobileViewModel) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HermesTab.entries.forEach { tab ->
                val selected = state.selectedTab == tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { viewModel.setTab(tab) },
                    color = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            tab.icon(),
                            contentDescription = tab.label,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            tab.navLabel(),
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(state: HermesUiState, viewModel: HermesMobileViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Panel(title = "Tailnet Login", icon = Icons.Outlined.Lock) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.backendUrl,
                    onValueChange = viewModel::setBackendUrl,
                    label = { Text("Backend URL") },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.username,
                    onValueChange = viewModel::setUsername,
                    label = { Text("Username") },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.password,
                    onValueChange = viewModel::setPassword,
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = viewModel::refreshStatus) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Check")
                    }
                    Button(onClick = viewModel::login, enabled = state.password.isNotBlank()) {
                        Icon(Icons.Outlined.Key, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Login")
                    }
                }
            }
        }
        item { StatusPanel(state) }
        item { DiagnosticsPanel(state, viewModel) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(state: HermesUiState, viewModel: HermesMobileViewModel) {
    var showTools by remember { mutableStateOf(false) }
    var showModelProfile by remember { mutableStateOf(false) }
    var showArtifacts by remember { mutableStateOf(false) }
    val toolsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val modelSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val artifactsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chatEvidence = remember(state.messages, state.events, state.selectedStoredSessionId, state.activeRuntimeSessionId) {
        extractWorkbenchEvidence(chatEvidenceChunks(state))
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectionPill(modifier = Modifier.weight(1.15f), state.gatewayState)
            CompactActionPill(
                modifier = Modifier.weight(0.9f),
                icon = Icons.Outlined.Refresh,
                label = "Connect",
                onClick = viewModel::connectGateway
            )
            CompactActionPill(
                modifier = Modifier.weight(0.8f),
                icon = Icons.Outlined.Build,
                label = "Tools",
                onClick = { showTools = true }
            )
        }

        ChatContextStrip(state = state, onOpenModelProfile = { showModelProfile = true })
        InteractionQueuePanel(state.pendingInteractions, viewModel)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.messages.isEmpty()) {
                item {
                    EmptyState("No mobile session loaded", "Start a new Hermes turn or open a desktop session.")
                }
            }
            items(state.messages, key = { it.id }) { line ->
                MessageBubble(line)
            }
        }

        ChatArtifactStrip(
            evidence = chatEvidence,
            viewModel = viewModel,
            onOpenArtifacts = { showArtifacts = true }
        )
        ChatComposer(state, viewModel)
    }

    if (showTools) {
        ModalBottomSheet(onDismissRequest = { showTools = false }, sheetState = toolsSheetState) {
            ToolEventList(state, viewModel)
        }
    }

    if (showModelProfile) {
        ModalBottomSheet(onDismissRequest = { showModelProfile = false }, sheetState = modelSheetState) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { ModelProfilePanel(state, viewModel, compact = false) }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }

    if (showArtifacts) {
        ModalBottomSheet(onDismissRequest = { showArtifacts = false }, sheetState = artifactsSheetState) {
            ChatArtifactDrawer(
                evidence = chatEvidence,
                viewModel = viewModel,
                onClose = { showArtifacts = false }
            )
        }
    }
}

@Composable
private fun ChatComposer(state: HermesUiState, viewModel: HermesMobileViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    state.selectedStoredSessionId ?: "New session",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.pendingSend?.let { pending ->
                PendingSendPanel(pending = pending, viewModel = viewModel, busy = state.busy)
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.prompt,
                onValueChange = viewModel::setPrompt,
                label = { Text("Prompt") },
                minLines = 1,
                maxLines = 3
            )
            val failedPending = state.pendingSend?.takeIf {
                it.lastError != null &&
                    it.promptText.trim() == state.prompt.trim() &&
                    it.source == HermesSendSource.Chat
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                onClick = if (failedPending != null) viewModel::retryPendingSend else viewModel::sendPrompt,
                enabled = state.prompt.isNotBlank() && !state.busy
            ) {
                Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (failedPending != null) "Retry Send" else "Send", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PendingSendPanel(pending: PendingHermesSend, viewModel: HermesMobileViewModel, busy: Boolean) {
    val failed = pending.lastError != null
    val color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = if (failed) 0.14f else 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.76f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (failed) {
                    Icon(Icons.Outlined.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = color)
                }
                Text(
                    if (failed) "${pending.stage.label} failed" else pending.stage.label,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
                Text("try ${pending.attempts}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                pending.lastError ?: "${pending.source.label} prompt waiting for Hermes acknowledgement",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (failed) {
                Text(
                    pending.preview,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = viewModel::retryPendingSend, enabled = !busy) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retry")
                    }
                    TextButton(onClick = viewModel::clearPendingSend, enabled = !busy) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionsScreen(state: HermesUiState, viewModel: HermesMobileViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::refreshSessions) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Refresh")
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.sessions.isEmpty()) {
                item { EmptyState("No sessions loaded", "Refresh after login to read desktop host's Hermes state.") }
            }
            items(state.sessions, key = { it.displayId }) { session ->
                SessionRow(session = session, selected = state.selectedStoredSessionId == session.displayId) {
                    viewModel.openSession(session)
                }
            }
        }
    }
}

@Composable
private fun WorkbenchScreen(state: HermesUiState, viewModel: HermesMobileViewModel) {
    val workbenchEvidence = extractWorkbenchEvidence(workbenchEvidenceChunks(state))
    val previewTargets = workbenchEvidence.previews

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { WorkbenchEvidencePanel(state, workbenchEvidence, viewModel) }
        item { WorkbenchLauncherPanel(state, viewModel) }
        item { BrowserPreviewPanel(state, viewModel, previewTargets) }
        item { ModelProfilePanel(state, viewModel, compact = false) }
        item { ThemePanel(state, viewModel) }
        item { DesktopParityPanel(state) }
    }
}

private fun chatEvidenceChunks(state: HermesUiState): List<WorkbenchEvidenceChunk> {
    val sessionId = state.activeRuntimeSessionId ?: state.selectedStoredSessionId
    return buildList {
        state.events.forEach { event ->
            add(
                WorkbenchEvidenceChunk(
                    source = WorkbenchEvidenceSource.GatewayEvent,
                    text = event.text,
                    sourceDetail = event.type,
                    sessionId = event.sessionId ?: sessionId,
                    eventType = event.type,
                    at = event.at
                )
            )
        }
        state.messages.forEach { line ->
            add(
                WorkbenchEvidenceChunk(
                    source = WorkbenchEvidenceSource.Chat,
                    text = line.text,
                    sourceDetail = line.role,
                    sessionId = sessionId
                )
            )
        }
    }
}

private fun workbenchEvidenceChunks(state: HermesUiState): List<WorkbenchEvidenceChunk> {
    return buildList {
        addAll(chatEvidenceChunks(state))
        state.debugWorkbenchEvidence.takeIf { it.isNotBlank() }?.let { text ->
            add(
                WorkbenchEvidenceChunk(
                    source = WorkbenchEvidenceSource.DebugFixture,
                    text = text,
                    sourceDetail = "deterministic fixture"
                )
            )
        }
    }
}

@Composable
private fun ChatContextStrip(state: HermesUiState, onOpenModelProfile: () -> Unit) {
    val provider = state.modelInfo?.provider ?: state.modelOptions?.provider ?: state.selectedModelProvider
    val model = state.modelInfo?.model ?: state.modelOptions?.model ?: state.selectedModel
    val modelLabel = listOf(provider, model)
        .filterNot { it.isNullOrBlank() }
        .joinToString(" / ")
        .ifBlank { "model not loaded" }
    val profileLabel = state.selectedProfile ?: "default"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Profile: $profileLabel",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = modelLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            CompactTextPill(label = "Switch", onClick = onOpenModelProfile)
        }
    }
}

@Composable
private fun ChatArtifactStrip(
    evidence: WorkbenchEvidence,
    viewModel: HermesMobileViewModel,
    onOpenArtifacts: () -> Unit
) {
    if (evidence.isEmpty) return

    val firstPreview = evidence.items.firstOrNull { it.kind == WorkbenchEvidenceKind.Preview }?.value

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenArtifacts,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Storage, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Artifacts", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                Text(
                    evidence.compactCountSummary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            firstPreview?.let { preview ->
                CompactTextPill(label = "Preview", onClick = { viewModel.openPreviewTarget(preview) })
            }
            CompactTextPill(label = "Details", onClick = onOpenArtifacts)
            CompactTextPill(label = "Work", onClick = viewModel::openWorkbenchEvidence)
        }
    }
}

@Composable
private fun ChatArtifactDrawer(
    evidence: WorkbenchEvidence,
    viewModel: HermesMobileViewModel,
    onClose: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Chat Artifacts", style = MaterialTheme.typography.titleMedium)
                    Text(
                        evidence.compactCountSummary(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        item {
            InfoLine("Sources", evidence.sourceSummary())
        }

        if (evidence.previews.isNotEmpty()) {
            item { Text("Previews", style = MaterialTheme.typography.labelLarge) }
            items(
                evidence.items.filter { it.kind == WorkbenchEvidenceKind.Preview }.take(4),
                key = { "${it.kind.name}:${it.value}:${it.sourceDescription}" }
            ) { item ->
                ChatArtifactDrawerRow(item, viewModel, onClose)
            }
        }

        if (evidence.filePaths.isNotEmpty()) {
            item { Text("Files touched", style = MaterialTheme.typography.labelLarge) }
            items(
                evidence.items.filter { it.kind == WorkbenchEvidenceKind.File }.take(5),
                key = { "${it.kind.name}:${it.value}:${it.sourceDescription}" }
            ) { item ->
                ChatArtifactDrawerRow(item, viewModel, onClose)
            }
        }

        if (evidence.commands.isNotEmpty()) {
            item { Text("Runs", style = MaterialTheme.typography.labelLarge) }
            items(
                evidence.items.filter { it.kind == WorkbenchEvidenceKind.Command }.take(5),
                key = { "${it.kind.name}:${it.value}:${it.sourceDescription}" }
            ) { item ->
                ChatArtifactDrawerRow(item, viewModel, onClose)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onClose) {
                    Text("Close")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.openWorkbenchEvidence()
                        onClose()
                    }
                ) {
                    Text("Workbench")
                }
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun ChatArtifactDrawerRow(
    item: WorkbenchEvidenceItem,
    viewModel: HermesMobileViewModel,
    onClose: () -> Unit
) {
    val handoff = item.toWorkbenchHandoff()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Text(item.label, modifier = Modifier.width(58.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        item.value,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        item.sourceDescription,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "Handoff: ${handoff.risk.label}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.kind == WorkbenchEvidenceKind.Preview) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.openPreviewTarget(item.value)
                            onClose()
                        }
                    ) {
                        Text("Open")
                    }
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.draftEvidenceFollowUp(item)
                        onClose()
                    }
                ) {
                    Text("Draft")
                }
            }
        }
    }
}

@Composable
private fun ModelProfilePanel(state: HermesUiState, viewModel: HermesMobileViewModel, compact: Boolean) {
    val currentProvider = state.modelInfo?.provider ?: state.modelOptions?.provider ?: state.selectedModelProvider
    val currentModel = state.modelInfo?.model ?: state.modelOptions?.model ?: state.selectedModel
    val providers = state.modelOptions?.providers.orEmpty()
        .filter { it.models.isNotEmpty() && it.authenticated != false }
    val selectedProvider = state.selectedModelProvider.ifBlank { currentProvider.orEmpty() }
    val selectedProviderInfo = providers.firstOrNull { it.slug == selectedProvider } ?: providers.firstOrNull()
    val selectedModels = selectedProviderInfo?.models.orEmpty().take(if (compact) 5 else 16)
    val profileLimit = if (compact) 4 else 12

    Panel(title = "Model / Profile", icon = Icons.Outlined.Person) {
        InfoLine("Current", listOf(currentProvider, currentModel).filterNot { it.isNullOrBlank() }.joinToString(" / ").ifBlank { "unknown" })
        InfoLine("Profile", state.selectedProfile ?: "default")

        Text("Profiles", style = MaterialTheme.typography.labelLarge)
        SelectionButton(
            selected = state.selectedProfile == null,
            label = "default",
            onClick = { viewModel.selectProfile(null) }
        )
        state.profiles
            .filter { !it.isDefault }
            .take(profileLimit)
            .forEach { profile ->
                SelectionButton(
                    selected = state.selectedProfile == profile.name,
                    label = profile.name,
                    onClick = { viewModel.selectProfile(profile.name) }
                )
            }
        if (state.profiles.isEmpty()) {
            Text("Refresh management surfaces to load Hermes profiles.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text("Providers", style = MaterialTheme.typography.labelLarge)
        if (providers.isEmpty()) {
            Text("No authenticated model options loaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            providers.take(if (compact) 4 else 10).forEach { provider ->
                SelectionButton(
                    selected = selectedProvider == provider.slug,
                    label = provider.displayLabel(),
                    onClick = { viewModel.selectModelProvider(provider.slug) }
                )
            }
        }

        if (selectedModels.isNotEmpty()) {
            Text("Models", style = MaterialTheme.typography.labelLarge)
            selectedModels.forEach { model ->
                val locked = selectedProviderInfo?.unavailableModels?.contains(model) == true
                SelectionButton(
                    selected = state.selectedModel == model && state.selectedModelProvider == selectedProviderInfo?.slug,
                    label = if (locked) "$model / locked" else model,
                    enabled = !locked,
                    onClick = {
                        selectedProviderInfo?.slug?.let(viewModel::selectModelProvider)
                        viewModel.selectModel(model)
                    }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::refreshSurfaces) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Refresh")
            }
            Button(
                onClick = viewModel::applySelectedModel,
                enabled = state.selectedModelProvider.isNotBlank() && state.selectedModel.isNotBlank() && !state.busy
            ) {
                Text("Apply Model")
            }
        }
    }
}

@Composable
private fun SelectionButton(
    selected: Boolean,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    if (selected) {
        Button(modifier = Modifier.fillMaxWidth(), enabled = enabled, onClick = onClick) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = enabled, onClick = onClick) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InteractionQueuePanel(interactions: List<PendingHermesInteraction>, viewModel: HermesMobileViewModel) {
    if (interactions.isEmpty()) return

    Panel(title = "Needs Input", icon = Icons.Outlined.Warning) {
        interactions.forEach { interaction ->
            InteractionCard(interaction, viewModel)
        }
    }
}

@Composable
private fun InteractionCard(interaction: PendingHermesInteraction, viewModel: HermesMobileViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    when (interaction.kind) {
                        HermesInteractionKind.Approval -> Icons.Outlined.Build
                        HermesInteractionKind.Clarify -> Icons.Outlined.Info
                        HermesInteractionKind.Sudo -> Icons.Outlined.Lock
                        HermesInteractionKind.Secret -> Icons.Outlined.Key
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(interaction.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        interaction.sessionId ?: "current session",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(interaction.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (interaction.kind) {
                HermesInteractionKind.Approval -> ApprovalControls(interaction, viewModel)
                HermesInteractionKind.Clarify -> ClarifyControls(interaction, viewModel)
                HermesInteractionKind.Sudo -> SudoControls(interaction, viewModel)
                HermesInteractionKind.Secret -> SecretControls(interaction, viewModel)
            }
        }
    }
}

@Composable
private fun ApprovalControls(interaction: PendingHermesInteraction, viewModel: HermesMobileViewModel) {
    var confirmAlways by remember(interaction.stableKey) { mutableStateOf(false) }
    if (interaction.command.isNotBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                modifier = Modifier.padding(10.dp),
                text = interaction.command,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    Button(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.respondApproval(interaction, "once") }) {
        Text("Run Once")
    }
    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.respondApproval(interaction, "session") }) {
        Text("Allow Session")
    }
    if (confirmAlways) {
        Text(
            "Always allow persists the command pattern in Hermes config.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
        Button(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.respondApproval(interaction, "always") }) {
            Text("Confirm Always Allow")
        }
        TextButton(onClick = { confirmAlways = false }) {
            Text("Cancel Always")
        }
    } else {
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { confirmAlways = true }) {
            Text("Always Allow...")
        }
    }
    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.respondApproval(interaction, "deny") }) {
        Text("Deny")
    }
}

@Composable
private fun ClarifyControls(interaction: PendingHermesInteraction, viewModel: HermesMobileViewModel) {
    var draft by remember(interaction.stableKey) { mutableStateOf("") }

    interaction.choices.forEach { choice ->
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.respondClarify(interaction, choice) }) {
            Text(choice, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = draft,
        onValueChange = { draft = it },
        label = { Text("Answer") },
        minLines = 1,
        maxLines = 4
    )
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = draft.isNotBlank(),
        onClick = { viewModel.respondClarify(interaction, draft) }
    ) {
        Text("Send Answer")
    }
}

@Composable
private fun SudoControls(interaction: PendingHermesInteraction, viewModel: HermesMobileViewModel) {
    Text(
        "Android does not provide a password fallback here. Use the trusted desktop/trusted desktop flow when elevated access is required.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.cancelSudo(interaction) }) {
        Text("Cancel Sudo Request")
    }
}

@Composable
private fun SecretControls(interaction: PendingHermesInteraction, viewModel: HermesMobileViewModel) {
    var secret by remember(interaction.stableKey) { mutableStateOf("") }

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = secret,
        onValueChange = { secret = it },
        label = { Text(interaction.envVar.ifBlank { "Secret value" }) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation()
    )
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = secret.isNotBlank(),
        onClick = { viewModel.respondSecret(interaction, secret) }
    ) {
        Text("Send Secret")
    }
    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.respondSecret(interaction, "") }) {
        Text("Cancel Secret Request")
    }
}

@Composable
private fun WorkbenchLauncherPanel(state: HermesUiState, viewModel: HermesMobileViewModel) {
    Panel(title = "Hermes Workbench", icon = Icons.Outlined.Build) {
        Text(
            text = "Launch Android build, web preview, repo coding, test/build, or browser QA work through desktop host's Hermes Agent.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        InfoLine("Gateway", state.gatewayState.name.lowercase())
        InfoLine("Workspace", state.workbenchCwd.ifBlank { "/home/user/projects" })
        WorkbenchMode.entries.forEach { mode ->
            val selected = state.workbenchMode == mode
            val onClick = { viewModel.setWorkbenchMode(mode) }
            if (selected) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
                    Text(mode.label)
                }
            } else {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
                    Text(mode.label)
                }
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.workbenchCwd,
            onValueChange = viewModel::setWorkbenchCwd,
            label = { Text("desktop host workspace cwd") },
            singleLine = true
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.workbenchGoal,
            onValueChange = viewModel::setWorkbenchGoal,
            label = { Text("What should Hermes build, code, or test?") },
            minLines = 3,
            maxLines = 6
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = viewModel::launchWorkbenchPrompt,
            enabled = state.workbenchGoal.isNotBlank() && !state.busy
        ) {
            Icon(Icons.Outlined.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Launch In Hermes")
        }
    }
}

@Composable
private fun BrowserPreviewPanel(
    state: HermesUiState,
    viewModel: HermesMobileViewModel,
    previewTargets: List<String>
) {
    var draftUrl by remember(state.workbenchBrowserUrl) { mutableStateOf(state.workbenchBrowserUrl) }

    Panel(title = "Android Browser", icon = Icons.Outlined.Info) {
        Text(
            text = "Use this for tailnet dashboards, local dev servers, and Hermes preview links while the agent builds on desktop host.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = draftUrl,
            onValueChange = { draftUrl = it },
            label = { Text("Preview URL") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.setWorkbenchBrowserUrl(draftUrl) }) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Load")
            }
            OutlinedButton(onClick = viewModel::openFirstPreviewTarget) {
                Text("First Chat Preview")
            }
        }
        if (previewTargets.isNotEmpty()) {
            Text("Chat previews", style = MaterialTheme.typography.labelLarge)
            previewTargets.take(4).forEach { target ->
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.openPreviewTarget(target) }) {
                    Text(previewName(target), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        MobileBrowser(url = state.workbenchBrowserUrl)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkbenchEvidencePanel(state: HermesUiState, evidence: WorkbenchEvidence, viewModel: HermesMobileViewModel) {
    var selectedItem by remember { mutableStateOf<WorkbenchEvidenceItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Panel(title = "Agent Evidence", icon = Icons.Outlined.Storage) {
        if (evidence.isEmpty) {
            EmptyState(
                "No work evidence yet",
                "Hermes output will surface previews, files, and commands here."
            )
            if (state.debugBuild) {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.loadDemoWorkbenchEvidence() }) {
                    Text("Load Demo Evidence")
                }
            }
            return@Panel
        }

        if (state.debugBuild) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (state.debugWorkbenchEvidence.isBlank()) {
                        viewModel.loadDemoWorkbenchEvidence()
                    } else {
                        viewModel.clearDemoWorkbenchEvidence()
                    }
                }
            ) {
                Text(if (state.debugWorkbenchEvidence.isBlank()) "Load Demo Evidence" else "Clear Demo Evidence")
            }
        }

        InfoLine("Sources", evidence.sourceSummary())

        if (evidence.previews.isNotEmpty()) {
            Text("Previews", style = MaterialTheme.typography.labelLarge)
            evidence.items
                .filter { it.kind == WorkbenchEvidenceKind.Preview }
                .take(4)
                .forEach { item ->
                    EvidenceLine(item) { selectedItem = it }
            }
        }

        if (evidence.filePaths.isNotEmpty()) {
            Text("Files", style = MaterialTheme.typography.labelLarge)
            evidence.items
                .filter { it.kind == WorkbenchEvidenceKind.File }
                .take(6)
                .forEach { item ->
                    EvidenceLine(item) { selectedItem = it }
            }
        }

        if (evidence.commands.isNotEmpty()) {
            Text("Commands", style = MaterialTheme.typography.labelLarge)
            evidence.items
                .filter { it.kind == WorkbenchEvidenceKind.Command }
                .take(6)
                .forEach { item ->
                    EvidenceLine(item) { selectedItem = it }
            }
        }
    }

    selectedItem?.let { item ->
        ModalBottomSheet(onDismissRequest = { selectedItem = null }, sheetState = sheetState) {
            EvidenceActionSheet(
                item = item,
                viewModel = viewModel,
                onClose = { selectedItem = null }
            )
        }
    }
}

@Composable
private fun EvidenceLine(item: WorkbenchEvidenceItem, onClick: (WorkbenchEvidenceItem) -> Unit) {
    val handoff = item.toWorkbenchHandoff()
    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { onClick(item) }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.label, modifier = Modifier.width(54.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.value,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    item.sourceDescription,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "Handoff: ${handoff.risk.label}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun EvidenceActionSheet(
    item: WorkbenchEvidenceItem,
    viewModel: HermesMobileViewModel,
    onClose: () -> Unit
) {
    val handoff = item.toWorkbenchHandoff()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(handoff.title, style = MaterialTheme.typography.titleMedium)
        Text(
            item.value,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        InfoLine("Source", item.sourceDescription)
        InfoLine("Risk", handoff.risk.label)
        InfoLine("Next", handoff.nextAction)
        Text("desktop host handoff prompt", style = MaterialTheme.typography.labelLarge)
        Text(
            handoff.prompt,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        if (item.kind == WorkbenchEvidenceKind.Preview) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.openPreviewTarget(item.value)
                    onClose()
                }
            ) {
                Text("Open Preview")
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.draftEvidenceFollowUp(item)
                onClose()
            }
        ) {
            Text("Draft Handoff Prompt")
        }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onClose) {
            Text("Close")
        }
    }
}

private fun WorkbenchEvidence.sourceSummary(): String {
    return items
        .groupingBy { it.source.label }
        .eachCount()
        .entries
        .joinToString(" / ") { "${it.key}: ${it.value}" }
        .ifBlank { "none" }
}

private fun WorkbenchEvidence.compactCountSummary(): String {
    return buildList {
        if (previews.isNotEmpty()) add("Previews ${previews.size}")
        if (filePaths.isNotEmpty()) add("Files ${filePaths.size}")
        if (commands.isNotEmpty()) add("Runs ${commands.size}")
        val sources = sourceSummary()
        if (sources != "none") add(sources)
    }.joinToString(" / ")
}

@Composable
private fun ThemePanel(state: HermesUiState, viewModel: HermesMobileViewModel) {
    Panel(title = "Hermes Themes", icon = Icons.Outlined.Settings) {
        Text(
            text = "Desktop skin presets mapped to native Android Material colors.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        HermesThemePresets.forEach { preset ->
            val selected = preset.name == state.themeName
            val onClick = { viewModel.setThemeName(preset.name) }
            if (selected) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
                    Text("${preset.label} / ${preset.description}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
                    Text("${preset.label} / ${preset.description}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun DesktopParityPanel(state: HermesUiState) {
    Panel(title = "Desktop Parity", icon = Icons.Outlined.List) {
        Text(
            text = "Hermes Desktop routes mapped into mobile-native surfaces.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        state.parityItems.forEach { item ->
            ParityRow(item)
        }
    }
}

@Composable
private fun ManageScreen(state: HermesUiState, viewModel: HermesMobileViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::refreshSurfaces) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Refresh")
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ModelProfilePanel(state, viewModel, compact = false) }
            item {
                Panel(title = "Admin Actions", icon = Icons.Outlined.Warning) {
                    Text(
                        text = "Env reveal, credential edits, gateway restart, hooks, imports, deletes, and updates are locked in V1.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(state.surfaces, key = { it.label }) { surface ->
                SurfaceRow(surface)
            }
        }
    }
}

@Composable
private fun DiagnosticsScreen(state: HermesUiState, viewModel: HermesMobileViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StatusPanel(state) }
        item { DiagnosticsPanel(state, viewModel) }
        item {
            Panel(title = "Auth", icon = Icons.Outlined.Key) {
                Text("Identity: ${state.identity?.displayName ?: "not logged in"}")
                Text("Providers: ${state.providers.joinToString { it.name.ifBlank { it.displayName } }.ifBlank { "none" }}")
                Text("Auth required: ${state.status?.authRequired ?: false}")
            }
        }
    }
}

@Composable
private fun StatusPanel(state: HermesUiState) {
    Panel(title = "Backend", icon = Icons.Outlined.Storage) {
        val status = state.status
        if (status == null) {
            Text("Not checked", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            InfoLine("Version", "${status.version} / ${status.releaseDate}")
            InfoLine("Gateway", "${status.gatewayRunning} / ${status.gatewayState}")
            InfoLine("Auth", "${status.authRequired} / ${status.authProviders.joinToString()}")
            InfoLine("Home", status.hermesHome)
        }
    }
}

@Composable
private fun DiagnosticsPanel(state: HermesUiState, viewModel: HermesMobileViewModel) {
    Panel(title = "Phone Route", icon = Icons.Outlined.Info) {
        val route = state.routeSnapshot
        if (route == null) {
            Text("No route snapshot")
        } else {
            InfoLine("Device", route.deviceLabel)
            InfoLine("Android", route.androidLabel)
            InfoLine("Transport", route.activeTransports.joinToString().ifBlank { "unknown" })
            InfoLine("VPN", route.vpnActive.toString())
            InfoLine("Validated", route.networkValidated.toString())
            InfoLine("Checked", route.checkedAt)
        }
        OutlinedButton(onClick = viewModel::refreshRoutes) {
            Icon(Icons.Outlined.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Refresh Route")
        }
    }
}

@Composable
private fun MessageBubble(line: ChatLine) {
    val isUser = line.role == "user"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = when {
            isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            line.role == "system" -> MaterialTheme.colorScheme.error.copy(alpha = 0.13f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = line.role.uppercase() + if (line.pending) " / streaming" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(line.text.ifBlank { "..." }, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SessionRow(session: SessionInfo, selected: Boolean, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(session.displayTitle, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = listOfNotNull(session.profile ?: "default", session.model, session.messageCount?.let { "$it messages" })
                    .joinToString(" / "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            session.preview?.takeIf { it.isNotBlank() }?.let {
                Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SurfaceRow(surface: SurfaceSnapshot) {
    val ok = surface.state == SurfaceState.Ready
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (ok) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = if (ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(surface.label, fontWeight = FontWeight.SemiBold)
                    Text(surface.endpoint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = surface.preview,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ParityRow(item: DesktopParityItem) {
    val tone = when (item.status) {
        ParityStatus.Ready -> MaterialTheme.colorScheme.tertiary
        ParityStatus.InProgress -> MaterialTheme.colorScheme.primary
        ParityStatus.Planned -> MaterialTheme.colorScheme.onSurfaceVariant
        ParityStatus.Locked -> MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = tone, content = {})
                Text(item.desktopSurface, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text(item.status.label, color = tone, style = MaterialTheme.typography.labelMedium)
            }
            InfoLine("Desktop", item.desktopRoute)
            InfoLine("Mobile", item.mobileSurface)
            Text(item.notes, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MobileBrowser(url: String) {
    val target = url.trim()
    if (!target.startsWith("http://") && !target.startsWith("https://")) {
        EmptyState(
            "No web preview loaded",
            "Load a tailnet or dev-server URL. File paths from Desktop preview markers need a served URL before Android can render them."
        )
        return
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember(target) { mutableStateOf("") }
    var currentUrl by remember(target) { mutableStateOf(target) }
    var loadingProgress by remember(target) { mutableStateOf(0) }
    var isLoading by remember(target) { mutableStateOf(false) }
    var canGoBack by remember(target) { mutableStateOf(false) }
    var canGoForward by remember(target) { mutableStateOf(false) }
    var lastError by remember(target) { mutableStateOf("") }

    fun updateNavigationState(view: WebView?) {
        canGoBack = view?.canGoBack() == true
        canGoForward = view?.canGoForward() == true
        currentUrl = view?.url ?: currentUrl.ifBlank { target }
        pageTitle = view?.title.orEmpty().ifBlank { pageTitle }
    }

    InfoLine("Page", pageTitle.ifBlank { "not loaded" })
    InfoLine("URL", currentUrl.ifBlank { target })
    InfoLine(
        "Status",
        when {
            lastError.isNotBlank() -> lastError
            isLoading -> "Loading $loadingProgress%"
            loadingProgress >= 100 -> "Loaded"
            else -> "Ready"
        }
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            modifier = Modifier.weight(1f),
            enabled = canGoBack,
            onClick = {
                webViewRef?.goBack()
                updateNavigationState(webViewRef)
            }
        ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Back")
        }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            enabled = canGoForward,
            onClick = {
                webViewRef?.goForward()
                updateNavigationState(webViewRef)
            }
        ) {
            Icon(Icons.Outlined.ArrowForward, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Forward")
        }
    }
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            lastError = ""
            webViewRef?.reload()
        }
    ) {
        Icon(Icons.Outlined.Refresh, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Reload")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewRef = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            loadingProgress = newProgress
                            isLoading = newProgress in 1..99
                            updateNavigationState(view)
                        }

                        override fun onReceivedTitle(view: WebView, title: String?) {
                            pageTitle = title.orEmpty()
                            updateNavigationState(view)
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            loadingProgress = 0
                            lastError = ""
                            currentUrl = url ?: target
                            updateNavigationState(view)
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            isLoading = false
                            loadingProgress = 100
                            currentUrl = url ?: view.url ?: target
                            updateNavigationState(view)
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError
                        ) {
                            if (request.isForMainFrame) {
                                isLoading = false
                                lastError = error.description?.toString()?.take(120).orEmpty().ifBlank { "Page error" }
                                updateNavigationState(view)
                            }
                        }
                    }
                    loadUrl(target)
                }
            },
            update = { webView ->
                webViewRef = webView
                if (webView.url != target) {
                    lastError = ""
                    webView.loadUrl(target)
                }
                updateNavigationState(webView)
            }
        )
    }
}

@Composable
private fun ToolEventList(state: HermesUiState, viewModel: HermesMobileViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Tool Activity", style = MaterialTheme.typography.titleMedium)
        InteractionQueuePanel(state.pendingInteractions, viewModel)
        if (state.events.isEmpty()) {
            EmptyState("No gateway events", "Tool, approval, clarify, sudo, and secret events appear here.")
        } else {
            state.events.take(40).forEach { event ->
                SurfaceRow(
                    SurfaceSnapshot(
                        label = event.type,
                        endpoint = event.sessionId ?: "gateway",
                        state = if (event.important) SurfaceState.Ready else SurfaceState.Ready,
                        preview = event.text.ifBlank { event.at }
                    )
                )
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun Panel(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            content()
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.width(92.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatusBanner(text: String?, error: Boolean) {
    if (text.isNullOrBlank()) return
    Surface(
        color = if (error) MaterialTheme.colorScheme.error.copy(alpha = 0.16f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (error) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EmptyState(title: String, detail: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConnectionPill(modifier: Modifier = Modifier, state: GatewayConnectionState) {
    val (label, color) = when (state) {
        GatewayConnectionState.Open -> "Gateway open" to MaterialTheme.colorScheme.tertiary
        GatewayConnectionState.Connecting -> "Connecting" to MaterialTheme.colorScheme.primary
        GatewayConnectionState.Error -> "Gateway error" to MaterialTheme.colorScheme.error
        GatewayConnectionState.Closed -> "Gateway closed" to MaterialTheme.colorScheme.onSurfaceVariant
        GatewayConnectionState.Idle -> "Gateway idle" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier.height(36.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(modifier = Modifier.size(7.dp), shape = CircleShape, color = color, content = {})
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                color = if (state == GatewayConnectionState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CompactActionPill(modifier: Modifier = Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(36.dp),
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(5.dp))
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CompactTextPill(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.height(34.dp),
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun IconButtonLike(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = label)
    }
}

private fun HermesTab.icon(): ImageVector {
    return when (this) {
        HermesTab.Chat -> Icons.Outlined.Chat
        HermesTab.Sessions -> Icons.Outlined.List
        HermesTab.Workbench -> Icons.Outlined.Build
        HermesTab.Manage -> Icons.Outlined.Settings
        HermesTab.Diagnostics -> Icons.Outlined.Info
    }
}

private fun HermesTab.navLabel(): String {
    return when (this) {
        HermesTab.Chat -> "Chat"
        HermesTab.Sessions -> "Sessions"
        HermesTab.Workbench -> "Work"
        HermesTab.Manage -> "Manage"
        HermesTab.Diagnostics -> "Diag"
    }
}

private fun ModelOptionProvider.displayLabel(): String {
    val count = totalModels ?: models.size
    val auth = when (authenticated) {
        false -> "locked"
        true -> "ready"
        null -> "available"
    }
    return "${name.ifBlank { slug }} / $count / $auth"
}
