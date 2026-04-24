package dev.brahmkshatriya.echo.extension.dto.endpoints

import dev.brahmkshatriya.echo.extension.dto.types.ErrorDto
import dev.brahmkshatriya.echo.extension.dto.types.SongDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetSongsByGenreDto(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicResponseDto,
) {
    @Serializable
    data class SubsonicResponseDto(
        val status: String,
        val error: ErrorDto? = null,

        val songsByGenre: SongsByGenreDto? = null,
    ) {
        @Serializable
        data class SongsByGenreDto(
            val song: List<SongDto>,
        )
    }
}