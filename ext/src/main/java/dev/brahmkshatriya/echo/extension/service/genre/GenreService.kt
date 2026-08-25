package dev.brahmkshatriya.echo.extension.service.genre

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
import dev.brahmkshatriya.echo.extension.service.feed.FeedUtils.concurrentShelves
import dev.brahmkshatriya.echo.extension.service.feed.FeedUtils.continuousFeed
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError

object GenreService {
    suspend fun getGenres(): List<String> {
        val genresData = runRequest(
            authenticatedRequest(
                endpoint = "getGenres",
                parameters = listOf(),
            ),
        ).parseAs<GetGenresDto>().subsonicResponse
        if (genresData.status != "ok") {
            throwOnError(genresData.error)
        }

        return genresData.genres?.genre?.map { it.value } ?: listOf()
    }

    suspend fun getGenreTracks(genre: String, count: Int, offset: Int = 0): List<Track> {
        val tracksData = runRequest(
            authenticatedRequest(
                endpoint = "getSongsByGenre",
                parameters = listOf(
                    "genre" to genre,
                    "count" to count.toString(),
                    "offset" to offset.toString(),
                ),
            ),
        ).parseAs<GetSongsByGenreDto>().subsonicResponse
        if (tracksData.status != "ok") {
            throwOnError(tracksData.error)
        }

        return tracksData.songsByGenre?.song?.map { it.toTrack() } ?: listOf()
    }

    suspend fun getGenreAlbums(genre: String, count: Int, offset: Int = 0): List<Album> {
        val albumsData = runRequest(
            authenticatedRequest(
                endpoint = "getAlbumList2",
                parameters = listOf(
                    "type" to "byGenre",
                    "genre" to genre,
                    "size" to count.toString(),
                    "offset" to offset.toString(),
                ),
            ),
        ).parseAs<GetAlbumListDto>().subsonicResponse
        if (albumsData.status != "ok") {
            throwOnError(albumsData.error)
        }

        return albumsData.albumList2?.album?.map { it.toAlbum() } ?: listOf()
    }

    fun createGenreFeed(genre: String): Feed<Shelf> {
        return PagedData.Single {
            concurrentShelves(
                {
                    val tracks = getRandomTracks(20, genre)
                    val pageSize = 20
                    val tracksFull = continuousFeed(pageSize) { offset ->
                        getGenreTracks(
                            genre = genre,
                            count = pageSize,
                            offset = offset,
                        ).map { it.toShelf() }
                    }

                    Shelf.Lists.Items(
                        id = "tracks",
                        title = "Tracks",
                        list = tracks,
                        more = tracksFull,
                        type = Shelf.Lists.Type.Linear,
                    )
                },
                {
                    val albums = getGenreAlbums(genre, 10, 0)
                    val pageSize = 20
                    val albumsFull = continuousFeed(pageSize) { offset ->
                        getGenreAlbums(
                            genre = genre,
                            count = pageSize,
                            offset = offset,
                        ).map { it.toShelf() }
                    }

                    Shelf.Lists.Items(
                        id = "albums",
                        title = "Albums",
                        list = albums,
                        more = albumsFull,
                        type = Shelf.Lists.Type.Linear,
                    )
                },
            )
        }.toFeed()
    }
}