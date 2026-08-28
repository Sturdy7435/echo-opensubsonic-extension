package dev.brahmkshatriya.echo.extension.service.library

import dev.brahmkshatriya.echo.extension.dto.endpoints.GetScanStatusDto
import dev.brahmkshatriya.echo.extension.service.request.RequestService.authenticatedRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.parseAs
import dev.brahmkshatriya.echo.extension.service.request.RequestService.runRequest
import dev.brahmkshatriya.echo.extension.service.request.RequestService.throwOnError

object LibraryService {
    suspend fun isScanning(): Boolean {
        val scanningData = runRequest(
            authenticatedRequest(
                endpoint = "getScanStatus",
                parameters = listOf(),
            )
        ).parseAs<GetScanStatusDto>().subsonicResponse
        if (scanningData.status != "ok") {
            throwOnError(scanningData.error)
        }

        return scanningData.scanStatus?.scanning ?: false
    }

    suspend fun startScan() {
        if (isScanning()) return

        val scanData = runRequest(
            authenticatedRequest(
                endpoint = "startScan",
                parameters = listOf(),
            )
        ).parseAs<GetScanStatusDto>().subsonicResponse
        if (scanData.status != "ok") {
            throwOnError(scanData.error)
        }
    }
}