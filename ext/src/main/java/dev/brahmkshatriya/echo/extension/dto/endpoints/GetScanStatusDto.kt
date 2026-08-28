package dev.brahmkshatriya.echo.extension.dto.endpoints

import dev.brahmkshatriya.echo.extension.dto.types.ErrorDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Also used for the startScan endpoint
@Serializable
data class GetScanStatusDto(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicResponseDto,
) {
    @Serializable
    data class SubsonicResponseDto(
        val status: String,
        val error: ErrorDto? = null,

        val scanStatus: ScanStatusDto? = null,
    ) {
        @Serializable
        data class ScanStatusDto(
            val scanning: Boolean,
        )
    }
}