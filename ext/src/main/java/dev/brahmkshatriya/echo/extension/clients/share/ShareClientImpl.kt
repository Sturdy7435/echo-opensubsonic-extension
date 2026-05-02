package dev.brahmkshatriya.echo.extension.clients.share

import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.dto.endpoints.CreateShareDto
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError
import kotlin.time.Clock

class ShareClientImpl : ShareClient {
    override suspend fun onShare(item: EchoMediaItem): String {
        if (item !is Track && item !is Album) {
            throw UnsupportedOperationException(
                "OpenSubsonic servers can only share tracks and albums",
            )
        }

        val shareData = runRequest(
            authenticatedRequest(
                endpoint = "createShare",
                parameters = mapOf(
                    "id" to item.id,
                    "description" to item.title,
                    "expires" to (Clock.System.now().toEpochMilliseconds() * 31_536_000_000L)
                        .toString(), // The share will last 365 days
                ),
            ),
        ).parseAs<CreateShareDto>().subsonicResponse
        if (shareData.status != "ok") {
            throwOnError(shareData.error)
        }

        return shareData.shares?.share?.getOrNull(0)?.url
            ?: throw Exception("The server did not create a share, try again")
    }
}