package dev.brahmkshatriya.echo.extension.tabs

import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
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
        ).awaitAll()
    }.toFeed()
}
