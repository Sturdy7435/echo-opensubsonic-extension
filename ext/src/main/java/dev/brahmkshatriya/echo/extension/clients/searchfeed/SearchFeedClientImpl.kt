package dev.brahmkshatriya.echo.extension.clients.searchfeed

import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.extension.service.genre.GenreService.createGenreFeed
import dev.brahmkshatriya.echo.extension.service.genre.GenreService.getGenres
import dev.brahmkshatriya.echo.extension.service.search.SearchService
import dev.brahmkshatriya.echo.extension.service.search.SearchService.search

class SearchFeedClientImpl : SearchFeedClient {
    override suspend fun loadSearchFeed(query: String): Feed<Shelf> {
        val data: SearchService.SearchResult = if (query.isBlank()) {
            search("", -1, -1, -1)
        } else {
            search(query, 20, 20, 20)
        }

        val genresData = if (query.isBlank()) {
            getGenres()
        } else {
            getGenres().filter { it.contains(query.trim(), ignoreCase = true) }
        }

        return Feed(
            listOf("Tracks", "Albums", "Artists", "Genres").map { Tab(it, it) },
        ) { tab ->
            val pagedData: PagedData.Single<Shelf> = when (tab?.id) {
                "Tracks" -> PagedData.Single {
                    data.tracks?.map { it.toShelf() as Shelf } ?: emptyList()
                }

                "Albums" -> PagedData.Single {
                    data.albums?.map { it.toShelf() as Shelf } ?: emptyList()
                }

                "Artists" -> PagedData.Single {
                    data.artists?.map { it.toShelf() as Shelf } ?: emptyList()
                }

                "Genres" -> PagedData.Single {
                    genresData.map {
                        Shelf.Category(
                            id = it.lowercase().replace(" ", ""),
                            title = it,
                            feed = createGenreFeed(it),
                        )
                    }
                }

                else -> throw IllegalArgumentException("Unknown tab")
            }
            pagedData.toFeedData()
        }
    }
}
