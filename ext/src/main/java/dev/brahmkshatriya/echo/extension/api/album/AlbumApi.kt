package dev.brahmkshatriya.echo.extension.api.album

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.api.request.parseAs
import dev.brahmkshatriya.echo.extension.api.request.runRequest
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetAlbumDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetArtistDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

suspend fun getAlbum(album: Album): Album {
    val albumData = runRequest(
        authenticatedRequest(
            endpoint = "getAlbum",
            parameters = mapOf(
                "id" to album.id
            ),
        )
    ).parseAs<GetAlbumDto>().subsonicResponse

    return albumData.album!!.toAlbum()
}

suspend fun getTracks(album: Album): Feed<Track>? {
    val albumData = runRequest(
        authenticatedRequest(
            endpoint = "getAlbum",
            parameters = mapOf(
                "id" to album.id
            ),
        )
    ).parseAs<GetAlbumDto>().subsonicResponse

    return albumData.album!!.song?.map { it.toTrack() }?.toFeed()
}

suspend fun createAlbumFeed(album: Album): Feed<Shelf>? {
    val artist = album.artists.firstOrNull() ?: return null
    val otherAlbumsData = runRequest(
        authenticatedRequest(
            endpoint = "getArtist",
            parameters = mapOf(
                "id" to artist.id
            ),
        )
    ).parseAs<GetArtistDto>().subsonicResponse.artist?.album
    val otherAlbums: List<Album> = otherAlbumsData?.map { it.toAlbum() } ?: listOf()

    return withContext(Dispatchers.IO) {
        listOf(
            async {
                Shelf.Lists.Items(
                    id = "otherAlbums",
                    title = "More from this artist",
                    list = otherAlbums,
                    type = Shelf.Lists.Type.Linear,
                )
            },
        ).awaitAll()
    }.toFeed()
}