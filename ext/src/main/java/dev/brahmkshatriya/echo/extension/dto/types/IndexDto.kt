package dev.brahmkshatriya.echo.extension.dto.types

import kotlinx.serialization.Serializable

@Serializable
data class IndexDto(
    val name: String,
    val artist: List<ArtistDto>? = null,
)