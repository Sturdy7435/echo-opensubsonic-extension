package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.OpenSubsonicExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

context(ext: OpenSubsonicExtension)
suspend fun createHomeFeed(): Feed<Shelf> {
    return withContext(Dispatchers.IO) {
        listOf(
            async { ext.api.getRandomTracks() },
        ).awaitAll()
    }.toFeed()
}
