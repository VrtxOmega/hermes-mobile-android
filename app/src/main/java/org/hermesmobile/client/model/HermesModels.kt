package org.hermesmobile.client.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class HermesStatus(
    val version: String = "",
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("hermes_home") val hermesHome: String = "",
    @SerialName("gateway_running") val gatewayRunning: Boolean = false,
    @SerialName("gateway_state") val gatewayState: String = "",
    @SerialName("auth_required") val authRequired: Boolean = false,
    @SerialName("auth_providers") val authProviders: List<String> = emptyList(),
    @SerialName("gateway_platforms") val gatewayPlatforms: JsonElement? = null
)

@Serializable
data class AuthProvidersResponse(
    val providers: List<AuthProvider> = emptyList()
)

@Serializable
data class AuthProvider(
    val name: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("supports_password") val supportsPassword: Boolean = false
)

@Serializable
data class PasswordLoginRequest(
    val provider: String,
    val username: String,
    val password: String,
    val next: String = "/"
)

@Serializable
data class LoginResponse(
    val ok: Boolean = false,
    val next: String = "/"
)

@Serializable
data class AuthMeResponse(
    @SerialName("user_id") val userId: String = "",
    val email: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("org_id") val orgId: String? = null,
    val provider: String = "",
    @SerialName("expires_at") val expiresAt: Long = 0
)

@Serializable
data class WsTicketResponse(
    val ticket: String = "",
    @SerialName("ttl_seconds") val ttlSeconds: Int = 0
)

@Serializable
data class PaginatedSessions(
    val sessions: List<SessionInfo> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    @SerialName("profile_totals") val profileTotals: Map<String, Int> = emptyMap()
)

@Serializable
data class SessionInfo(
    val id: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("stored_session_id") val storedSessionId: String? = null,
    @SerialName("_lineage_root_id") val lineageRootId: String? = null,
    val title: String? = null,
    val preview: String? = null,
    val profile: String? = null,
    val provider: String? = null,
    val model: String? = null,
    @SerialName("message_count") val messageCount: Int? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val archived: Boolean = false
) {
    val displayId: String
        get() = storedSessionId ?: id ?: sessionId ?: lineageRootId ?: ""

    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() }
            ?: preview?.lineSequence()?.firstOrNull()?.take(72)
            ?: "Untitled session"
}

@Serializable
data class SessionMessagesResponse(
    val messages: List<HermesMessage> = emptyList(),
    val session: SessionInfo? = null
)

@Serializable
data class HermesMessage(
    val id: String? = null,
    val role: String = "",
    val content: JsonElement? = null,
    val text: String? = null,
    val parts: List<JsonElement> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null
) {
    fun displayText(): String {
        text?.takeIf { it.isNotBlank() }?.let { return it }
        content?.extractText()?.takeIf { it.isNotBlank() }?.let { return it }
        return parts.joinToString("\n") { it.extractText() }.trim()
    }
}

@Serializable
data class SessionCreateResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("stored_session_id") val storedSessionId: String? = null,
    val title: String? = null,
    val cwd: String? = null
)

@Serializable
data class SessionResumeResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("stored_session_id") val storedSessionId: String? = null,
    val messages: List<HermesMessage> = emptyList()
)

@Serializable
data class ProfilesResponse(
    val profiles: List<ProfileInfo> = emptyList()
)

@Serializable
data class ProfileInfo(
    val name: String = "",
    val path: String = "",
    @SerialName("is_default") val isDefault: Boolean = false,
    val provider: String? = null,
    val model: String? = null,
    @SerialName("skill_count") val skillCount: Int = 0,
    @SerialName("has_env") val hasEnv: Boolean = false
)

@Serializable
data class SkillsResponse(
    val skills: List<SkillInfo> = emptyList()
)

@Serializable
data class SkillInfo(
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val enabled: Boolean = false
)

@Serializable
data class CronJobsResponse(
    val jobs: List<CronJob> = emptyList()
)

@Serializable
data class CronJob(
    val id: String = "",
    val name: String? = null,
    val prompt: String? = null,
    val state: String? = null,
    val enabled: Boolean? = null,
    @SerialName("next_run_at") val nextRunAt: String? = null,
    @SerialName("last_run_at") val lastRunAt: String? = null,
    @SerialName("schedule_display") val scheduleDisplay: String? = null,
    @SerialName("last_error") val lastError: String? = null
)

@Serializable
data class ModelInfoResponse(
    val provider: String? = null,
    val model: String? = null,
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
    @SerialName("service_tier") val serviceTier: String? = null
)

@Serializable
data class ModelOptionsResponse(
    val provider: String? = null,
    val model: String? = null,
    val providers: List<ModelOptionProvider> = emptyList()
)

@Serializable
data class ModelOptionProvider(
    val name: String = "",
    val slug: String = "",
    val models: List<String> = emptyList(),
    @SerialName("total_models") val totalModels: Int? = null,
    val warning: String? = null,
    val authenticated: Boolean? = null,
    @SerialName("auth_type") val authType: String? = null,
    @SerialName("key_env") val keyEnv: String? = null,
    @SerialName("is_user_defined") val isUserDefined: Boolean? = null,
    @SerialName("free_tier") val freeTier: Boolean? = null,
    @SerialName("unavailable_models") val unavailableModels: List<String> = emptyList()
)

@Serializable
data class ModelAssignmentRequest(
    val scope: String = "main",
    val provider: String,
    val model: String,
    @SerialName("base_url") val baseUrl: String? = null,
    val task: String? = null
)

@Serializable
data class ModelAssignmentResponse(
    val ok: Boolean? = null,
    val provider: String = "",
    val model: String = "",
    @SerialName("base_url") val baseUrl: String? = null
)

@Serializable
data class MemoryResponse(
    @SerialName("memory_provider") val memoryProvider: String? = null,
    @SerialName("context_engine") val contextEngine: String? = null,
    val configured: Boolean? = null,
    val enabled: Boolean? = null
)

@Serializable
data class RpcError(
    val code: Int? = null,
    val message: String = ""
)

@Serializable
data class RpcFrame(
    val id: JsonElement? = null,
    val method: String? = null,
    val params: GatewayEvent? = null,
    val result: JsonElement? = null,
    val error: RpcError? = null
)

@Serializable
data class GatewayEvent(
    val type: String = "",
    @SerialName("session_id") val sessionId: String? = null,
    val payload: JsonElement? = null
) {
    fun text(): String = payload.extractText()
}

fun JsonElement?.extractText(): String {
    return when (this) {
        null, JsonNull -> ""
        is JsonPrimitive -> contentOrNull.orEmpty()
        is JsonArray -> jsonArray.joinToString("\n") { it.extractText() }.trim()
        is JsonObject -> {
            val obj = jsonObject
            val direct = listOf("text", "delta", "message", "content", "preview", "summary", "error")
                .firstNotNullOfOrNull { key -> obj[key]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) }
            direct ?: obj.entries.joinToString("\n") { (key, value) ->
                val text = value.extractText()
                if (text.isBlank()) "" else "$key: $text"
            }.trim()
        }
    }
}
