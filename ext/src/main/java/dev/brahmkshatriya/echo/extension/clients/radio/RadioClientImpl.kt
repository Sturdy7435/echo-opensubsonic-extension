package dev.brahmkshatriya.echo.extension.clients.radio

import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetSimilarSongsDto
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError

class RadioClientImpl: RadioClient {
    override suspend fun loadRadio(radio: Radio): Radio {
        return radio
    }

    override suspend fun loadTracks(radio: Radio): Feed<Track> {
        val radioData = runRequest(
            authenticatedRequest(
                endpoint = "getSimilarSongs2",
                parameters = listOf(
                    "id" to radio.artists[0].id,
                ),
            ),
        ).parseAs<GetSimilarSongsDto>().subsonicResponse
        if (radioData.status != "ok") {
            throwOnError(radioData.error)
        }

        return (radioData.similarSongs2?.song?.map { it.toTrack() } ?: emptyList()).toFeed()
    }

    override suspend fun radio(
        item: EchoMediaItem,
        context: EchoMediaItem?,
    ): Radio {
        val (title, artist) = when (item) {
            is Track -> item.title to item.artists[0]
            is Album -> item.title to item.artists[0]
            is Artist -> item.name to item
            else -> throw ClientException.NotSupported("Radio on ${item.javaClass.name}")
        }

        return Radio(
            id = item.id,
            title = "$title Radio",
            authors = listOf(artist),
            isShareable = false,
        )
    }
}