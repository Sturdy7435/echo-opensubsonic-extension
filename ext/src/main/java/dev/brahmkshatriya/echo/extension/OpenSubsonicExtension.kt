package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings

class OpenSubsonicExtension :
    ExtensionClient,
    LoginClient.CustomInput {

    val api by lazy { OpenSubsonicApi() }

    // Settings

    override suspend fun onExtensionSelected() {}

    override suspend fun getSettingItems(): List<Setting> {
        return emptyList()
    }

    private lateinit var setting: Settings

    override fun setSettings(settings: Settings) {
        setting = settings
    }

    // Login

    enum class LoginType {
        UserPass,
        //ApiKey,
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
        /*
        LoginClient.Form(
            key = LoginType.ApiKey.name,
            label = "API Key",
            icon = LoginClient.InputField.Type.Misc,
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
        */
    )

    override suspend fun onLogin(
        key: String,
        data: Map<String, String?>,
    ): List<User> {
        return when (LoginType.valueOf(key)) {
            LoginType.UserPass -> {
                api.onPasswordLogin(data)
            }

            // TODO: Implement functions for pw and key logins
            /*LoginType.ApiKey -> {
                api.onApiLogin(data)
            }*/
        }
    }

    override fun setLoginUser(user: User?) {
        api.setUser(user)
    }

    override suspend fun getCurrentUser(): User? {
        return api.getUser()
    }
}