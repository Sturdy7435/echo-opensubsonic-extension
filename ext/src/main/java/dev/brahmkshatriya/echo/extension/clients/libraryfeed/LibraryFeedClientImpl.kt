package dev.brahmkshatriya.echo.extension.clients.libraryfeed

import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.clients.playlist.PlaylistCombinedClientImpl.Companion.getPlaylists

class LibraryFeedClientImpl : LibraryFeedClient {
    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        return getPlaylists().map { it.toShelf() }.toFeed()
    }
}