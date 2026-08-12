package dev.brahmkshatriya.echo.extension.clients.playlist

import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetPlaylistDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetPlaylistsDto
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError

class PlaylistClientImpl : PlaylistClient {
    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        val playlistData = runRequest(
            authenticatedRequest(
                endpoint = "getPlaylist",
                parameters = mapOf(
                    "id" to playlist.id,
                ),
            ),
        ).parseAs<GetPlaylistDto>().subsonicResponse
        if (playlistData.status != "ok") {
            throwOnError(playlistData.error)
        }

        return playlistData.playlist!!.toPlaylist()
    }

    override suspend fun loadTracks(playlist: Playlist): Feed<Track> {
        val playlistData = runRequest(
            authenticatedRequest(
                endpoint = "getPlaylist",
                parameters = mapOf(
                    "id" to playlist.id,
                ),
            ),
        ).parseAs<GetPlaylistDto>().subsonicResponse
        if (playlistData.status != "ok") {
            throwOnError(playlistData.error)
        }

        return (playlistData.playlist!!.entry?.map { it.toTrack() } ?: listOf()).toFeed()
    }

    // There is nothing to show under the list of songs
    override suspend fun loadFeed(playlist: Playlist): Feed<Shelf>? {
        return null
    }

    companion object {
        suspend fun getPlaylists(): List<Playlist> {
            val playlistsData = runRequest(
                authenticatedRequest(
                    endpoint = "getPlaylists",
                    parameters = mapOf(),
                ),
            ).parseAs<GetPlaylistsDto>().subsonicResponse
            if (playlistsData.status != "ok") {
                throwOnError(playlistsData.error)
            }

            return playlistsData.playlists?.playlist?.map { it.toPlaylist() } ?: listOf()
        }
    }
}