package dev.brahmkshatriya.echo.extension.dto.types

import dev.brahmkshatriya.echo.common.models.Date
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.toNetworkRequest
import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    val id: String,
    val parent: String? = null,
    val isDir: Boolean,

    val title: String,
    val album: String? = null,
    val artists: List<ArtistDto>? = null,
    val artist: String? = null,
    val track: Int? = null,
    val discNumber: Int? = null,
    val duration: Int? = null, // in seconds
    val bitRate: Int? = null,

    val year: Int? = null,
    val genres: List<GenreDto>? = null,
    val explicitStatus: String? = null,
    val isrc: List<String>? = null,
    val coverArt: String? = null,

    val contentType: String? = null,
) {
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
                        parameters = mapOf("id" to it),
                    ).toNetworkRequest(),
                    crop = false,
                )
            },
            artists = artists?.map { it.toArtist() } ?: listOf(),
            album = null,
            duration = duration?.let { (it * 1000).toLong() },
            releaseDate = year?.let { Date(year = it) },
            genres = genres?.map { it.name } ?: listOf(),
            isrc = isrc?.firstOrNull(),
            albumOrderNumber = track?.toLong(),
            albumDiscNumber = discNumber?.toLong(),
            isExplicit = explicitStatus in EXPLICIT_VALUES,
            extras = mapOf(
                "coverArtID" to coverArt,
            ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap(),
            streamables = listOf(
                Streamable.server(
                    id = id,
                    quality = bitRate ?: 0,
                    title = "${bitRate ?: 0}kbps",
                )
            )
        )
    }
}