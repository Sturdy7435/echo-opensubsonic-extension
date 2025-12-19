package dev.brahmkshatriya.echo.extension.dto.endpoints

import dev.brahmkshatriya.echo.extension.dto.types.ErrorDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginDto(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicResponseDto
) {
    @Serializable
    data class SubsonicResponseDto(
        val status: String,
        val error: ErrorDto? = null,

        val user: UserDto? = null,
    ) {
        @Serializable
        data class UserDto(
            val username: String,
            val email: String? = null,
        )
    }
}