package dev.brahmkshatriya.echo.extension.api.track

import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.api.request.handleError
import dev.brahmkshatriya.echo.extension.api.request.parseAs
import dev.brahmkshatriya.echo.extension.api.request.runRequest
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetRandomSongsDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetSimilarSongsDto
import dev.brahmkshatriya.echo.extension.toNetworkRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

suspend fun getRandomTracks(count: Int, genre: String? = null): List<Track> {
    val songsData = runRequest(
        authenticatedRequest(
            endpoint = "getRandomSongs",
            parameters = mapOf(
                "size" to count.toString(),
                "genre" to genre,
            ).filterValues { it != null }.mapValues { it.value!! },
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

suspend fun getSimilarTracks(track: Track, count: Int): List<Track> {
    val id = track.artists.firstOrNull()?.id ?: return listOf()
    val tracksData = runRequest(
        authenticatedRequest(
            endpoint = "getSimilarSongs2",
            parameters = mapOf(
                "id" to id,
                "count" to count.toString(),
            ),
        )
    ).parseAs<GetSimilarSongsDto>().subsonicResponse

    return tracksData.similarSongs2?.song?.map { it.toTrack() } ?: listOf()
}

suspend fun createTrackFeed(track: Track): Feed<Shelf> {
    return withContext(Dispatchers.IO) {
        listOf(
            async {
                Shelf.Lists.Items(
                    id = "otherAlbums",
                    title = "More from this artist",
                    list = getSimilarTracks(track, 20),
                    type = Shelf.Lists.Type.Linear,
                )
            },
        ).awaitAll()
    }.toFeed()
}