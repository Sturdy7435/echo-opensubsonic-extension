package dev.brahmkshatriya.echo.extension.clients.artist

import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetArtistDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetArtistInfoDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetArtistsDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetTopSongsDto
import dev.brahmkshatriya.echo.extension.service.feed.FeedUtils.concurrentFeed
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError

class ArtistClientImpl : ArtistClient {
    override suspend fun loadArtist(artist: Artist): Artist {
        val artistData = runRequest(
            authenticatedRequest(
                endpoint = "getArtist",
                parameters = listOf(
                    "id" to artist.id,
                ),
            ),
        ).parseAs<GetArtistDto>().subsonicResponse
        if (artistData.status != "ok") {
            throwOnError(artistData.error)
        }

        val extraData = runRequest(
            authenticatedRequest(
                endpoint = "getArtistInfo2",
                parameters = listOf(
                    "id" to artist.id,
                ),
            ),
        ).parseAs<GetArtistInfoDto>().subsonicResponse
        if (extraData.status != "ok") {
            throwOnError(extraData.error)
        }

        return artistData.artist!!.toArtist().copy(
            bio = extraData.biography,
        )
    }

    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        return concurrentFeed(
            {
                val topData = runRequest(
                    authenticatedRequest(
                        endpoint = "getTopSongs",
                        parameters = listOf(
                            "artist" to artist.name,
                            "count" to "50",
                        ),
                    ),
                ).parseAs<GetTopSongsDto>().subsonicResponse
                if (topData.status != "ok") {
                    throwOnError(topData.error)
                }

                val topMore =
                    topData.topSongs?.song?.map { it.toTrack() } ?: return@concurrentFeed null
                val top = topMore.take(10)

                Shelf.Lists.Tracks(
                    id = "top",
                    title = "Top songs",
                    list = top,
                    more = topMore.map { it.toShelf() }.toFeed(),
                    type = Shelf.Lists.Type.Grid,
                )
            },
            {
                val albumsData = runRequest(
                    authenticatedRequest(
                        endpoint = "getArtist",
                        parameters = listOf(
                            "id" to artist.id,
                        ),
                    ),
                ).parseAs<GetArtistDto>().subsonicResponse
                if (albumsData.status != "ok") {
                    throwOnError(albumsData.error)
                }
                val albums =
                    albumsData.artist?.album?.map { it.toAlbum() } ?: return@concurrentFeed null

                Shelf.Lists.Items(
                    id = "albums",
                    title = "Albums",
                    list = albums,
                    type = Shelf.Lists.Type.Linear,
                )
            },
            {
                val similarData = runRequest(
                    authenticatedRequest(
                        endpoint = "getArtistInfo2",
                        parameters = listOf(
                            "id" to artist.id,
                        ),
                    ),
                ).parseAs<GetArtistInfoDto>().subsonicResponse
                if (similarData.status != "ok") {
                    throwOnError(similarData.error)
                }
                val similar =
                    similarData.similarArtist?.map { it.toArtist() } ?: return@concurrentFeed null

                Shelf.Lists.Items(
                    id = "similar",
                    title = "Similar artists",
                    list = similar,
                    type = Shelf.Lists.Type.Linear,
                )
            },
        )
    }

    companion object {
        suspend fun getArtists(): List<Artist> {
            val artistsData = runRequest(
                authenticatedRequest(
                    endpoint = "getArtists",
                    parameters = listOf(),
                ),
            ).parseAs<GetArtistsDto>().subsonicResponse
            if (artistsData.status != "ok") {
                throwOnError(artistsData.error)
            }

            // Create a List<Artist> from the artists inside each `artist` field of the elements of
            // `index`
            return artistsData.artists?.index?.flatMap { it.artist.orEmpty() }
                ?.map { it.toArtist() } ?: listOf()
        }
    }
}