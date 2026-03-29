package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.api.OpenSubsonicApi
import dev.brahmkshatriya.echo.extension.tabs.createHomeFeed

class OpenSubsonicExtension :
    ExtensionClient,
    HomeFeedClient,
    LoginClient.CustomInput,
    TrackClient,
    ArtistClient {

    val api by lazy { OpenSubsonicApi() }

    // Settings

    override suspend fun getSettingItems(): List<Setting> {
        return SettingsObject.items
    }

    override fun setSettings(settings: Settings) {
        SettingsObject.current = settings
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

    // Home Feed

    override suspend fun loadHomeFeed(): Feed<Shelf> {
        return createHomeFeed()
    }

    // Track

    override suspend fun loadTrack(track: Track, isDownload: Boolean): Track {
        return api.loadTrack(track)
    }

    override suspend fun loadStreamableMedia(
        streamable: Streamable,
        isDownload: Boolean,
    ): Streamable.Media {
        return api.loadStreamableMedia(streamable, isDownload)
    }

    override suspend fun loadFeed(track: Track): Feed<Shelf> {
        return createHomeFeed()
    }

    // Artists

    override suspend fun loadArtist(artist: Artist): Artist {
        return api.loadArtist(artist)
    }

    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        return api.loadArtistFeed(artist)
    }
}