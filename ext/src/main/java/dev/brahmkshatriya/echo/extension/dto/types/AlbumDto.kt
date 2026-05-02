package dev.brahmkshatriya.echo.extension.dto.types

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Date
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.toNetworkRequest
import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val id: String,
    val name: String,
    val coverArt: String? = null,

    val isCompilation: Boolean = false,
    val artists: List<ArtistDto>? = null,
    val songCount: Long? = null,
    val duration: Long? = null, // In seconds
    val releaseData: DateDto? = null,
    val year: Int? = null,
    val recordLabels: List<RecordLabelDto>? = null,
    val explicitStatus: String? = null,
    val version: String? = null,
    val starred: String? = null,

    val song: List<SongDto>? = null,
) {
    fun toAlbum(): Album {
        return Album(
            id = id,
            title = name,
            type = if (isCompilation) {
                Album.Type.Compilation
            } else {
                songCount?.let {
                    when {
                        it == 1L -> Album.Type.Single
                        it in 3L..6L -> Album.Type.EP
                        it >= 7L -> Album.Type.LP
                        else -> null
                    }
                }
            },
            cover = coverArt?.let {
                ImageHolder.NetworkRequestImageHolder(
                    request = authenticatedRequest(
                        endpoint = "getCoverArt",
                        parameters = mapOf("id" to it),
                        needsGet = true,
                    ).toNetworkRequest(),
                    crop = false,
                )
            },
            artists = artists?.map { it.toArtist() } ?: listOf(),
            trackCount = songCount,
            duration = duration?.times(1000),
            releaseDate = releaseData?.let {
                Date(year = it.year, month = it.month, day = it.day)
            } ?: year?.let {
                Date(year = it)
            },
            label = recordLabels?.joinToString(", ") { it.name },
            isExplicit = explicitStatus.equals("explicit"),
            subtitle = version,
            isLikeable = true,
            isShareable = true,
        )
    }
}