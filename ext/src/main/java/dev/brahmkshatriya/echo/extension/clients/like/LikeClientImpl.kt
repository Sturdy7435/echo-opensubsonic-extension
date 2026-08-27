package dev.brahmkshatriya.echo.extension.clients.like

import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetAlbumDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetSongDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetStarredDto
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError
import dev.brahmkshatriya.echo.extension.service.search.SearchService.SearchResult

class LikeClientImpl : LikeClient {
    override suspend fun likeItem(
        item: EchoMediaItem,
        shouldLike: Boolean,
    ) {
        if (item !is Track && item !is Album) {
            return
        }

        runRequest(
            authenticatedRequest(
                endpoint = if (shouldLike) "star" else "unstar",
                parameters = if (item is Track) {
                    listOf(
                        "id" to item.id,
                    )
                } else { // item is Album
                    listOf(
                        "albumId" to item.id,
                    )
                },
            ),
        )
    }

    override suspend fun isItemLiked(item: EchoMediaItem): Boolean {
        when (item) {
            is Track -> {
                val trackData = runRequest(
                    authenticatedRequest(
                        endpoint = "getSong",
                        parameters = listOf(
                            "id" to item.id,
                        ),
                    ),
                ).parseAs<GetSongDto>().subsonicResponse
                if (trackData.status != "ok") {
                    throwOnError(trackData.error)
                }

                return trackData.song!!.starred != null
            }

            is Album -> {
                val albumData = runRequest(
                    authenticatedRequest(
                        endpoint = "getAlbum",
                        parameters = listOf(
                            "id" to item.id,
                        ),
                    ),
                ).parseAs<GetAlbumDto>().subsonicResponse
                if (albumData.status != "ok") {
                    throwOnError(albumData.error)
                }

                return albumData.album!!.starred != null
            }

            else -> return false
        }
    }

    companion object {
        suspend fun getStarred(): SearchResult {
            val starredData = runRequest(
                authenticatedRequest(
                    endpoint = "getStarred2",
                    parameters = listOf(),
                ),
            ).parseAs<GetStarredDto>().subsonicResponse
            if (starredData.status != "ok") {
                throwOnError(starredData.error)
            }

            return SearchResult(
                tracks = starredData.starred2?.song?.map { it.toTrack() },
                albums = starredData.starred2?.album?.map { it.toAlbum() },
                artists = starredData.starred2?.artist?.map { it.toArtist() },
            )
        }
    }
}