package dev.brahmkshatriya.echo.extension.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginDto(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicResponseDto
)

@Serializable
data class SubsonicResponseDto(
    val user: UserDto? = null,
    val error: ErrorDto? = null,
)

@Serializable
data class UserDto(
    val username: String,
    val email: String? = null
)

@Serializable
data class ErrorDto(
    val code: Int,
    val message: String,
)