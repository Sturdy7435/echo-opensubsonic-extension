package dev.brahmkshatriya.echo.extension.clients.libraryfeed

import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.extension.clients.like.LikeClientImpl.Companion.getStarred
import dev.brahmkshatriya.echo.extension.clients.playlist.PlaylistCombinedClientImpl.Companion.getPlaylists

class LibraryFeedClientImpl : LibraryFeedClient {
    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        return Feed(
            tabs = listOf("Playlists", "Liked").map { Tab(it, it) },
        ) { tab ->
            val pagedData: PagedData.Single<Shelf> = when (tab?.id) {
                "Playlists" -> PagedData.Single {
                    getPlaylists().map { it.toShelf() }
                }

                "Liked" -> PagedData.Single {
                    val starred = getStarred()

                    listOf(
                        Shelf.Lists.Items(
                            id = "followedArtists",
                            title = "Followed artists",
                            list = starred.artists?.take(6) ?: emptyList(),
                            more = starred.artists?.map { it.toShelf() }?.toFeed(),
                            type = Shelf.Lists.Type.Grid,
                        ),
                        Shelf.Lists.Items(
                            id = "likedAlbums",
                            title = "Liked albums",
                            list = starred.albums?.take(6) ?: emptyList(),
                            more = starred.albums?.map { it.toShelf() }?.toFeed(),
                            type = Shelf.Lists.Type.Grid,
                        ),
                        Shelf.Lists.Items(
                            id = "likedTracks",
                            title = "Liked tracks",
                            list = starred.tracks?.take(6) ?: emptyList(),
                            more = starred.tracks?.map { it.toShelf() }?.toFeed(),
                            type = Shelf.Lists.Type.Grid,
                        ),
                    )
                }

                else -> throw IllegalArgumentException("Unknown tab")
            }
            pagedData.toFeedData(buttons = Feed.Buttons(showSearch = false, showSort = false))
        }
    }
}