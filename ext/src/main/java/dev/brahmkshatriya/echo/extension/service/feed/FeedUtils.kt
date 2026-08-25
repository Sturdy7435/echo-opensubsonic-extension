package dev.brahmkshatriya.echo.extension.service.feed

import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

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

    /**
     * Runs multiple Shelf-producing suspend blocks concurrently on the IO thread,
     * waits for all of them to complete, and combines the results into a feed.
     */
    suspend fun concurrentFeed(
        vararg tasks: suspend () -> Shelf,
    ): Feed<Shelf> {
        return withContext(Dispatchers.IO) {
            if (tasks.size == 1)
                tasks.map { it() }.toFeed()
            else
                tasks.map { async { it() } }.awaitAll().toFeed()
        }
    }

    fun continuousFeed(
        pageSize: Int,
        callback: suspend(offset: Int) -> List<Shelf>
    ): Feed<Shelf> {
        return PagedData.Continuous { continuation ->
            val contInt = continuation?.toIntOrNull() ?: 0
            val things: List<Shelf> = callback(contInt)

            Page(
                things,
                if (things.size < pageSize) null else (contInt + pageSize).toString()
            )
        }.toFeed()
    }
}