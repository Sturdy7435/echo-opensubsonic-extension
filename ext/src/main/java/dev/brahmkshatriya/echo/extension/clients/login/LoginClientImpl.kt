package dev.brahmkshatriya.echo.extension.clients.login

import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetOpenSubsonicExtensionsDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetUserDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.TokenInfoDto
import dev.brahmkshatriya.echo.extension.models.ServerData
import dev.brahmkshatriya.echo.extension.models.ServerData.Extension.Companion.ID_TO_NAME
import dev.brahmkshatriya.echo.extension.models.UserData
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.get
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError
import dev.brahmkshatriya.echo.extension.service.request.RequestService.toNetworkRequest
import dev.brahmkshatriya.echo.extension.service.session.UserSession
import okhttp3.Response
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URISyntaxException
import java.net.UnknownHostException
import java.util.EnumSet

class LoginClientImpl : LoginClient.CustomInput {
    enum class LoginType {
        UserPass, ApiKey,
    }

    override val forms: List<LoginClient.Form> = listOf(
        LoginClient.Form(
            key = LoginType.UserPass.name,
            label = "Username and Password",
            icon = LoginClient.InputField.Type.Username,
            inputFields = listOf(
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Url,
                    key = "address",
                    label = "Address",
                    isRequired = true,
                ),
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Username,
                    key = "username",
                    label = "Username",
                    isRequired = true,
                ),
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Password,
                    key = "password",
                    label = "Password",
                    isRequired = false,
                ),
            ),
        ),
        LoginClient.Form(
            key = LoginType.ApiKey.name,
            label = "API Key",
            icon = LoginClient.InputField.Type.Password,
            inputFields = listOf(
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Url,
                    key = "address",
                    label = "Address",
                    isRequired = true,
                ),
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Password,
                    key = "apiKey",
                    label = "API key",
                    isRequired = true,
                ),
            ),
        ),
    )

    override suspend fun onLogin(key: String, data: Map<String, String?>): List<User> {
        return when (LoginType.valueOf(key)) {
            LoginType.UserPass -> {
                passwordLogin(data)
            }

            LoginType.ApiKey -> {
                keyLogin(data)
            }
        }
    }

    override fun setLoginUser(user: User?) {
        UserSession.currentUser = user?.let {
            UserData(
                username = it.name,
                email = it.subtitle,
                avatar = it.cover,
                server = ServerData(
                    it.extras["serverUrl"]!!,
                    ServerData.Extension.deserialize(it.extras["serverExtensions"]),
                ),
                password = it.extras["password"],
                apiKey = it.extras["apiKey"],
            )
        } ?: UserData.EMPTY
    }

    override suspend fun getCurrentUser(): User? {
        try {
            checkAuth()
        } catch (_: ClientException.LoginRequired) {
            return null
        }

        // Email address (subtitle), password and apiKey removed
        val user = UserSession.currentUser
        return User(
            id = user.username,
            name = user.username,
            cover = user.avatar,
            subtitle = null,
            extras = buildMap {
                user.server?.url?.let { put("serverUrl", it) }
                user.server?.extensions
                    ?.let { ServerData.Extension.serialize(it) }
                    ?.let { put("serverExtensions", it) }
            },
        )
    }

    /**
     * Returns the OpenSubsonic extensions supported by the server.
     * As this should be the first request executed, it must catch any exceptions caused by an
     * invalid URL.
     */
    private suspend fun getServerExtensions(url: String): EnumSet<ServerData.Extension> {
        val resp: Response = try {
            runRequest(
                get(
                    baseUrl = url,
                    endpoint = "getOpenSubsonicExtensions",
                ),
            )
        } catch (e: Exception) {
            when (e) {
                is MalformedURLException, is URISyntaxException, is UnknownHostException -> {
                    throw Exception("Invalid server address")
                }

                is SocketTimeoutException -> {
                    throw Exception("Connection timed out, check server availability")
                }

                else -> {
                    throw e
                }
            }
        }
        if (!resp.isSuccessful) {
            throw Exception("Server returned error ${resp.code}: ${resp.message}")
        }

        val data = resp.parseAs<GetOpenSubsonicExtensionsDto>().subsonicResponse
        if (data.status != "ok") {
            throwOnError(data.error)
        }

        return ServerData.Extension.EMPTY.apply {
            data.openSubsonicExtensions!!
                .mapNotNull { ID_TO_NAME[it.name] }
                .forEach { add(it) }
        }
    }

    private suspend fun passwordLogin(data: Map<String, String?>): List<User> {
        val url: String = data["address"]!!
        val server = ServerData(
            url = url,
            extensions = getServerExtensions(url),
        )
        val tmp = UserData.EMPTY.copy(
            username = data["username"]!!,
            password = data["password"]!!,
            server = server,
        )

        val loginData = runRequest(
            authenticatedRequest(
                endpoint = "getUser",
                parameters = listOf(
                    "username" to tmp.username,
                ),
                credentials = tmp,
            ),
        ).parseAs<GetUserDto>().subsonicResponse
        if (loginData.status != "ok") {
            throwOnError(loginData.error)
        }

        val avatar: ImageHolder = ImageHolder.NetworkRequestImageHolder(
            authenticatedRequest(
                endpoint = "getAvatar",
                parameters = listOf(
                    "username" to tmp.username,
                ),
                needsGet = true,
                credentials = tmp,
            ).toNetworkRequest(),
            crop = false,
        )

        return listOf(
            User(
                id = tmp.username,
                name = tmp.username,
                cover = avatar,
                subtitle = loginData.user?.email,
                extras = mapOf(
                    "password" to tmp.password!!,
                    "serverUrl" to server.url,
                    "serverExtensions" to ServerData.Extension.serialize(server.extensions)!!,
                ),
            ),
        )
    }

    private suspend fun keyLogin(data: Map<String, String?>): List<User> {
        val url: String = data["address"]!!
        val server = ServerData(
            url = url,
            extensions = getServerExtensions(url),
        )
        val tmp = UserData.EMPTY.copy(
            apiKey = data["apiKey"]!!,
            server = server,
        )

        val tokenData = runRequest(
            authenticatedRequest(
                endpoint = "tokenInfo",
                credentials = tmp,
            ),
        ).parseAs<TokenInfoDto>().subsonicResponse
        if (tokenData.status != "ok") {
            throwOnError(tokenData.error)
        }
        val username = tokenData.tokenInfo!!.username

        val loginData = runRequest(
            authenticatedRequest(
                endpoint = "getUser",
                parameters = listOf(
                    "username" to username,
                ),
                credentials = tmp,
            ),
        ).parseAs<GetUserDto>().subsonicResponse
        if (loginData.status != "ok") {
            throwOnError(loginData.error)
        }

        val avatar = ImageHolder.NetworkRequestImageHolder(
            authenticatedRequest(
                endpoint = "getAvatar",
                parameters = listOf(
                    "username" to username,
                ),
                needsGet = true,
                credentials = tmp,
            ).toNetworkRequest(),
            crop = false,
        )

        return listOf(
            User(
                id = username,
                name = username,
                cover = avatar,
                subtitle = loginData.user?.email,
                extras = mapOf(
                    "apiKey" to tmp.apiKey!!,
                    "serverUrl" to url,
                    "serverExtensions" to ServerData.Extension.serialize(server.extensions)!!,
                ),
            ),
        )
    }

    companion object {
        fun getCurrentUser(): UserData {
            return UserSession.currentUser
        }

        fun checkAuth(credentials: UserData = getCurrentUser()) {
            if (credentials.server == null
                || (credentials.password == null && credentials.apiKey == null)
            ) {
                throw ClientException.LoginRequired()
            }
        }
    }
}