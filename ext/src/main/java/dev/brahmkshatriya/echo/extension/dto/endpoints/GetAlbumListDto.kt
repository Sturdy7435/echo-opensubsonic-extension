package dev.brahmkshatriya.echo.extension.dto.endpoints

import dev.brahmkshatriya.echo.extension.dto.types.AlbumDto
import dev.brahmkshatriya.echo.extension.dto.types.ErrorDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetAlbumListDto(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicResponseDto
) {
    @Serializable
    data class SubsonicResponseDto(
        val status: String,
        val error: ErrorDto? = null,

        val albumList2: AlbumListDto? = null,
    ) {
        @Serializable
        data class AlbumListDto (
            val album: List<AlbumDto>? = null,
        )
    }
}
