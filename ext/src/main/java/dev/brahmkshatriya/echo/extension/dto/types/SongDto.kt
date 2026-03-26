package dev.brahmkshatriya.echo.extension.dto.types

import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.api.login.checkAuth
import dev.brahmkshatriya.echo.extension.toNetworkRequest
import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    val id: String,
    val parent: String? = null,
    val isDir: Boolean,

    val title: String,
    val album: String? = null,
    val artist: String? = null,
    val track: Int? = null,
    val duration: Int? = null, // in seconds
    val bitRate: Int? = null,

    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,

    val contentType: String? = null,
) {
    fun toTrack(): Track {
        checkAuth()
        return Track(
            id = id,
            title = title,
            type = Track.Type.Song,
            streamables = listOf(
                Streamable.server(
                    id = id,
                    quality = bitRate ?: 0,
                    title = "${bitRate}kbps",
                )
            ),
            cover =
                if (coverArt == null) {
                    null
                } else {
                    ImageHolder.NetworkRequestImageHolder(
                        authenticatedRequest(
                            endpoint = "getCoverArt",
                            parameters = mapOf(
                                "id" to coverArt,
                            ),
                        ).toNetworkRequest(),
                        crop = false,
                    )
                },
            duration = duration?.times(1000)?.toLong(),
            genres = listOf(genre ?: ""),
            albumOrderNumber = track?.toLong(),
            extras = mapOf(
                "coverArtID" to coverArt,
            ).mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
        )
    }
}