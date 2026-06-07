package org.hermesmobile.client.data

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.hermesmobile.client.model.GatewayEvent
import org.hermesmobile.client.model.RpcFrame
import org.hermesmobile.client.model.SessionCreateResponse
import org.hermesmobile.client.model.SessionResumeResponse

class HermesGatewayWsClient(private val restClient: HermesRestClient) {
    private val nextId = AtomicInteger(0)
    private val pending = ConcurrentHashMap<String, CompletableDeferred<JsonElement>>()
    private var socket: WebSocket? = null
    private var eventLog: (String) -> Unit = {}

    suspend fun connect(
        onState: (GatewayConnectionState) -> Unit,
        onEvent: (GatewayEvent) -> Unit,
        onLog: (String) -> Unit
    ) {
        close()
        eventLog = onLog
        onState(GatewayConnectionState.Connecting)
        eventLog("ws.ticket.request")
        val ticket = restClient.wsTicket().ticket
        val opened = CompletableDeferred<Unit>()
        val request = Request.Builder()
            .url(restClient.wsUrl(ticket))
            .header("Origin", restClient.currentBaseUrl())
            .build()

        socket = restClient.http.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    onState(GatewayConnectionState.Open)
                    eventLog("ws.open code=${response.code}")
                    opened.complete(Unit)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleFrame(text, onEvent, onLog)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    onState(GatewayConnectionState.Closed)
                    eventLog("ws.closed code=$code reason=${reason.safeGatewayLog()}")
                    rejectAll(IllegalStateException("WebSocket closed: $code $reason"))
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onState(GatewayConnectionState.Error)
                    eventLog("ws.failure http=${response?.code ?: "none"} error=${t.safeGatewayLog()}")
                    if (!opened.isCompleted) opened.completeExceptionally(t)
                    rejectAll(t)
                }
            }
        )

        withTimeout(10_000) { opened.await() }
    }

    fun close() {
        socket?.close(1000, "Hermes Mobile closing")
        socket = null
        rejectAll(IllegalStateException("WebSocket closed"))
    }

    suspend fun createSession(cols: Int = 96, cwd: String? = null, profile: String? = null): SessionCreateResponse {
        val result = request(
            "session.create",
            buildJsonObject {
                put("cols", cols)
                cwd?.takeIf { it.isNotBlank() }?.let { put("cwd", it) }
                profile?.takeIf { it.isNotBlank() }?.let { put("profile", it) }
            }
        )
        return restClient.json.decodeFromJsonElement(SessionCreateResponse.serializer(), result)
    }

    suspend fun resumeSession(storedSessionId: String, profile: String?): SessionResumeResponse {
        val params = buildJsonObject {
            put("session_id", storedSessionId)
            put("cols", 96)
            profile?.takeIf { it.isNotBlank() }?.let { put("profile", it) }
        }
        val result = request("session.resume", params)
        return restClient.json.decodeFromJsonElement(SessionResumeResponse.serializer(), result)
    }

    suspend fun submitPrompt(sessionId: String, text: String) {
        request(
            "prompt.submit",
            buildJsonObject {
                put("session_id", sessionId)
                put("text", text)
            },
            timeoutMs = 180_000
        )
    }

    suspend fun respondApproval(sessionId: String, choice: String) {
        request(
            "approval.respond",
            buildJsonObject {
                put("session_id", sessionId)
                put("choice", choice)
            },
            timeoutMs = 30_000
        )
    }

    suspend fun respondClarify(requestId: String, answer: String) {
        request(
            "clarify.respond",
            buildJsonObject {
                put("request_id", requestId)
                put("answer", answer)
            },
            timeoutMs = 30_000
        )
    }

    suspend fun respondSudo(requestId: String, password: String = "") {
        request(
            "sudo.respond",
            buildJsonObject {
                put("request_id", requestId)
                put("password", password)
            },
            timeoutMs = 30_000
        )
    }

    suspend fun respondSecret(requestId: String, value: String) {
        request(
            "secret.respond",
            buildJsonObject {
                put("request_id", requestId)
                put("value", value)
            },
            timeoutMs = 30_000
        )
    }

    suspend fun request(method: String, params: JsonElement, timeoutMs: Long = 120_000): JsonElement {
        val webSocket = socket ?: error("Gateway is not connected")
        val id = "m${nextId.incrementAndGet()}"
        val deferred = CompletableDeferred<JsonElement>()
        pending[id] = deferred
        val frame = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", id)
            put("method", method)
            put("params", params)
        }

        eventLog("$method.request id=$id")
        val sent = webSocket.send(restClient.json.encodeToString(frame))
        if (!sent) {
            pending.remove(id)
            eventLog("$method.send_failed id=$id")
            error("Gateway send failed")
        }

        return runCatching {
            withTimeout(timeoutMs) { deferred.await() }
        }.onSuccess {
            eventLog("$method.ok id=$id")
        }.onFailure { error ->
            pending.remove(id)
            eventLog("$method.failed id=$id error=${error.safeGatewayLog()}")
        }.getOrThrow()
    }

    private fun handleFrame(
        text: String,
        onEvent: (GatewayEvent) -> Unit,
        onLog: (String) -> Unit
    ) {
        val frame = try {
            restClient.json.decodeFromString(RpcFrame.serializer(), text)
        } catch (error: SerializationException) {
            onLog("frame.unparsed chars=${text.length}")
            return
        }

        val id = frame.id?.jsonPrimitive?.content
        if (id != null && (frame.result != null || frame.error != null)) {
            val deferred = pending.remove(id)
            if (frame.error != null) {
                deferred?.completeExceptionally(IllegalStateException(frame.error.message))
            } else {
                deferred?.complete(frame.result ?: JsonNull)
            }
            return
        }

        if (frame.method == "event" && frame.params != null) {
            onEvent(frame.params)
        } else {
            onLog("frame.unhandled method=${frame.method ?: "unknown"} id=${id ?: "none"}")
        }
    }

    private fun rejectAll(error: Throwable) {
        pending.values.forEach { it.completeExceptionally(error) }
        pending.clear()
    }

    private fun Throwable.safeGatewayLog(): String {
        return (message ?: javaClass.simpleName).safeGatewayLog()
    }

    private fun String.safeGatewayLog(): String {
        return replace(Regex("\\s+"), " ").trim().ifBlank { "none" }.take(160)
    }
}

enum class GatewayConnectionState {
    Idle,
    Connecting,
    Open,
    Closed,
    Error
}
