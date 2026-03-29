package dev.brahmkshatriya.echo.extension.dto.types

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.toNetworkRequest
import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val id: String,
    val name: String,
    val coverArt: String? = null,
) {
    fun toAlbum(): Album {
        return Album(
            id = id,
            title = name,
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