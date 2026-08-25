package dev.brahmkshatriya.echo.extension.clients.homefeed

import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.clients.album.AlbumClientImpl.Companion.AlbumListType
import dev.brahmkshatriya.echo.extension.clients.album.AlbumClientImpl.Companion.getAlbumList
import dev.brahmkshatriya.echo.extension.clients.artist.ArtistClientImpl.Companion.getArtists
import dev.brahmkshatriya.echo.extension.clients.track.TrackClientImpl.Companion.getRandomTracks
import dev.brahmkshatriya.echo.extension.service.feed.FeedUtils.concurrentFeed
import dev.brahmkshatriya.echo.extension.service.feed.FeedUtils.continuousFeed
import dev.brahmkshatriya.echo.extension.service.genre.GenreService.createGenreFeed
import dev.brahmkshatriya.echo.extension.service.genre.GenreService.getGenres

class HomeFeedClientImpl : HomeFeedClient {
    private val pageSize = 20
    private val listSize = 10
    private val gridSize = 8

    override suspend fun loadHomeFeed(): Feed<Shelf> {
        return concurrentFeed(
            {
                Shelf.Lists.Items(
                    id = "randomTracks",
                    title = "Random Tracks",
                    list = getRandomTracks(listSize * 2),
                    type = Shelf.Lists.Type.Linear,
                )
            },
            {
                val albumList = getAlbumList(AlbumListType.Random, listSize)
                val albumListFull = continuousFeed(pageSize) { offset ->
                    getAlbumList(
                        type = AlbumListType.AlphabeticalByName,
                        count = pageSize,
                        offset = offset,
                    ).map { it.toShelf() }
                }

                Shelf.Lists.Items(
                    id = "albums",
                    title = "Albums",
                    list = albumList,
                    more = albumListFull,
                    type = Shelf.Lists.Type.Linear,
                )
            },
            {
                val artistListFull = getArtists()
                val artistList = artistListFull.shuffled().take(listSize)
                val artistFeed: Feed<Shelf> = artistListFull.map { it.toShelf() }.toFeed()

                Shelf.Lists.Items(
                    id = "artists",
                    title = "Artists",
                    list = artistList,
                    more = artistFeed,
                    type = Shelf.Lists.Type.Linear,
                )
            },
            {
                val genresListFull: List<String> = getGenres()
                val genresList: List<String> = genresListFull.shuffled().take(gridSize)
                fun toCategory(x: List<String>) = x.map {
                    Shelf.Category(
                        id = it.lowercase().replace(" ", ""),
                        title = it,
                        feed = createGenreFeed(it),
                    )
                }

                Shelf.Lists.Categories(
                    id = "genres",
                    title = "Genres",
                    list = toCategory(genresList),
                    more = toCategory(genresListFull).toFeed(),
                    type = Shelf.Lists.Type.Grid,
                )
            },
        )
    }
}