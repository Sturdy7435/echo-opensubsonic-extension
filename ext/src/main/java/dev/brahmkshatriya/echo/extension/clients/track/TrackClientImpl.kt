package dev.brahmkshatriya.echo.extension.clients.track

import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetRandomSongsDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetSimilarSongsDto
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.handleError
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.toNetworkRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class TrackClientImpl : TrackClient {
    override suspend fun loadTrack(track: Track, isDownload: Boolean): Track {
        return track
    }

    override suspend fun loadStreamableMedia(
        streamable: Streamable,
        isDownload: Boolean,
    ): Streamable.Media {
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

    override suspend fun loadFeed(track: Track): Feed<Shelf> {
        return withContext(Dispatchers.IO) {
            listOf(
                async {
                    Shelf.Lists.Items(
                        id = "similar",
                        title = "Similar tracks",
                        list = getSimilarTracks(track, 20),
                        type = Shelf.Lists.Type.Linear,
                    )
                },
            ).awaitAll()
        }.toFeed()
    }

    companion object {
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
    }
}