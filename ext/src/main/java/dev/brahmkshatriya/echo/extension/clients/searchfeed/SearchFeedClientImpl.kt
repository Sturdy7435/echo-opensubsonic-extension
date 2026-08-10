package dev.brahmkshatriya.echo.extension.clients.searchfeed

import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.extension.dto.endpoints.SearchDto
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError

class SearchFeedClientImpl : SearchFeedClient {
    override suspend fun loadSearchFeed(query: String): Feed<Shelf> {
        /*return Feed(
            listOf("Tracks", "Albums", "Artists").map { Tab(it, it) }
        ) { tab ->
            val pagedData: PagedData.Single<EchoMediaItem> = when (tab?.id) {
                "Tracks" -> TODO()
                "Albums" -> TODO()
                "Artists" -> TODO()
                else -> throw IllegalArgumentException("Unknown tab")
            }

        }*/
        return if (query.isBlank()) {
            search("", -1, -1, -1)
        } else {
            search(query, 20, 20, 20)
        }.map { it.toShelf() }.toFeed()
    }

    suspend fun search(
        query: String,
        trackCount: Int,
        albumCount: Int,
        artistCount: Int,
    ): List<EchoMediaItem> {
        val searchData = runRequest(
            authenticatedRequest(
                endpoint = "search3",
                parameters = mapOf(
                    "query" to query,
                    "songCount" to trackCount.toString(),
                    "albumCount" to albumCount.toString(),
                    "artistCount" to artistCount.toString(),
                ),
            ),
        ).parseAs<SearchDto>().subsonicResponse
        if (searchData.status != "ok") {
            throwOnError(searchData.error)
        }

        return listOf(
            searchData.searchResult3?.song?.map { it.toTrack() } ?: listOf(),
            searchData.searchResult3?.album?.map { it.toAlbum() } ?: listOf(),
            searchData.searchResult3?.artist?.map { it.toArtist() } ?: listOf(),
        ).flatten()
    }
}
