package dev.brahmkshatriya.echo.extension.dto.types

import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    val id: String,
    val parent: String? = null,
    val isDir: String,

    val title: String,
    val album: String? = null,
    val artist: String? = null,
    val track: Int? = null,
    val duration: Int? = null, // in seconds

    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,

    val contentType: String? = null,
)