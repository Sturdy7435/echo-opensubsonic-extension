package dev.brahmkshatriya.echo.extension.service.feed

import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll

object FeedUtils {

    /**
     * Awaits a list of Deferred Shelf.Lists, discards those whose `list` is empty,
     * and returns a Feed containing only the non‑empty shelves.
     */
    suspend fun nonEmptyShelvesToFeed(shelves: List<Deferred<Shelf.Lists<out EchoMediaItem>>>):
            Feed<Shelf> {
        return shelves
            .awaitAll()
            .filter { it.list.isNotEmpty() }
            .map { it as Shelf }
            .toFeed()
    }
}