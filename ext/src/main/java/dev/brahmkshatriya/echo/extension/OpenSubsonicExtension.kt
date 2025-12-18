package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.Settings

class OpenSubsonicExtension :
    ExtensionClient,
    LoginClient.CustomInput {

    private val api by lazy { OpenSubsonicApi() }

    // Settings

    override suspend fun onExtensionSelected() {}

    override suspend fun getSettingItems(): List<Setting> {
        return listOf(
            SettingSwitch(
                "Force GET requests",
                "force_get_requests",
                "Whether to force usage of GET requests even if the server supports POST, useful for debugging through server logs but also allows logging of authentication data",
                forceGetRequests
            )
        )
    }
    val forceGetRequests get() = setting.getBoolean("force_get_requests") ?: false

    private lateinit var setting: Settings

    override fun setSettings(settings: Settings) {
        setting = settings
    }

    // Login

    enum class LoginType {
        UserPass,
        ApiKey,
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

    override suspend fun onLogin(
        key: String,
        data: Map<String, String?>,
    ): List<User> {
        return when (LoginType.valueOf(key)) {
            LoginType.UserPass -> {
                api.onPasswordLogin(data)
            }

            LoginType.ApiKey -> {
                api.onKeyLogin(data)
            }
        }
    }

    override fun setLoginUser(user: User?) {
        api.setUser(user)
    }

    override suspend fun getCurrentUser(): User? {
        return api.getUser()
    }
}