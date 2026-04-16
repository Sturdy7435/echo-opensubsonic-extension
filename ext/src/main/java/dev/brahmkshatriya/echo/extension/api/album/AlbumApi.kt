package dev.brahmkshatriya.echo.extension.api.album

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.api.request.parseAs
import dev.brahmkshatriya.echo.extension.api.request.runRequest
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetAlbumDto

suspend fun getAlbum(album: Album): Album {
    val albumData = runRequest(
        authenticatedRequest(
            endpoint = "getAlbum",
            parameters = mapOf(
                "id" to album.id
            ),
        )
    ).parseAs<GetAlbumDto>().subsonicResponse

    return albumData.album!!.toAlbum()
}