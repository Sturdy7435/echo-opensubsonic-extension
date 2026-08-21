package dev.brahmkshatriya.echo.extension.dto.types

import dev.brahmkshatriya.echo.common.models.Date
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.toNetworkRequest
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.OffsetDateTime

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val coverArt: String? = null,

    val comment: String? = null,
    val public: Boolean = false,
    val songCount: Long? = null,
    val duration: Long? = null, // In seconds
    val created: String? = null, // ISO8601
    val readonly: Boolean = false,

    val entry: List<SongDto>? = null,
) {
    fun toPlaylist(): Playlist {
        return Playlist(
            id = id,
            title = name,
            isEditable = !readonly,
            isPrivate = !public,
            cover = coverArt?.let {
                ImageHolder.NetworkRequestImageHolder(
                    request = authenticatedRequest(
                        endpoint = "getCoverArt",
                        parameters = listOf("id" to it),
                        needsGet = true,
                    ).toNetworkRequest(),
                    crop = false,
                )
            },
            trackCount = songCount,
            duration = duration?.times(1000),
            creationDate = created?.let {
                val date = runCatching {
                    OffsetDateTime.parse(it).toLocalDate()
                }.getOrElse { _ -> LocalDate.parse(it) }
                Date(
                    year = date.year,
                    month = date.monthValue,
                    day = date.dayOfMonth,
                )
            },
            description = comment,
            isRadioSupported = false,
            isLikeable = true,
            isShareable = true,
        )
    }
}