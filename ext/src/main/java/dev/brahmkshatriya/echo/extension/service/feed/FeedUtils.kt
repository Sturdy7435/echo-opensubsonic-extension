package dev.brahmkshatriya.echo.extension.service.feed

import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object FeedUtils {
    /**
     * Runs multiple Shelf-producing suspend blocks concurrently on the IO thread,
     * waits for all of them to complete, and returns non-null results as a list.
     */
    suspend fun concurrentShelves(
        vararg tasks: suspend () -> Shelf?,
    ): List<Shelf> {
        return withContext(Dispatchers.IO) {
            if (tasks.size == 1)
                tasks.mapNotNull { it() }
            else
                tasks.map { async { it() } }.awaitAll().filterNotNull()
        }
    }

    /**
     * Runs multiple Shelf-producing suspend blocks concurrently on the IO thread,
     * waits for all of them to complete, and combines non-null results into a feed.
     */
    suspend fun concurrentFeed(
        vararg tasks: suspend () -> Shelf?,
    ): Feed<Shelf> {
        return concurrentShelves(*tasks).toFeed()
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