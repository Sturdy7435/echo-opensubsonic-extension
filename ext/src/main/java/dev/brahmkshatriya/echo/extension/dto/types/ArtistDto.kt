package dev.brahmkshatriya.echo.extension.dto.types

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.toNetworkRequest
import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val coverArt: String? = null,

    val albumCount: Int? = null,
    val album: List<AlbumDto>? = null,
) {
    fun toArtist(): Artist {
        return Artist(
            id = id,
            name = name,
            cover = coverArt?.let {
                ImageHolder.NetworkRequestImageHolder(
                    request = authenticatedRequest(
                        endpoint = "getCoverArt",
                        parameters = mapOf("id" to it),
                    ).toNetworkRequest(),
                    crop = false,
                )
            },
        )
    }
}
