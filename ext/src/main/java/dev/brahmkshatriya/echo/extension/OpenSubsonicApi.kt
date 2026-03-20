package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetOpenSubsonicExtensionsDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetRandomSongsDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetUserDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.TokenInfoDto
import dev.brahmkshatriya.echo.extension.dto.types.ErrorDto
import kotlinx.serialization.ExperimentalSerializationApi
//import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
//import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
//import okhttp3.RequestBody
//import okhttp3.RequestBody.Companion.toRequestBody as asRequestBody
import okhttp3.Response
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.UnknownHostException
import java.util.EnumSet

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

    //private val ext by lazy { OpenSubsonicExtension() }
    private val client = OkHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
    }

    var userData = UserData.EMPTY

    // Login

    private suspend fun getServerExtensions(url: String): EnumSet<Server.Extension> {
        val resp: Response
        try {
            resp = client.newCall(
                getRequest(
                    url = url.toHttpUrl().newBuilder().apply {
                        addPathSegments("rest/getOpenSubsonicExtensions")
                        COMMON_PARAMETERS.forEach { addQueryParameter(it.key, it.value) }
                    }.build()
                ),
            ).await()
        } catch (e: Exception) {
            when (e) {
                is MalformedURLException,
                is URISyntaxException,
                is UnknownHostException -> throw Exception("Invalid server address")

                else -> throw e
            }
        }
        if (resp.code == 404) {
            throw Exception("Invalid server address")
        }

        val data = resp.parseAs<GetOpenSubsonicExtensionsDto>().subsonicResponse
        if (data.status != "ok") {
            handleError(data.error)
        }
        val extensions: EnumSet<Server.Extension> = EnumSet.noneOf(Server.Extension::class.java)
        data.openSubsonicExtensions!!.forEach {
            Server.Extension.entries.forEach { entry ->
                if (it.name == entry.id) {
                    extensions.add(entry)
                }
            }
        }
        return extensions
    }

    suspend fun onPasswordLogin(data: Map<String, String?>): List<User> {
        val u: String = data["username"]!!
        val p: String = data["password"]!!
        val url: String = data["address"]!!

        val extensions: EnumSet<Server.Extension> = getServerExtensions(url)

        val resp = runRequest(
            endpoint = "getUser",
            parameters = mapOf(
                "username" to u,
            ),
            serverUrl = url,
            serverExtensions = extensions,
            username = u,
            password = p,
        )
        val loginData = resp.parseAs<GetUserDto>().subsonicResponse
        if (loginData.status != "ok") {
            handleError(loginData.error)
        }

        val avatar: ImageHolder = ImageHolder.NetworkRequestImageHolder(
            authenticatedRequest(
                endpoint = "getAvatar",
                parameters = mapOf(
                    "username" to u,
                ),
                serverUrl = url,
                serverExtensions = extensions,
                username = u,
                password = p,
            ).toNetworkRequest(),
            crop = false,
        )

        val user = User(
            id = u,
            name = u,
            cover = avatar,
            subtitle = loginData.user?.email,
            extras = mapOf(
                "password" to p,
                "serverUrl" to url,
                "serverExtensions" to Server.Extension.serialize(extensions)!!,
            ),
        )

        return listOf(user)
    }

    suspend fun onKeyLogin(data: Map<String, String?>): List<User> {
        val k: String = data["apiKey"]!!
        val url: String = data["address"]!!

        val extensions: EnumSet<Server.Extension> = getServerExtensions(url)

        var resp = runRequest(
            endpoint = "tokenInfo",
            serverUrl = url,
            serverExtensions = extensions,
            apiKey = k,
        )
        val respData = resp.parseAs<TokenInfoDto>().subsonicResponse
        if (respData.status != "ok") {
            handleError(respData.error)
        }
        val username = respData.tokenInfo!!.username

        resp = runRequest(
            endpoint = "getUser",
            parameters = mapOf(
                "username" to username,
            ),
            serverUrl = url,
            serverExtensions = extensions,
            apiKey = k,
        )
        val loginData = resp.parseAs<GetUserDto>().subsonicResponse
        if (loginData.status != "ok") {
            handleError(loginData.error)
        }

        val avatar = ImageHolder.NetworkRequestImageHolder(
            authenticatedRequest(
                endpoint = "getAvatar",
                parameters = mapOf(
                    "username" to username,
                ),
                serverUrl = url,
                serverExtensions = extensions,
                apiKey = k,
            ).toNetworkRequest(),
            crop = false,
        )

        val user = User(
            id = username,
            name = username,
            cover = avatar,
            subtitle = loginData.user?.email,
            extras = mapOf(
                "apiKey" to k,
                "serverUrl" to url,
                "serverExtensions" to Server.Extension.serialize(extensions)!!,
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
                server = Server(
                    it.extras["serverUrl"]!!,
                    Server.Extension.deserialize(it.extras["serverExtensions"]),
                ),
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
                "serverUrl" to userData.server?.url,
                "serverExtensions" to Server.Extension.serialize(userData.server?.extensions)
            ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
        )
    }

    // Track

    suspend fun getRandomTracks(): Shelf {
        val resp = runRequest(
            endpoint = "getRandomSongs",
            parameters = mapOf(
                "size" to "20",
            ),
        ).parseAs<GetRandomSongsDto>().subsonicResponse
        if (resp.status != "ok") {
            handleError(resp.error)
        }
        val songs: List<Track> = resp.randomSongs!!.song.map { it.toTrack() }

        return Shelf.Lists.Items(
            id = "randomTracks",
            title = "Random Tracks",
            list = songs,
        )
    }

    fun getTrack(track: Track): Track {
        throw ClientException.NotSupported("track")
    }

    fun getStreamableMedia(streamable: Streamable): Streamable.Media {
        throw ClientException.NotSupported("media streaming")
    }

    // Utils

    fun handleError(error: ErrorDto?) {
        if (error == null) {
            throw Exception("Unknown error")
        }
        when (error.code) {
            40 -> throw Exception("Invalid credentials")
            41 or 42 -> throw Exception("Login method not supported")
            44 -> throw Exception("Invalid API key")
            else -> throw Exception(error.message)
        }
    }

    fun getUrlBuilder(credentials: UserData = userData): HttpUrl.Builder {
        return credentials.server!!.url.toHttpUrl().newBuilder()
    }

    fun checkAuth(credentials: UserData = userData) {
        if (credentials.server == null || (credentials.password == null && credentials.apiKey == null)) {
            throw ClientException.LoginRequired()
        }
    }

    fun authenticatedGet(
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
        credentials: UserData = userData,
        //headers: Headers = DEFAULT_HEADERS,
        //cache: CacheControl = DEFAULT_CACHE_CONTROL,
    ): Request {
        checkAuth(credentials)

        val p: String? = credentials.password
        val k: String? = credentials.apiKey

        var salt: String? = null
        var token: String? = null
        if (p != null) {
            salt = generateSalt()
            token = computeToken(p, salt)
        }

        val url = getUrlBuilder(credentials).apply {
            addPathSegment("rest")
            addPathSegment(endpoint)

            if (p != null) {
                addQueryParameter("u", credentials.username)
                addQueryParameter("t", token)
                addQueryParameter("s", salt)
            } else {
                addQueryParameter("apiKey", k)
            }
            (COMMON_PARAMETERS + parameters).forEach { addQueryParameter(it.key, it.value) }
        }.build()

        return getRequest(url = url)
    }

    fun authenticatedPost(
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
        credentials: UserData = userData,
        //headers: Headers = DEFAULT_HEADERS,
        //cache: CacheControl = DEFAULT_CACHE_CONTROL,
    ): Request {
        checkAuth(credentials)

        val p: String? = credentials.password
        val k: String? = credentials.apiKey

        var salt: String? = null
        var token: String? = null
        if (p != null) {
            salt = generateSalt()
            token = computeToken(p, salt)
        }

        val url = getUrlBuilder(credentials).apply {
            addPathSegment("rest")
            addPathSegment(endpoint)
        }.build()

        val body = FormBody.Builder().apply {
            if (p != null) {
                add("u", credentials.username)
                add("t", token!!)
                add("s", salt!!)
            } else {
                add("apiKey", k!!)
            }
            (COMMON_PARAMETERS + parameters).forEach { add(it.key, it.value) }
        }.build()

        return postRequest(url = url, body = body)
    }

    fun authenticatedRequest(
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
        //headers: Headers = DEFAULT_HEADERS,
        //cache: CacheControl = DEFAULT_CACHE_CONTROL,
    ): Request {
        val supportsPost = userData.server?.extensions?.contains(Server.Extension.FormPost) ?: false
        if (supportsPost /*&& !ext.forceGetRequests*/) {
            return authenticatedPost(endpoint, parameters)
        }

        return authenticatedGet(endpoint, parameters)
    }

    fun authenticatedRequest(
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
        serverUrl: String,
        serverExtensions: EnumSet<Server.Extension>? = null,
        username: String = "",
        password: String? = null,
        apiKey: String? = null,
        //headers: Headers = DEFAULT_HEADERS,
        //cache: CacheControl = DEFAULT_CACHE_CONTROL,
    ): Request {
        val userDataOverride = UserData(
            username = username,
            password = password,
            apiKey = apiKey,
            server = Server(
                url = serverUrl,
                extensions = serverExtensions,
            ),
            email = null,
            avatar = null,
        )

        val supportsPost =
            userDataOverride.server?.extensions?.contains(Server.Extension.FormPost) ?: false
        if (supportsPost /*&& !ext.forceGetRequests*/) {
            return authenticatedPost(endpoint, parameters, userDataOverride)
        }

        return authenticatedGet(endpoint, parameters, userDataOverride)
    }

    suspend fun runRequest(
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
    ): Response {
        return client.newCall(
            authenticatedRequest(
                endpoint = endpoint,
                parameters = parameters,
            )
        ).await()
    }

    suspend fun runRequest(
        endpoint: String,
        parameters: Map<String, String> = mapOf(),
        serverUrl: String,
        serverExtensions: EnumSet<Server.Extension>? = null,
        username: String = "",
        password: String? = null,
        apiKey: String? = null,
    ): Response {
        return client.newCall(
            authenticatedRequest(
                endpoint = endpoint,
                parameters = parameters,
                serverUrl = serverUrl,
                serverExtensions = serverExtensions,
                username = username,
                password = password,
                apiKey = apiKey,
            )
        ).await()
    }

    private inline fun <reified T> Response.parseAs(): T {
        return json.decodeFromStream(body.byteStream())
    }
}

data class Server(
    val url: String,
    val extensions: EnumSet<Extension>?
) {
    enum class Extension(val id: String) {
        ApiKeyAuthentication("apiKeyAuthentication"),
        GetPodcastEpisode("getPodcastEpisode"),
        FormPost("formPost"),
        IndexBasedQueue("indexBasedQueue"),
        SongLyrics("songLyrics"),
        Template("template"),
        TranscodeOffset("transcodeOffset"),
        Transcoding("transcoding");

        companion object {
            fun serialize(extensions: EnumSet<Extension>?): String? {
                if (extensions == null) {
                    return null
                }
                return extensions.joinToString(",") { it.id }
            }

            fun deserialize(s: String?): EnumSet<Extension>? {
                if (s == null) {
                    return null
                }

                val set = EnumSet.noneOf(Extension::class.java)
                if (s.isNotBlank()) {
                    s.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .mapNotNull { name -> runCatching { valueOf(name) }.getOrNull() }
                        .forEach { set.add(it) }
                }
                return set
            }
        }
    }
}

data class UserData(
    val username: String,
    val email: String?,
    val avatar: ImageHolder?,
    val server: Server?,
    val password: String?,
    val apiKey: String?,
) {
    companion object {
        val EMPTY = UserData("", null, null, null, null, null)
    }
}