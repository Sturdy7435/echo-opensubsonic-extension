package dev.brahmkshatriya.echo.extension.dto.types

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Date
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.toNetworkRequest
import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    val id: String,
    val isDir: Boolean,
    //val parent: String? = null,

    val title: String,
    val album: String? = null,
    val albumId: String? = null,
    val artists: List<ArtistDto>? = null,
    val track: Int? = null,
    val discNumber: Int? = null,
    val duration: Int? = null, // in seconds
    val bitRate: Int? = null,
    val year: Int? = null,
    val genres: List<ItemGenre>? = null,
    val explicitStatus: String? = null,
    val isrc: List<String>? = null,
    val coverArt: String? = null,
    val contentType: String? = null,
    val starred: String? = null,
) {
    @Serializable
    data class ItemGenre(
        val name: String,
    )

    companion object {
        private val EXPLICIT_VALUES = setOf("explicit", "1", "4")
    }

    fun toTrack(): Track {
        return Track(
            id = id,
            title = title,
            type = Track.Type.Song,
            cover = coverArt?.let {
                ImageHolder.NetworkRequestImageHolder(
                    request = authenticatedRequest(
                        endpoint = "getCoverArt",
                        parameters = listOf("id" to it),
                        needsGet = true,
                    ).toNetworkRequest(),
                    crop = false,
                )
            },
            artists = artists?.map { it.toArtist() } ?: emptyList(),
            album = albumId?.let { album?.let { name -> Album(id = it, title = name) } },
            duration = duration?.let { (it * 1000).toLong() },
            releaseDate = year?.let { Date(year = it) },
            genres = genres?.map { it.name } ?: emptyList(),
            isrc = isrc?.firstOrNull(),
            albumOrderNumber = track?.toLong(),
            albumDiscNumber = discNumber?.toLong(),
            isExplicit = explicitStatus in EXPLICIT_VALUES,
            streamables = buildList {
                add(
                    Streamable.server(
                        id = id,
                        quality = Int.MAX_VALUE, // Quality is just for sorting
                        title = if (contentType == "audio/flac") "FLAC" else "${bitRate ?: 0}kbps",
                    ),
                )
                bitRate?.let {
                    if (it > 320) {
                        add(
                            Streamable.server(
                                id = id,
                                quality = 320,
                                title = "320kbps • Transcoding",
                            ),
                        )
                    }
                    if (it > 128) {
                        add(
                            Streamable.server(
                                id = id,
                                quality = 128,
                                title = "128kbps • Transcoding",
                            ),
                        )
                    }
                }
            },
            isRadioSupported = true,
            isLikeable = true,
            isShareable = true,
        )
    }
}