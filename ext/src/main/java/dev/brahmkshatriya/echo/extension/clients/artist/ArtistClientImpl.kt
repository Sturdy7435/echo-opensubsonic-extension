package dev.brahmkshatriya.echo.extension.clients.artist

import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetArtistDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetArtistInfoDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetArtistsDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetTopSongsDto
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class ArtistClientImpl : ArtistClient {
    override suspend fun loadArtist(artist: Artist): Artist {
        val artistData = runRequest(
            authenticatedRequest(
                endpoint = "getArtist",
                parameters = mapOf(
                    "id" to artist.id
                ),
            )
        ).parseAs<GetArtistDto>().subsonicResponse

        val extraData = runRequest(
            authenticatedRequest(
                endpoint = "getArtistInfo2",
                parameters = mapOf(
                    "id" to artist.id
                ),
            )
        ).parseAs<GetArtistInfoDto>().subsonicResponse

        return artistData.artist!!.toArtist().copy(
            bio = extraData.biography,
        )
    }

    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        val topData = runRequest(
            authenticatedRequest(
                endpoint = "getTopSongs",
                parameters = mapOf(
                    "artist" to artist.name,
                    "count" to "50",
                ),
            )
        ).parseAs<GetTopSongsDto>().subsonicResponse.topSongs
        val topMore = topData?.song?.map { it.toTrack() } ?: listOf()
        val topSize = 9.coerceAtMost(topMore.size - 1)
        val top = topMore.slice(0..topSize)

        val albumsData = runRequest(
            authenticatedRequest(
                endpoint = "getArtist",
                parameters = mapOf(
                    "id" to artist.id
                ),
            )
        ).parseAs<GetArtistDto>().subsonicResponse.artist?.album
        val albums: List<Album> = albumsData?.map { it.toAlbum() } ?: listOf()

        val similarData = runRequest(
            authenticatedRequest(
                endpoint = "getArtistInfo2",
                parameters = mapOf(
                    "id" to artist.id
                ),
            )
        ).parseAs<GetArtistInfoDto>().subsonicResponse.similarArtist
        val similar: List<Artist> = similarData?.map { it.toArtist() } ?: listOf()

        return withContext(Dispatchers.IO) {
            listOf(
                async {
                    Shelf.Lists.Tracks(
                        id = "top",
                        title = "Top songs",
                        list = top,
                        more = topMore.map { it.toShelf() }.toFeed(),
                        type = Shelf.Lists.Type.Grid,
                    )
                },
                async {
                    Shelf.Lists.Items(
                        id = "albums",
                        title = "Albums",
                        list = albums,
                        type = Shelf.Lists.Type.Linear,
                    )
                },
                async {
                    Shelf.Lists.Items(
                        id = "similar",
                        title = "Similar artists",
                        list = similar,
                        type = Shelf.Lists.Type.Linear
                    )
                },
            ).awaitAll()
        }.toFeed()
    }

    companion object {
        suspend fun getArtists(): List<Artist> {
            val artistsData = runRequest(
                authenticatedRequest(
                    endpoint = "getArtists",
                    parameters = mapOf(),
                )
            ).parseAs<GetArtistsDto>().subsonicResponse

            /// Create a List<Artist> from the artists inside each `artist` field of the elements of
            /// `index`
            return artistsData.artists?.index
                ?.flatMap { it.artist.orEmpty() }
                ?.map { it.toArtist() }
                ?: listOf()
        }
    }
}