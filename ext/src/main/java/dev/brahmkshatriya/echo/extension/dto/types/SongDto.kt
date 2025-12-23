package dev.brahmkshatriya.echo.extension.dto.types

import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    val id: String,
    val parent: String? = null,
    val isDir: Boolean,

    val title: String,
    val album: String? = null,
    val artist: String? = null,
    val track: Int? = null,
    val duration: Int? = null, // in seconds

    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,

    val contentType: String? = null,
) {
    fun toTrack(): Track {
        return Track(
            id = id,
            title = title,
            type = Track.Type.Song,
            cover = null, // cover retrieval should be handled separately
            duration = duration?.times(1000)?.toLong(),
            genres = listOf(genre ?: ""),
            albumOrderNumber = track?.toLong(),
        )
    }
}