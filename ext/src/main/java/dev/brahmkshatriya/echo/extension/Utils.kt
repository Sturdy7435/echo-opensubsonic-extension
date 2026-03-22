package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.NetworkRequest
import okhttp3.Request
import okhttp3.RequestBody

fun RequestBody.toByteArray(): ByteArray {
    val buffer = okio.Buffer()
    this.writeTo(buffer)
    return buffer.readByteArray()
}

fun Request.toNetworkRequest(): NetworkRequest {
    return NetworkRequest(
        url = url.toString(),
        headers = buildMap {
            headers.forEach { put(it.first, it.second) }
        },
        method = NetworkRequest.Method.valueOf(method),
        body = body?.toByteArray(),
    )
}