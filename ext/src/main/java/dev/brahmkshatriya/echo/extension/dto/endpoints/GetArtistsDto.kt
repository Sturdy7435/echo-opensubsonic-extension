package dev.brahmkshatriya.echo.extension.dto.endpoints

import dev.brahmkshatriya.echo.extension.dto.types.ErrorDto
import dev.brahmkshatriya.echo.extension.dto.types.IndexDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetArtistsDto(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicResponseDto
) {
    @Serializable
    data class SubsonicResponseDto(
        val status: String,
        val error: ErrorDto? = null,

        val artists: ArtistsDto? = null,
    ) {
        @Serializable
        data class ArtistsDto (
            val index: List<IndexDto>? = null,
        )
    }
}