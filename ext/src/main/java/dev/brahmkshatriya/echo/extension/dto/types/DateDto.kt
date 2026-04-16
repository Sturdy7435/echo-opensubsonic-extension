package dev.brahmkshatriya.echo.extension.dto.types

import kotlinx.serialization.Serializable

@Serializable
data class DateDto(
    val year: Int,
    val month: Int,
    val day: Int,
)