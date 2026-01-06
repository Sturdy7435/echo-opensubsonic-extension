package dev.brahmkshatriya.echo.extension.dto.types

import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.OpenSubsonicApi
import dev.brahmkshatriya.echo.extension.toNetworkRequest
import kotlinx.serialization.Serializable

val api by lazy { OpenSubsonicApi() }

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

    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,

    val contentType: String? = null,
) {
    fun toTrack(): Track {
        return Track(
            id = id,
            title = title,
            type = Track.Type.Song,
            cover =
                if (coverArt == null) {
                    null
                } else {
                    ImageHolder.NetworkRequestImageHolder(
                        api.authenticatedRequest(
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