package dev.brahmkshatriya.echo.extension.service.genre

import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.clients.track.TrackClientImpl.Companion.getRandomTracks
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetAlbumListDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetGenresDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetSongsByGenreDto
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object GenreService {
    suspend fun getGenres(): List<String> {
        val genresData = runRequest(
            authenticatedRequest(
                endpoint = "getGenres",
                parameters = mapOf(),
            )
        ).parseAs<GetGenresDto>().subsonicResponse

        return genresData.genres?.genre?.map { it.value } ?: listOf()
    }

    suspend fun getGenreTracks(genre: String, count: Int, offset: Int = 0): List<Track> {
        val tracksData = runRequest(
            authenticatedRequest(
                endpoint = "getSongsByGenre",
                parameters = mapOf(
                    "genre" to genre,
                    "count" to count.toString(),
                    "offset" to offset.toString()
                ),
            )
        ).parseAs<GetSongsByGenreDto>().subsonicResponse

        return tracksData.songsByGenre?.song?.map { it.toTrack() } ?: listOf()
    }

    suspend fun getGenreAlbums(genre: String, count: Int, offset: Int = 0): List<Album> {
        val albumsData = runRequest(
            authenticatedRequest(
                endpoint = "getAlbumList2",
                parameters = mapOf(
                    "type" to "byGenre",
                    "genre" to genre,
                    "size" to count.toString(),
                    "offset" to offset.toString()
                ),
            )
        ).parseAs<GetAlbumListDto>().subsonicResponse

        return albumsData.albumList2?.album?.map { it.toAlbum() } ?: listOf()
    }

    suspend fun createGenreFeed(genre: String): Feed<Shelf> {
        return withContext(Dispatchers.IO) {
            listOf(
                async {
                    val tracks = getRandomTracks(20, genre)
                    val pageSize = 20
                    val tracksFull = PagedData.Continuous { continuation ->
                        val contInt = continuation?.toIntOrNull() ?: 0

                        val tracks: List<Shelf> =
                            getGenreTracks(genre, pageSize, contInt)
                                .map { it.toShelf() }

                        if (tracks.size < pageSize) Page(tracks, null)
                        else Page(tracks, (contInt + pageSize).toString())
                    }.toFeed()

                    Shelf.Lists.Items(
                        id = "tracks",
                        title = "Tracks",
                        list = tracks,
                        more = tracksFull,
                        type = Shelf.Lists.Type.Linear,
                    )
                },
                async {
                    val albums = getGenreAlbums(genre, 10, 0)
                    val pageSize = 20
                    val albumsFull = PagedData.Continuous { continuation ->
                        val contInt = continuation?.toIntOrNull() ?: 0

                        val albums: List<Shelf> =
                            getGenreAlbums(genre, pageSize, contInt)
                                .map { it.toShelf() }

                        if (albums.size < pageSize) Page(albums, null)
                        else Page(albums, (contInt + pageSize).toString())
                    }.toFeed()

                    Shelf.Lists.Items(
                        id = "albums",
                        title = "Albums",
                        list = albums,
                        more = albumsFull,
                        type = Shelf.Lists.Type.Linear,
                    )
                },
            ).awaitAll()
        }.toFeed()
    }
}