package dev.brahmkshatriya.echo.extension.api.artist

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.api.request.parseAs
import dev.brahmkshatriya.echo.extension.api.request.runRequest
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetArtistDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetArtistInfoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

suspend fun getArtist(artist: Artist): Artist {
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

    return artistData.artist.toArtist().copy(
        bio = extraData.biography,
    )
}

suspend fun createArtistFeed(artist: Artist): Feed<Shelf> {
    val albumsData = runRequest(
        authenticatedRequest(
            endpoint = "getArtist",
            parameters = mapOf(
                "id" to artist.id
            ),
        )
    ).parseAs<GetArtistDto>().subsonicResponse.artist.album
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
            }
        ).awaitAll()
    }.toFeed()
}