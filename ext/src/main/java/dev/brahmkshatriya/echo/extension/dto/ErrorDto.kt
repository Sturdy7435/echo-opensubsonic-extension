package dev.brahmkshatriya.echo.extension.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorDto(
    val code: Int,
    val message: String?,
)