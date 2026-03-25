package dev.brahmkshatriya.echo.extension.api.track

import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.api.request.handleError
import dev.brahmkshatriya.echo.extension.api.request.parseAs
import dev.brahmkshatriya.echo.extension.api.request.runRequest
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetRandomSongsDto

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