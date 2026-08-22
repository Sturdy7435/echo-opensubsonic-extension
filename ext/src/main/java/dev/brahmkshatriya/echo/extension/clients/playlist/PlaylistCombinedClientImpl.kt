package dev.brahmkshatriya.echo.extension.clients.playlist

import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.dto.endpoints.CreatePlaylistDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.DeletePlaylistDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetPlaylistDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetPlaylistsDto
import dev.brahmkshatriya.echo.extension.dto.endpoints.UpdatePlaylistDto
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError
import kotlin.math.max
import kotlin.math.min

class PlaylistCombinedClientImpl : PlaylistCombinedClient {

    // PlaylistClient implementation

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        val playlistData = runRequest(
            authenticatedRequest(
                endpoint = "getPlaylist",
                parameters = listOf(
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
                parameters = listOf(
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

    // PlaylistEditClient implementation

    // HACK: track is not checked for.
    override suspend fun listEditablePlaylists(track: Track?): List<Pair<Playlist, Boolean>> {
        return getPlaylists().filter { it.isEditable }.map { it to true }
    }

    override suspend fun createPlaylist(
        title: String,
        description: String?,
    ): Playlist {
        val playlistData = runRequest(
            authenticatedRequest(
                endpoint = "createPlaylist",
                parameters = listOf(
                    "name" to title,
                ),
            ),
        ).parseAs<CreatePlaylistDto>().subsonicResponse
        if (playlistData.status != "ok") {
            throwOnError(playlistData.error)
        }

        description?.let {
            val updateData = runRequest(
                authenticatedRequest(
                    endpoint = "updatePlaylist",
                    parameters = listOf(
                        "playlistId" to playlistData.playlist!!.id,
                        "comment" to it,
                    ),
                ),
            ).parseAs<UpdatePlaylistDto>().subsonicResponse
            if (updateData.status != "ok") {
                throwOnError(updateData.error)
            }
        }

        return playlistData.playlist!!.toPlaylist().copy(
            description = description,
        )
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        val deleteData = runRequest(
            authenticatedRequest(
                endpoint = "deletePlaylist",
                parameters = listOf(
                    "id" to playlist.id,
                ),
            ),
        ).parseAs<DeletePlaylistDto>().subsonicResponse
        if (deleteData.status != "ok") {
            throwOnError(deleteData.error)
        }
    }

    override suspend fun editPlaylistMetadata(
        playlist: Playlist,
        title: String,
        description: String?,
    ) {
        val updateData = runRequest(
            authenticatedRequest(
                endpoint = "updatePlaylist",
                parameters = listOf(
                    "playlistId" to playlist.id,
                    "name" to title,
                    "comment" to (description ?: ""),
                ),
            ),
        ).parseAs<UpdatePlaylistDto>().subsonicResponse
        if (updateData.status != "ok") {
            throwOnError(updateData.error)
        }
    }

    override suspend fun addTracksToPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        index: Int,
        new: List<Track>,
    ) {
        val updateData = runRequest(
            authenticatedRequest(
                endpoint = "updatePlaylist",
                parameters = buildList {
                    add("playlistId" to playlist.id)

                    new.forEach {
                        add("songIdToAdd" to it.id)
                    }
                    tracks.drop(index).forEach {
                        add("songIdToAdd" to it.id)
                    }

                    (index until tracks.size).forEach {
                        add("songIndexToRemove" to it.toString())
                    }
                },
            ),
        ).parseAs<UpdatePlaylistDto>().subsonicResponse
        if (updateData.status != "ok") {
            throwOnError(updateData.error)
        }
    }

    override suspend fun removeTracksFromPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        indexes: List<Int>,
    ) {
        val updateData = runRequest(
            authenticatedRequest(
                endpoint = "updatePlaylist",
                parameters = buildList {
                    add("playlistId" to playlist.id)

                    indexes.forEach {
                        add("songIndexToRemove" to it.toString())
                    }
                },
            ),
        ).parseAs<UpdatePlaylistDto>().subsonicResponse
        if (updateData.status != "ok") {
            throwOnError(updateData.error)
        }
    }

    override suspend fun moveTrackInPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (fromIndex == toIndex) return

        val transactionStart = min(fromIndex, toIndex)
        val transactionEnd = max(fromIndex, toIndex)

        val updateData = runRequest(
            authenticatedRequest(
                endpoint = "updatePlaylist",
                parameters = buildList {
                    add("playlistId" to playlist.id)

                    if (fromIndex < toIndex) {
                        // Move forward
                        tracks.slice(transactionStart + 1..transactionEnd).forEach {
                            add("songIdToAdd" to it.id)
                        }
                        add("songIdToAdd" to tracks[fromIndex].id)
                    } else {
                        // Move backwards
                        add("songIdToAdd" to tracks[fromIndex].id)
                        tracks.slice(transactionStart until transactionEnd).forEach {
                            add("songIdToAdd" to it.id)
                        }
                    }
                    tracks.drop(transactionEnd + 1).forEach {
                        add("songIdToAdd" to it.id)
                    }

                    (transactionStart..tracks.lastIndex).forEach {
                        add("songIndexToRemove" to it.toString())
                    }
                },
            ),
        ).parseAs<UpdatePlaylistDto>().subsonicResponse
        if (updateData.status != "ok") {
            throwOnError(updateData.error)
        }
    }

    // PlaylistEditPrivacyClient implementation

    override suspend fun setPrivacy(
        playlist: Playlist,
        isPrivate: Boolean,
    ) {
        val updateData = runRequest(
            authenticatedRequest(
                endpoint = "updatePlaylist",
                parameters = listOf(
                    "playlistId" to playlist.id,
                    "public" to (!isPrivate).toString(),
                ),
            ),
        ).parseAs<UpdatePlaylistDto>().subsonicResponse
        if (updateData.status != "ok") {
            throwOnError(updateData.error)
        }
    }

    companion object {
        suspend fun getPlaylists(): List<Playlist> {
            val playlistsData = runRequest(
                authenticatedRequest(
                    endpoint = "getPlaylists",
                    parameters = listOf(),
                ),
            ).parseAs<GetPlaylistsDto>().subsonicResponse
            if (playlistsData.status != "ok") {
                throwOnError(playlistsData.error)
            }

            return playlistsData.playlists?.playlist?.map { it.toPlaylist() } ?: listOf()
        }
    }
}