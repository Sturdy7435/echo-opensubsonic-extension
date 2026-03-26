package dev.brahmkshatriya.echo.extension.api.track

import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.api.request.handleError
import dev.brahmkshatriya.echo.extension.api.request.parseAs
import dev.brahmkshatriya.echo.extension.api.request.runRequest
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetRandomSongsDto
import dev.brahmkshatriya.echo.extension.toNetworkRequest

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
    return track
}

fun getStreamableMedia(streamable: Streamable, isDownload: Boolean): Streamable.Media {
    return Streamable.Media.Server(
        sources = listOf(
            Streamable.Source.Http(
                request = if (isDownload) {
                    authenticatedRequest(
                        endpoint = "download",
                        parameters = mapOf(
                            "id" to streamable.id,
                        ),
                    ).toNetworkRequest()
                } else {
                    authenticatedRequest(
                        endpoint = "stream",
                        parameters = mapOf(
                            "id" to streamable.id,
                        ),
                    ).toNetworkRequest()
                },
                type = Streamable.SourceType.Progressive,
                quality = streamable.quality,
                title = streamable.title,
            ),
        ),
        merged = true
    )
}