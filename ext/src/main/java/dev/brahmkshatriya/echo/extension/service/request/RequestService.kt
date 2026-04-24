package dev.brahmkshatriya.echo.extension.service.request

import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.models.NetworkRequest
import dev.brahmkshatriya.echo.extension.clients.login.LoginClientImpl.Companion.checkAuth
import dev.brahmkshatriya.echo.extension.clients.login.LoginClientImpl.Companion.getCurrentUser
import dev.brahmkshatriya.echo.extension.dto.types.ErrorDto
import dev.brahmkshatriya.echo.extension.models.ServerData
import dev.brahmkshatriya.echo.extension.models.UserData
import dev.brahmkshatriya.echo.extension.service.session.SettingsSession
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.text.Charsets.UTF_8

object RequestService {
    private const val API_VERSION: String = "1.16"
    private const val CLIENT_NAME: String = "Echo nightly"
    private const val RESPONSE_FORMAT: String = "json"
    private val COMMON_PARAMETERS: Map<String, String> = mapOf(
        "v" to API_VERSION,
        "c" to CLIENT_NAME,
        "f" to RESPONSE_FORMAT,
    )
    private val DEFAULT_CACHE_CONTROL = CacheControl.Builder().maxAge(10, MINUTES).build()
    private val DEFAULT_HEADERS = Headers.Builder().build()

    private val rng = SecureRandom()
    private val httpClient = OkHttpClient()
    val json = Json { ignoreUnknownKeys = true }

    // CREDENTIALS

    private fun generateSalt(length: Int = 8): String {
        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '-' + '_'

        return buildString(length) {
            repeat(length) {
                val char = charPool[rng.nextInt(charPool.size)]
                append(char)
            }
        }
    }

    private fun computeToken(password: String, salt: String): String {
        val md = MessageDigest.getInstance("MD5")
        val input = (password + salt).toByteArray(UTF_8)

        return md.digest(input).joinToString("") {
            "%02x".format(it)
        }
    }

    private fun appendAuthParameters(
        parameters: Map<String, String> = mapOf(),
        credentials: UserData = getCurrentUser(),
    ): Map<String, String> {
        checkAuth(credentials)

        val k = credentials.apiKey
        if (k != null) {
            return parameters + mapOf(
                "apiKey" to k,
            )
        }

        val salt = generateSalt()
        val token = computeToken(credentials.password!!, salt)
        return parameters + mapOf(
            "u" to credentials.username,
            "t" to token,
            "s" to salt,
        )
    }

    // REQUESTS

    fun get(
        baseUrl: String = getCurrentUser().server?.url ?: "",
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
    ): Request {
        return Request.Builder()
            .url(
                baseUrl.toHttpUrl().newBuilder().apply {
                    addPathSegment("rest")
                    addPathSegment(endpoint)

                    (COMMON_PARAMETERS + parameters).forEach { addQueryParameter(it.key, it.value) }
                }.build()
            )
            .headers(DEFAULT_HEADERS)
            .cacheControl(DEFAULT_CACHE_CONTROL)
            .build()
    }

    fun post(
        baseUrl: String = getCurrentUser().server?.url ?: "",
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
    ): Request {
        return Request.Builder()
            .url(
                baseUrl.toHttpUrl().newBuilder().apply {
                    addPathSegment("rest")
                    addPathSegment(endpoint)
                }.build()
            )
            .post(
                FormBody.Builder().apply {
                    (COMMON_PARAMETERS + parameters).forEach { add(it.key, it.value) }
                }.build()
            )
            .headers(DEFAULT_HEADERS)
            .cacheControl(DEFAULT_CACHE_CONTROL)
            .build()
    }

    fun authenticatedRequest(
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
        credentials: UserData = getCurrentUser(),
    ): Request {
        val p = appendAuthParameters(parameters, credentials)
        val server: ServerData = credentials.server!!

        val supportsPost = server.extensions?.contains(ServerData.Extension.FormPost) ?: false
        if (supportsPost && !SettingsSession.forceGetRequests) {
            return post(baseUrl = server.url, endpoint = endpoint, parameters = p)
        }
        return get(baseUrl = server.url, endpoint = endpoint, parameters = p)
    }

    suspend fun runRequest(
        request: Request,
    ): Response {
        return httpClient.newCall(request).await()
    }

    // UTILS

    fun RequestBody.toByteArray(): ByteArray {
        val buffer = Buffer()
        this.writeTo(buffer)
        return buffer.readByteArray()
    }

    fun Request.toNetworkRequest(): NetworkRequest {
        return NetworkRequest(
            url = url.toString(),
            headers = buildMap {
                headers.forEach { put(it.first, it.second) }
            },
            method = NetworkRequest.Method.valueOf(method),
            body = body?.toByteArray(),
        )
    }

    fun handleError(error: ErrorDto?) {
        if (error == null) {
            throw Exception("Unknown error")
        }
        throw Exception("Error " + error.code + ": " + (error.message ?: "Generic error"))
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> Response.parseAs(): T {
        return json.decodeFromStream(body.byteStream())
    }
}