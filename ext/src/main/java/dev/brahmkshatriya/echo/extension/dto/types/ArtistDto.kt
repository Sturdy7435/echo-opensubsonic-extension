package dev.brahmkshatriya.echo.extension.dto.types

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.toNetworkRequest
import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val coverArt: String? = null,

    val albumCount: Int? = null,
    val album: List<AlbumDto>? = null,
    val starred: String? = null,
) {
    fun toArtist(): Artist {
        return Artist(
            id = id,
            name = name,
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
            isRadioSupported = true,
            isFollowable = true,
            isShareable = false,
            isLikeable = false,
        )
    }
}
