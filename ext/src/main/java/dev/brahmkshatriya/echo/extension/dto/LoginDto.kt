package dev.brahmkshatriya.echo.extension.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginDto(
    val subsonicResponse: SubsonicResponseDto
) {
    @Serializable
    class SubsonicResponseDto(
        val user: UserDto
    ) {
        @Serializable
        class UserDto(
            val username: String,
            val email: String
        )
    }
}