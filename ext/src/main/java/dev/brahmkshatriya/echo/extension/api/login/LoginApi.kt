package dev.brahmkshatriya.echo.extension.api.login

import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.api.request.get
import dev.brahmkshatriya.echo.extension.api.request.handleError
import dev.brahmkshatriya.echo.extension.api.request.parseAs
import dev.brahmkshatriya.echo.extension.api.request.runRequest
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetOpenSubsonicExtensionsDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetUserDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.TokenInfoDto
import dev.brahmkshatriya.echo.extension.toNetworkRequest
import okhttp3.Response
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.UnknownHostException
import java.util.EnumSet

fun checkAuth(credentials: UserData = UserSession.current) {
    if (credentials.server == null || (credentials.password == null && credentials.apiKey == null)) {
        throw ClientException.LoginRequired()
    }
}

private suspend fun getServerExtensions(url: String): EnumSet<Server.Extension> {
    val resp: Response
    try {
        resp = runRequest(
            get(
                baseUrl = url,
                endpoint = "getOpenSubsonicExtensions",
            ),
        )
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

suspend fun passwordLogin(data: Map<String, String?>): List<User> {
    val url: String = data["address"]!!
    val server = Server(
        url = url,
        extensions = getServerExtensions(url)
    )
    val tmp = UserData.EMPTY.copy(
        username = data["username"]!!,
        password = data["password"]!!,
        server = server,
    )

    val loginData = runRequest(
        authenticatedRequest(
            endpoint = "getUser",
            parameters = mapOf(
                "username" to tmp.username,
            ),
            credentials = tmp
        )
    ).parseAs<GetUserDto>().subsonicResponse
    if (loginData.status != "ok") {
        handleError(loginData.error)
    }

    val avatar: ImageHolder = ImageHolder.NetworkRequestImageHolder(
        authenticatedRequest(
            endpoint = "getAvatar",
            parameters = mapOf(
                "username" to tmp.username,
            ),
            credentials = tmp
        ).toNetworkRequest(),
        crop = false,
    )

    val user = User(
        id = tmp.username,
        name = tmp.username,
        cover = avatar,
        subtitle = loginData.user?.email,
        extras = mapOf(
            "password" to tmp.password!!,
            "serverUrl" to server.url,
            "serverExtensions" to Server.Extension.serialize(server.extensions)!!,
        ),
    )

    return listOf(user)
}

suspend fun keyLogin(data: Map<String, String?>): List<User> {
    val url: String = data["address"]!!
    val server = Server(
        url = url,
        extensions = getServerExtensions(url)
    )
    val tmp = UserData.EMPTY.copy(
        apiKey = data["apiKey"]!!,
        server = server,
    )

    val tokenData = runRequest(
        authenticatedRequest(
            endpoint = "tokenInfo",
            credentials = tmp,
        )
    ).parseAs<TokenInfoDto>().subsonicResponse
    if (tokenData.status != "ok") {
        handleError(tokenData.error)
    }
    val username = tokenData.tokenInfo!!.username

    val loginData = runRequest(
        authenticatedRequest(
            endpoint = "getUser",
            parameters = mapOf(
                "username" to username,
            ),
            credentials = tmp,
        )
    ).parseAs<GetUserDto>().subsonicResponse
    if (loginData.status != "ok") {
        handleError(loginData.error)
    }

    val avatar = ImageHolder.NetworkRequestImageHolder(
        authenticatedRequest(
            endpoint = "getAvatar",
            parameters = mapOf(
                "username" to username,
            ),
            credentials = tmp,
        ).toNetworkRequest(),
        crop = false,
    )

    val user = User(
        id = username,
        name = username,
        cover = avatar,
        subtitle = loginData.user?.email,
        extras = mapOf(
            "apiKey" to tmp.apiKey!!,
            "serverUrl" to url,
            "serverExtensions" to Server.Extension.serialize(server.extensions)!!,
        ),
    )

    return listOf(user)
}

fun setUserSession(user: User?) {
    UserSession.current = user?.let {
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

fun getUserSession(): User? {
    try {
        checkAuth()
    } catch (_: ClientException.LoginRequired) {
        return null
    }

    val userData = UserSession.current
    // Email address (subtitle), password and apiKey removed
    return User(
        id = userData.username,
        name = userData.username,
        cover = userData.avatar,
        subtitle = null,
        extras = mapOf(
            "serverUrl" to userData.server?.url,
            "serverExtensions" to Server.Extension.serialize(userData.server?.extensions)
        ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
    )
}