package dev.brahmkshatriya.echo.extension.dto.types

import kotlinx.serialization.Serializable

@Serializable
data class ErrorDto(
    val code: Int,
    val message: String?,
    val helpUrl: String?,
)