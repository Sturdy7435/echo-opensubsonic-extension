package dev.brahmkshatriya.echo.extension.dto.endpoints

import dev.brahmkshatriya.echo.extension.dto.types.ErrorDto
import dev.brahmkshatriya.echo.extension.dto.types.ShareDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateShareDto (
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicResponseDto,
) {
    @Serializable
    data class SubsonicResponseDto(
        val status: String,
        val error: ErrorDto? = null,

        val shares: SharesDto? = null,
    ) {
        @Serializable
        data class SharesDto (
            val share: List<ShareDto>? = null
        )
    }
}