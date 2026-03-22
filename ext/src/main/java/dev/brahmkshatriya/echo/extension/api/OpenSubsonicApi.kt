package dev.brahmkshatriya.echo.extension.api

import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extension.api.login.getUserSession
import dev.brahmkshatriya.echo.extension.api.login.keyLogin
import dev.brahmkshatriya.echo.extension.api.login.passwordLogin
import dev.brahmkshatriya.echo.extension.api.login.setUserSession
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.api.request.handleError
import dev.brahmkshatriya.echo.extension.api.request.parseAs
import dev.brahmkshatriya.echo.extension.api.request.runRequest
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetRandomSongsDto
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
class OpenSubsonicApi {

    suspend fun onPasswordLogin(data: Map<String, String?>): List<User> {
        return passwordLogin(data)
    }

    suspend fun onKeyLogin(data: Map<String, String?>): List<User> {
        return keyLogin(data)
    }

    fun setUser(user: User?) {
        setUserSession(user)
    }

    fun getUser(): User? {
        return getUserSession()
    }

    // Track
    suspend fun getRandomTracks(): Shelf {
        val resp = runRequest(
            authenticatedRequest(
                endpoint = "getRandomSongs",
                parameters = mapOf(
                    "size" to "20",
                ),
            )
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
}
