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
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError
import dev.brahmkshatriya.echo.extension.service.request.RequestService.toNetworkRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class TrackClientImpl : TrackClient {
    /*
     * OpenSubsonic servers never return partial tracks (Child type in the API), so no further
     * operation is needed when loading them
     */
    override suspend fun loadTrack(track: Track, isDownload: Boolean): Track {
        return track
    }

    override suspend fun loadStreamableMedia(
        streamable: Streamable,
        isDownload: Boolean,
    ): Streamable.Media {
        if (isDownload) {
            return Streamable.Media.Server(
                sources = listOf(
                    Streamable.Source.Http(
                        request = authenticatedRequest(
                            endpoint = "download",
                            parameters = listOf(
                                "id" to streamable.id,
                            ),
                            needsGet = true,
                        ).toNetworkRequest(),
                        type = Streamable.SourceType.Progressive,
                        quality = streamable.quality,
                        title = streamable.title,
                    ),
                ),
                merged = false,
            )
        }

        return Streamable.Media.Server(
            sources = listOf(
                Streamable.Source.Http(
                    request = authenticatedRequest(
                        endpoint = "stream",
                        parameters = buildList {
                            add("id" to streamable.id)
                            streamable.quality.let {
                                if (it == Int.MAX_VALUE) {
                                    add("format" to "raw")
                                    add("maxBitRate" to "0")
                                } else {
                                    add("format" to "mp3")
                                    add("maxBitRate" to it.toString())
                                }
                            }
                        },
                        needsGet = true,
                    ).toNetworkRequest(),
                    type = Streamable.SourceType.Progressive,
                    quality = streamable.quality,
                    title = streamable.title,
                ),
            ),
            merged = false,
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
                    parameters = buildList {
                        add("size" to count.toString())
                        genre?.let { add("genre" to it) }
                    }
                ),
            ).parseAs<GetRandomSongsDto>().subsonicResponse
            if (songsData.status != "ok") {
                throwOnError(songsData.error)
            }

            return songsData.randomSongs?.song?.map { it.toTrack() } ?: listOf()
        }

        suspend fun getSimilarTracks(track: Track, count: Int): List<Track> {
            val id = track.artists.firstOrNull()?.id ?: return listOf()
            val tracksData = runRequest(
                authenticatedRequest(
                    endpoint = "getSimilarSongs2",
                    parameters = listOf(
                        "id" to id,
                        "count" to count.toString(),
                    ),
                ),
            ).parseAs<GetSimilarSongsDto>().subsonicResponse
            if (tracksData.status != "ok") {
                throwOnError(tracksData.error)
            }

            return tracksData.similarSongs2?.song?.map { it.toTrack() } ?: listOf()
        }
    }
}