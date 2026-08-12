package dev.brahmkshatriya.echo.extension.clients.searchfeed

import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.dto.endpoints.SearchDto
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError

class SearchFeedClientImpl : SearchFeedClient {
    override suspend fun loadSearchFeed(query: String): Feed<Shelf> {

        val data: SearchResult = if (query.isBlank()) {
            search("", -1, -1, -1)
        } else {
            search(query, 20, 20, 20)
        }

        return Feed(
            listOf("Tracks", "Albums", "Artists").map { Tab(it, it) },
        ) { tab ->
            val pagedData: PagedData.Single<Shelf> = when (tab?.id) {
                "Tracks" -> PagedData.Single { data.tracks?.map { it.toShelf() as Shelf } ?: listOf() }
                "Albums" -> PagedData.Single { data.albums?.map { it.toShelf() as Shelf } ?: listOf() }
                "Artists" -> PagedData.Single { data.artists?.map { it.toShelf() as Shelf } ?: listOf() }
                else -> throw IllegalArgumentException("Unknown tab")
            }
            pagedData.toFeedData()
        }
    }

    suspend fun search(
        query: String,
        trackCount: Int,
        albumCount: Int,
        artistCount: Int,
    ): SearchResult {
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

        return SearchResult(
            tracks = searchData.searchResult3?.song?.map { it.toTrack() },
            albums = searchData.searchResult3?.album?.map { it.toAlbum() },
            artists = searchData.searchResult3?.artist?.map { it.toArtist() },
        )
    }

    data class SearchResult(
        val tracks: List<Track>?,
        val albums: List<Album>?,
        val artists: List<Artist>?,
    )
}
