package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.NetworkRequest
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extension.dto.ErrorDto
import dev.brahmkshatriya.echo.extension.dto.LoginDto
import dev.brahmkshatriya.echo.extension.dto.TokenInfoDto
import kotlinx.serialization.ExperimentalSerializationApi
//import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
//import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
//import okhttp3.RequestBody
import okhttp3.Response
import java.net.UnknownHostException

//import okhttp3.RequestBody.Companion.toRequestBody as asRequestBody

@OptIn(ExperimentalSerializationApi::class)
class OpenSubsonicApi {
    companion object {
        private const val API_VERSION: String = "1"
        private const val CLIENT_NAME: String = "Echo nightly"
        private const val RESPONSE_FORMAT: String = "json"
        private val COMMON_PARAMETERS: Map<String, String> = mapOf(
            "v" to API_VERSION,
            "c" to CLIENT_NAME,
            "f" to RESPONSE_FORMAT,
        )
    }

    private var userData = UserData.EMPTY

    private val json = Json {
        ignoreUnknownKeys = true
        // namingStrategy = KebabCaseToCamelCase
    }

    private val client = OkHttpClient()

    // Login

    suspend fun onPasswordLogin(data: Map<String, String?>): List<User> {
        val u: String = data["username"]!!
        val p: String = data["password"]!!
        val url: String = data["address"]!!

        var s: String = generateSalt()
        var t: String = computeToken(p, s)

        val resp: Response
        try {
            resp = client.get(
                url = url.toHttpUrl().newBuilder().apply {
                    addPathSegments("rest/getUser")

                    addQueryParameter("u", u)
                    addQueryParameter("t", t)
                    addQueryParameter("s", s)
                    addQueryParameter("user", u)
                    COMMON_PARAMETERS.forEach { addQueryParameter(it.key, it.value) }
                }.build()
            )
        } catch (_: UnknownHostException) {
            throw Exception("Invalid server address")
        }
        if (resp.code == 404) {
            throw Exception("Invalid server address")
        }

        val loginData = resp.parseAs<LoginDto>().subsonicResponse
        if (loginData.status != "ok") {
            handleError(loginData.error)
        }

        s = generateSalt()
        t = computeToken(p, s)
        val avatar: ImageHolder = ImageHolder.NetworkRequestImageHolder(
            NetworkRequest(
                url.toHttpUrl().newBuilder().apply {
                    addPathSegments("rest/getAvatar")

                    addQueryParameter("u", u)
                    addQueryParameter("t", t)
                    addQueryParameter("s", s)
                    addQueryParameter("username", u)
                    COMMON_PARAMETERS.forEach { addQueryParameter(it.key, it.value) }
                }.toString()
            ),
            crop = false
        )

        val user = User(
            id = u,
            name = u,
            cover = avatar,
            subtitle = loginData.user?.email,
            extras = mapOf(
                "serverUrl" to url,
                "password" to p,
            ),
        )

        return listOf(user)
    }

    suspend fun onKeyLogin(data: Map<String, String?>): List<User> {
        val k: String = data["apiKey"]!!
        val url: String = data["address"]!!

        val resp: Response
        try {
            resp = client.get(
                url = url.toHttpUrl().newBuilder().apply {
                    addPathSegments("rest/tokenInfo")

                    addQueryParameter("apiKey", k)
                    COMMON_PARAMETERS.forEach { addQueryParameter(it.key, it.value) }
                }.build()
            )
        } catch (_: UnknownHostException) {
            throw Exception("Invalid server address")
        }
        if (resp.code == 404) {
            throw Exception("Invalid server address")
        }

        val respData = resp.parseAs<TokenInfoDto>().subsonicResponse
        if (respData.status != "ok") {
            handleError(respData.error)
        }
        val username = respData.tokenInfo!!.username

        val resp1 = client.get(
            url = url.toHttpUrl().newBuilder().apply {
                addPathSegments("rest/getUser")

                addQueryParameter("apiKey", k)
                addQueryParameter("user", username)
                COMMON_PARAMETERS.forEach { addQueryParameter(it.key, it.value) }
            }.build()
        )
        val loginData = resp1.parseAs<LoginDto>().subsonicResponse
        if (loginData.status != "ok") {
            handleError(loginData.error)
        }

        val avatar: ImageHolder = ImageHolder.NetworkRequestImageHolder(
            NetworkRequest(
                url.toHttpUrl().newBuilder().apply {
                    addPathSegments("rest/getAvatar")

                    addQueryParameter("apiKey", k)
                    addQueryParameter("username", username)
                    COMMON_PARAMETERS.forEach { addQueryParameter(it.key, it.value) }
                }.toString()
            ),
            crop = false
        )

        val user = User(
            id = username,
            name = username,
            cover = avatar,
            subtitle = loginData.user?.email,
            extras = mapOf(
                "serverUrl" to url,
                "apiKey" to k,
            ),
        )

        return listOf(user)
    }

    fun setUser(user: User?) {
        userData = user?.let {
            UserData(
                username = it.name,
                email = it.subtitle,
                avatar = it.cover,
                serverUrl = it.extras["serverUrl"],
                password = it.extras["password"],
                apiKey = it.extras["apiKey"],
            )
        } ?: UserData.EMPTY
    }

    fun getUser(): User? {
        try {
            checkAuth()
        } catch (_: ClientException.LoginRequired) {
            return null
        }

        return User(
            id = userData.username,
            name = userData.username,
            cover = userData.avatar,
            subtitle = userData.email,
            extras = mapOf(
                "password" to userData.password,
                "apiKey" to userData.apiKey,
                "serverUrl" to userData.serverUrl,
            ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
        )
    }

    // Utils

    fun handleError(error: ErrorDto?) {
        when (error?.code) {
            40 -> throw Exception("Invalid credentials")
            41 or 42 -> throw Exception("Login method not supported")
            44 -> throw Exception("Invalid API key")
        }
    }

    fun getUrlBuilder(): HttpUrl.Builder {
        checkAuth()
        return userData.serverUrl!!.toHttpUrl().newBuilder()
    }

    fun checkAuth() {
        if (userData.serverUrl == null || (userData.password == null && userData.apiKey == null)) {
            throw ClientException.LoginRequired()
        }
    }

    suspend fun authenticatedGet(
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
        //headers: Headers = DEFAULT_HEADERS,
        //cache: CacheControl = DEFAULT_CACHE_CONTROL,
    ): Response {
        checkAuth()

        val p: String? = userData.password
        val k: String? = userData.apiKey

        var salt: String? = null
        var token: String? = null
        if (p != null) {
            salt = generateSalt()
            token = computeToken(p, salt)
        }

        val url = getUrlBuilder().apply {
            addPathSegment("rest")
            addPathSegment(endpoint)

            if (p != null) {
                addQueryParameter("u", userData.username)
                addQueryParameter("t", token)
                addQueryParameter("s", salt)
            } else {
                addQueryParameter("apiKey", k)
            }
            (COMMON_PARAMETERS + parameters).forEach { addQueryParameter(it.key, it.value) }
        }.build()

        return client.get(url = url)
    }

    suspend fun authenticatedPost(
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
        //headers: Headers = DEFAULT_HEADERS,
        //cache: CacheControl = DEFAULT_CACHE_CONTROL,
    ): Response {
        checkAuth()

        val p: String? = userData.password
        val k: String? = userData.apiKey

        var salt: String? = null
        var token: String? = null
        if (p != null) {
            salt = generateSalt()
            token = computeToken(p, salt)
        }

        val url = getUrlBuilder().apply {
            addPathSegment("rest")
            addPathSegment(endpoint)
        }.build()

        val body = FormBody.Builder().apply {
            if (p != null) {
                add("u", userData.username)
                add("t", token!!)
                add("s", salt!!)
            } else {
                add("apiKey", k!!)
            }
            (COMMON_PARAMETERS + parameters).forEach { add(it.key, it.value) }
        }.build()

        return client.post(url = url, body = body)
    }

    private inline fun <reified T> Response.parseAs(): T {
        return json.decodeFromStream(body.byteStream())
    }

    /*
    private inline fun <reified T> Response.parseAs(serializer: KSerializer<T>): T {
        return json.decodeFromStream(serializer, body.byteStream())
    }
    */

    /*
    private inline fun <reified T> T.toRequestBody(): RequestBody {
        return json.encodeToString(this).asRequestBody(
            "application/json".toMediaType(),
        )
    }
    */
}

data class UserData(
    val username: String,
    val email: String?,
    val avatar: ImageHolder?,
    val serverUrl: String?,
    val password: String?,
    val apiKey: String?,
) {
    companion object {
        val EMPTY = UserData("", null, null, null, null, null)
    }
}