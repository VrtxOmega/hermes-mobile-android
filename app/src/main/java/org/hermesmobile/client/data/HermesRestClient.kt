package org.hermesmobile.client.data

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.hermesmobile.client.model.AuthMeResponse
import org.hermesmobile.client.model.AuthProvidersResponse
import org.hermesmobile.client.model.CronJobsResponse
import org.hermesmobile.client.model.HermesStatus
import org.hermesmobile.client.model.LoginResponse
import org.hermesmobile.client.model.MemoryResponse
import org.hermesmobile.client.model.ModelAssignmentRequest
import org.hermesmobile.client.model.ModelAssignmentResponse
import org.hermesmobile.client.model.ModelInfoResponse
import org.hermesmobile.client.model.ModelOptionsResponse
import org.hermesmobile.client.model.PaginatedSessions
import org.hermesmobile.client.model.PasswordLoginRequest
import org.hermesmobile.client.model.ProfilesResponse
import org.hermesmobile.client.model.SessionMessagesResponse
import org.hermesmobile.client.model.SkillsResponse
import org.hermesmobile.client.model.WsTicketResponse
import org.hermesmobile.client.runtime.HermesUrl

class HermesApiException(val code: Int, val responseBody: String) :
    IOException("Hermes API HTTP $code${responseBody.takeIf { it.isNotBlank() }?.let { ": ${it.take(180)}" }.orEmpty()}")

class HermesRestClient(
    private val store: HermesAuthStore,
    val cookieJar: HermesCookieJar = HermesCookieJar(store)
) {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    val http: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private var baseUrl: String = HermesUrl.normalizeBaseUrl(store.backendUrl())

    init {
        cookieJar.restoreFor(baseUrl.toHttpUrl())
    }

    fun currentBaseUrl(): String = baseUrl

    fun setBaseUrl(input: String): String {
        val normalized = HermesUrl.normalizeBaseUrl(input)
        baseUrl = normalized
        store.saveBackendUrl(normalized)
        cookieJar.restoreFor(normalized.toHttpUrl())
        return normalized
    }

    fun wsUrl(ticket: String): String = HermesUrl.websocketUrl(baseUrl, ticket = ticket)

    suspend fun status(): HermesStatus = get("/api/status")

    suspend fun providers(): AuthProvidersResponse = get("/api/auth/providers")

    suspend fun login(username: String, password: String, provider: String = "basic"): LoginResponse {
        return post("/auth/password-login", PasswordLoginRequest(provider, username, password))
    }

    suspend fun me(): AuthMeResponse = get("/api/auth/me")

    suspend fun wsTicket(): WsTicketResponse = postEmpty("/api/auth/ws-ticket")

    suspend fun logout() {
        val request = Request.Builder()
            .url(url("/auth/logout"))
            .header("Accept", "application/json")
            .post(ByteArray(0).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        execute(request)
    }

    suspend fun sessions(limit: Int = 40): PaginatedSessions =
        get("/api/profiles/sessions?limit=$limit&offset=0&min_messages=0&archived=exclude&order=recent&profile=all")

    suspend fun sessionMessages(sessionId: String, profile: String?): SessionMessagesResponse {
        val suffix = profile?.takeIf { it.isNotBlank() }?.let { "?profile=${java.net.URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
        return get("/api/sessions/${java.net.URLEncoder.encode(sessionId, "UTF-8")}/messages$suffix")
    }

    suspend fun profiles(): ProfilesResponse = get("/api/profiles")

    suspend fun skills(): SkillsResponse = get("/api/skills")

    suspend fun cronJobs(): CronJobsResponse = get("/api/cron/jobs")

    suspend fun memory(): MemoryResponse = get("/api/memory")

    suspend fun modelInfo(): ModelInfoResponse = get("/api/model/info")

    suspend fun modelOptions(): ModelOptionsResponse = get("/api/model/options")

    suspend fun setMainModel(provider: String, model: String): ModelAssignmentResponse =
        post(
            "/api/model/set",
            ModelAssignmentRequest(
                scope = "main",
                provider = provider,
                model = model
            )
        )

    suspend fun rawGet(path: String): JsonElement = get(path)

    private suspend inline fun <reified T> get(path: String): T {
        val request = Request.Builder()
            .url(url(path))
            .header("Accept", "application/json")
            .get()
            .build()
        return json.decodeFromString(execute(request))
    }

    private suspend inline fun <reified RequestT, reified ResponseT> post(path: String, payload: RequestT): ResponseT {
        val body = json.encodeToString(payload).toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url(path))
            .header("Accept", "application/json")
            .post(body)
            .build()
        return json.decodeFromString(execute(request))
    }

    private suspend inline fun <reified T> postEmpty(path: String): T {
        val request = Request.Builder()
            .url(url(path))
            .header("Accept", "application/json")
            .post(ByteArray(0).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return json.decodeFromString(execute(request))
    }

    private fun url(path: String): String {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "$baseUrl$normalizedPath"
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw HermesApiException(response.code, body)
            }
            body
        }
    }

    fun pretty(element: JsonElement): String = json.encodeToString(JsonElement.serializer(), element)

    fun surfaceSummary(element: JsonElement): String {
        return when (element) {
            is JsonObject -> element.entries.take(8).joinToString("\n") { (key, value) ->
                "$key: ${pretty(value).lineSequence().firstOrNull().orEmpty().take(220)}"
            }
            else -> pretty(element).take(1200)
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
