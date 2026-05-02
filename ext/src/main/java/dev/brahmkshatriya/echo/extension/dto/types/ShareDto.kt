package dev.brahmkshatriya.echo.extension.dto.types

import kotlinx.serialization.Serializable

@Serializable
data class ShareDto(
    val id: String,
    val url: String,
    val description: String?,
)