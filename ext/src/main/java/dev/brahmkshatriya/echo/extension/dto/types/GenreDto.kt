package dev.brahmkshatriya.echo.extension.dto.types

import kotlinx.serialization.Serializable

@Serializable
data class GenreDto (
    val value: String,
    val albumCount: Int,
    val songCount: Int,
)