package dev.brahmkshatriya.echo.extension.dto.endpoints

import dev.brahmkshatriya.echo.extension.dto.types.AlbumDto
import dev.brahmkshatriya.echo.extension.dto.types.ArtistDto
import dev.brahmkshatriya.echo.extension.dto.types.ErrorDto
import dev.brahmkshatriya.echo.extension.dto.types.SongDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchDto(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicResponseDto,
) {
    @Serializable
    data class SubsonicResponseDto(
        val status: String,
        val error: ErrorDto? = null,
        val searchResult3: SearchResultDto? = null,
    ) {
        @Serializable
        data class SearchResultDto(
            val artist: List<ArtistDto>? = null,
            val album: List<AlbumDto>? = null,
            val song: List<SongDto>? = null,
        )
    }
}