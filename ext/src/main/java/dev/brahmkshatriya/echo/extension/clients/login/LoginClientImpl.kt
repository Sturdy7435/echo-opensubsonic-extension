package dev.brahmkshatriya.echo.extension.clients.login

import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetOpenSubsonicExtensionsDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetUserDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.TokenInfoDto
import dev.brahmkshatriya.echo.extension.models.ServerData
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
            extras = mapOf(
                "serverUrl" to user.server?.url,
                "serverExtensions" to ServerData.Extension.serialize(user.server?.extensions)
            ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
        )
    }

    private suspend fun getServerExtensions(url: String): EnumSet<ServerData.Extension> {
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
                is MalformedURLException, is URISyntaxException, is UnknownHostException -> throw Exception(
                    "Invalid server address"
                )

                else -> throw e
            }
        }
        if (resp.code == 404) {
            throw Exception("Invalid server address")
        }

        val data = resp.parseAs<GetOpenSubsonicExtensionsDto>().subsonicResponse
        if (data.status != "ok") {
            throwOnError(data.error)
        }
        val extensions: EnumSet<ServerData.Extension> =
            EnumSet.noneOf(ServerData.Extension::class.java)
        data.openSubsonicExtensions!!.forEach {
            ServerData.Extension.entries.forEach { entry ->
                if (it.name == entry.id) {
                    extensions.add(entry)
                }
            }
        }
        return extensions
    }

    private suspend fun passwordLogin(data: Map<String, String?>): List<User> {
        val url: String = data["address"]!!
        val server = ServerData(
            url = url, extensions = getServerExtensions(url)
        )
        val tmp = UserData.EMPTY.copy(
            username = data["username"]!!,
            password = data["password"]!!,
            server = server,
        )

        val loginData = runRequest(
            authenticatedRequest(
                endpoint = "getUser", parameters = mapOf(
                    "username" to tmp.username,
                ), credentials = tmp
            )
        ).parseAs<GetUserDto>().subsonicResponse
        if (loginData.status != "ok") {
            throwOnError(loginData.error)
        }

        val avatar: ImageHolder = ImageHolder.NetworkRequestImageHolder(
            authenticatedRequest(
                endpoint = "getAvatar", parameters = mapOf(
                    "username" to tmp.username,
                ), credentials = tmp
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
                "serverExtensions" to ServerData.Extension.serialize(server.extensions)!!,
            ),
        )

        return listOf(user)
    }

    private suspend fun keyLogin(data: Map<String, String?>): List<User> {
        val url: String = data["address"]!!
        val server = ServerData(
            url = url, extensions = getServerExtensions(url)
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
            throwOnError(tokenData.error)
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
            throwOnError(loginData.error)
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
                "serverExtensions" to ServerData.Extension.serialize(server.extensions)!!,
            ),
        )

        return listOf(user)
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