package dev.brahmkshatriya.echo.extension.service.search

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.dto.endpoints.SearchDto
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError

object SearchService {
    suspend fun search(
        query: String,
        trackCount: Int,
        albumCount: Int,
        artistCount: Int,
    ): SearchResult {
        val searchData = runRequest(
            authenticatedRequest(
                endpoint = "search3",
                parameters = listOf(
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