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
    private val md5 = MessageDigest.getInstance("MD5")
    private val httpClient = OkHttpClient()
    val json = Json { ignoreUnknownKeys = true }

    // AUTHENTICATION

    private fun generateSalt(length: Int = 8): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '-' + '_'

        return buildString(length) {
            repeat(length) {
                append(charPool[rng.nextInt(charPool.size)])
            }
        }
    }

    private fun generateToken(password: String, salt: String): String {
        return md5.digest((password + salt).toByteArray(UTF_8))
            .joinToString("") {
                "%02x".format(it)
            }
    }

    private fun appendAuthParameters(
        parameters: Map<String, String> = mapOf(),
        credentials: UserData = getCurrentUser(),
    ): Map<String, String> {
        checkAuth(credentials)

        credentials.apiKey?.let {
            return parameters + mapOf(
                "apiKey" to it,
            )
        }

        credentials.password!!.let {
            val salt = generateSalt()
            val token = generateToken(it, salt)
            return parameters + mapOf(
                "u" to credentials.username,
                "t" to token,
                "s" to salt,
            )
        }
    }

    // REQUESTS

    fun get(
        baseUrl: String,
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
    ): Request {
        return Request.Builder()
            .url(
                baseUrl.toHttpUrl().newBuilder().apply {
                    addPathSegment("rest")
                    addPathSegment(endpoint)

                    (COMMON_PARAMETERS + parameters).forEach { addQueryParameter(it.key, it.value) }
                }.build(),
            )
            .headers(DEFAULT_HEADERS)
            .cacheControl(DEFAULT_CACHE_CONTROL)
            .build()
    }

    fun post(
        baseUrl: String,
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
    ): Request {
        return Request.Builder()
            .url(
                baseUrl.toHttpUrl().newBuilder().apply {
                    addPathSegment("rest")
                    addPathSegment(endpoint)
                }.build(),
            )
            .post(
                FormBody.Builder().apply {
                    (COMMON_PARAMETERS + parameters).forEach { add(it.key, it.value) }
                }.build(),
            )
            .headers(DEFAULT_HEADERS)
            .cacheControl(DEFAULT_CACHE_CONTROL)
            .build()
    }

    fun authenticatedRequest(
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
        needsGet: Boolean = false,
        credentials: UserData = getCurrentUser(),
    ): Request {
        val params: Map<String, String> = appendAuthParameters(parameters, credentials)
        val server: ServerData = credentials.server!!
        val supportsPost: Boolean =
            server.extensions?.contains(ServerData.Extension.FormPost) ?: false

        return if (supportsPost && !needsGet && !SettingsSession.forceGetRequests) {
            post(baseUrl = server.url, endpoint = endpoint, parameters = params)
        } else {
            get(baseUrl = server.url, endpoint = endpoint, parameters = params)
        }
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

    fun throwOnError(error: ErrorDto?) {
        throw error?.let {
            Exception("Error " + it.code + ": " + (it.message ?: "Unknown error"))
        } ?: Exception("Unknown error")
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> Response.parseAs(): T {
        return json.decodeFromStream(body.byteStream())
    }
}