package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.api.album.AlbumListType
import dev.brahmkshatriya.echo.extension.api.album.getAlbumList
import dev.brahmkshatriya.echo.extension.api.artist.getArtists
import dev.brahmkshatriya.echo.extension.api.genre.getGenres
import dev.brahmkshatriya.echo.extension.api.track.getRandomTracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

suspend fun createHomeFeed(): Feed<Shelf> {
    return withContext(Dispatchers.IO) {
        listOf(
            async {
                Shelf.Lists.Items(
                    id = "randomTracks",
                    title = "Random Tracks",
                    list = getRandomTracks(),
                    type = Shelf.Lists.Type.Linear,
                )
            },
            async {
                val pageSize = 20
                val albumListFull = PagedData.Continuous { continuation ->
                    val contInt = continuation?.toIntOrNull() ?: 0

                    val albums: List<Shelf> =
                        getAlbumList(AlbumListType.AlphabeticalByName, pageSize, contInt)
                            .map { it.toShelf() }

                    if (albums.size < pageSize) Page(albums, null)
                    else Page(albums, (contInt + pageSize).toString())
                }.toFeed()
                val albumList: List<Album> = getAlbumList(AlbumListType.Random, 10)

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
                            feed = null,
                        )
                    },
                    more = genresListFull.map {
                        Shelf.Category(
                            id = it.lowercase().replace(" ", ""),
                            title = it,
                            feed = null,
                        )
                    }.toFeed(),
                    type = Shelf.Lists.Type.Grid
                )
            },
        ).awaitAll()
    }.toFeed()
}
