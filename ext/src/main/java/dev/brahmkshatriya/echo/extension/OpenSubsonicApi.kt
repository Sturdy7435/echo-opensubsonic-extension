package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extension.dto.LoginDto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody as asRequestBody

@OptIn(ExperimentalSerializationApi::class)
class OpenSubsonicApi {
    companion object {
        private const val API_VERSION: String = "1"
        private const val CLIENT_NAME: String = "Echo nightly"
        private const val RESPONSE_FORMAT: String = "json"
    }

    // Create an instance of Json with custom options
    private val json = Json {
        ignoreUnknownKeys = true
        namingStrategy = KebabCaseToCamelCase
    }

    // Initialize the user credentials
    private var userCredentials = UserCredentials.EMPTY

    // Create a simple instance for okhttp3
    private val client = OkHttpClient()

    // Login

    suspend fun onPasswordLogin(data: Map<String, String?>): List<User> {
        val loginResp = authenticatedGet("getUser", mapOf("user" to data["user"].toString()))
        if (loginResp.code == 401) {
            throw Exception("Invalid credentials")
        }

        val loginData = loginResp.parseAs<LoginDto>()

        val user = User(
            id = loginData.subsonicResponse.user.username,
            name = loginData.subsonicResponse.user.username,
            cover = null,
            subtitle = null,
            extras = mapOf(
                "password" to "",
                "apiKey" to "",
                "serverUrl" to "",
            ),
        )

        return listOf(user)
    }

    fun setUser(user: User?) {
        userCredentials = user?.let {
            UserCredentials(
                username = it.name,
                password = it.extras["password"]!!,
                apiKey = it.extras["apiKey"]!!,
                serverUrl = it.extras["serverUrl"]!!,
                email = it.subtitle ?: ""
            )
        } ?: UserCredentials.EMPTY
    }

    fun getUser(): User? {
        try {
            checkAuth()
        } catch (_: ClientException.LoginRequired) {
            return null
        }

        return User(
            id = userCredentials.username,
            name = userCredentials.username,
            cover = null,
            subtitle = userCredentials.email.ifEmpty { null },
            extras = mapOf(
                "password" to userCredentials.password,
                "apiKey" to userCredentials.apiKey,
                "serverUrl" to userCredentials.serverUrl
            )
        )
    }

    // Utils

    fun getUrlBuilder(): HttpUrl.Builder {
        return userCredentials.serverUrl.toHttpUrl().newBuilder()
    }

    /*
    fun getRestUrlBuilder(): HttpUrl.Builder {
        return getServerUrlBuilder().apply {
            addPathSegment("rest")
        }
    }
    */

    fun checkAuth() {
        if (userCredentials.serverUrl.isEmpty() || (userCredentials.password.isEmpty() && userCredentials.apiKey.isEmpty())) {
            throw ClientException.LoginRequired()
        }
    }

    suspend fun authenticatedGet(
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
        headers: Headers = DEFAULT_HEADERS,
        cache: CacheControl = DEFAULT_CACHE_CONTROL,
    ): Response {
        checkAuth()

        val username: String = userCredentials.username
        val password: String = userCredentials.password
        val salt: String = generateSalt()
        val token: String = computeToken(password, salt)

        val url = getUrlBuilder().apply {
            addPathSegment("rest")
            addPathSegment(endpoint)
            addQueryParameter("u", username)
            addQueryParameter("t", token)
            addQueryParameter("s", salt)
            addQueryParameter("v", API_VERSION)
            addQueryParameter("c", CLIENT_NAME)
            addQueryParameter("f", RESPONSE_FORMAT)
            parameters.forEach { addQueryParameter(it.key, it.value) }
        }.build()

        return client.get(url, headers, cache)
    }

    private inline fun <reified T> Response.parseAs(): T {
        return json.decodeFromStream(body.byteStream())
    }

    private inline fun <reified T> Response.parseAs(serializer: KSerializer<T>): T {
        return json.decodeFromStream(serializer, body.byteStream())
    }

    private inline fun <reified T> T.toRequestBody(): RequestBody {
        return json.encodeToString(this).asRequestBody(
            "application/json".toMediaType(),
        )
    }
}

data class UserCredentials(
    val username: String,
    val password: String,
    val apiKey: String,
    val serverUrl: String,
    val email: String
) {
    companion object {
        val EMPTY = UserCredentials("", "", "", "", "")
    }
}