package dev.brahmkshatriya.echo.extension.clients.homefeed

import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.clients.album.AlbumClientImpl.Companion.AlbumListType
import dev.brahmkshatriya.echo.extension.clients.album.AlbumClientImpl.Companion.getAlbumList
import dev.brahmkshatriya.echo.extension.clients.artist.ArtistClientImpl.Companion.getArtists
import dev.brahmkshatriya.echo.extension.clients.track.TrackClientImpl.Companion.getRandomTracks
import dev.brahmkshatriya.echo.extension.service.genre.GenreService.createGenreFeed
import dev.brahmkshatriya.echo.extension.service.genre.GenreService.getGenres
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class HomeFeedClientImpl : HomeFeedClient {
    override suspend fun loadHomeFeed(): Feed<Shelf> {
        return withContext(Dispatchers.IO) {
            listOf(
                async {
                    Shelf.Lists.Items(
                        id = "randomTracks",
                        title = "Random Tracks",
                        list = getRandomTracks(20),
                        type = Shelf.Lists.Type.Linear,
                    )
                },
                async {
                    val albumList: List<Album> = getAlbumList(AlbumListType.Random, 10)
                    val pageSize = 20
                    val albumListFull = PagedData.Continuous { continuation ->
                        val contInt = continuation?.toIntOrNull() ?: 0

                        val albums: List<Shelf> =
                            getAlbumList(AlbumListType.AlphabeticalByName, pageSize, contInt)
                                .map { it.toShelf() }

                        if (albums.size < pageSize) Page(albums, null)
                        else Page(albums, (contInt + pageSize).toString())
                    }.toFeed()

                    Shelf.Lists.Items(
                        id = "albums",
                        title = "Albums",
                        list = albumList,
                        more = albumListFull,
                        type = Shelf.Lists.Type.Linear
                    )
                },
                async {
                    val artistListFull: List<Artist> = getArtists()
                    val artistList: List<Artist> =
                        artistListFull.shuffled().subList(0, 10.coerceAtMost(artistListFull.size))

                    Shelf.Lists.Items(
                        id = "artists",
                        title = "Artists",
                        list = artistList,
                        more = artistListFull.map { it.toShelf() }.toFeed(),
                        type = Shelf.Lists.Type.Linear
                    )
                },
                async {
                    // FIXME: too many requests during reload but it works

                    val genresListFull: List<String> = getGenres()
                    val genresList: List<String> =
                        genresListFull.shuffled().subList(0, 8.coerceAtMost(genresListFull.size))

                    Shelf.Lists.Categories(
                        id = "genres",
                        title = "Genres",
                        list = genresList.map {
                            Shelf.Category(
                                id = it.lowercase().replace(" ", ""),
                                title = it,
                                feed = createGenreFeed(it),
                            )
                        },
                        more = genresListFull.map {
                            Shelf.Category(
                                id = it.lowercase().replace(" ", ""),
                                title = it,
                                feed = createGenreFeed(it),
                            )
                        }.toFeed(),
                        type = Shelf.Lists.Type.Grid
                    )
                },
            ).awaitAll()
        }.toFeed()
    }
}