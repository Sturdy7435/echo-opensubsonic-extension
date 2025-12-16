package dev.brahmkshatriya.echo.extension.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetOpenSubsonicExtensionsDto(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicResponseDto
) {
    @Serializable
    data class SubsonicResponseDto(
        val status: String,
        val error: ErrorDto? = null,

        val openSubsonicExtensions: List<ExtensionDto>? = null,
    ) {
        @Serializable
        data class ExtensionDto(
            val name: String,
            val versions: List<Int>,
        )
    }
}