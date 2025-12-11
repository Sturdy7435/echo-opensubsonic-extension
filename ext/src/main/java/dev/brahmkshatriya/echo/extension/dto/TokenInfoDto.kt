package dev.brahmkshatriya.echo.extension.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenInfoDto(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicResponseDto
) {
    @Serializable
    data class SubsonicResponseDto(
        val status: String,
        val error: ErrorDto? = null,

        val tokenInfo: TokenDto? = null,
    ) {
        @Serializable
        data class TokenDto(
            val username: String,
        )
    }
}