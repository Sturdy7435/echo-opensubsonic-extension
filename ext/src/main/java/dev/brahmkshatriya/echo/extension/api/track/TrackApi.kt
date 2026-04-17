package dev.brahmkshatriya.echo.extension.api.track

import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.api.request.handleError
import dev.brahmkshatriya.echo.extension.api.request.parseAs
import dev.brahmkshatriya.echo.extension.api.request.runRequest
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetRandomSongsDto
import dev.brahmkshatriya.echo.extension.toNetworkRequest

suspend fun getRandomTracks(): List<Track> {
    val songsData = runRequest(
        authenticatedRequest(
            endpoint = "getRandomSongs",
            parameters = mapOf(
                "size" to "20",
            ),
        )
    ).parseAs<GetRandomSongsDto>().subsonicResponse
    if (songsData.status != "ok") {
        handleError(songsData.error)
    }

    return songsData.randomSongs?.song?.map { it.toTrack() } ?: listOf()
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

@Suppress("unused")
fun createTrackFeed(track: Track): Feed<Shelf>? {
    return null
}