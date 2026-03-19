package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.NetworkRequest
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit.MINUTES
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.text.Charsets.UTF_8

val rng = SecureRandom()
fun generateSalt(length: Int = 8): String {
    val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '-' + '_'

    return buildString(length) {
        repeat(length) {
            val char = charPool[rng.nextInt(charPool.size)]
            append(char)
        }
    }
}

fun computeToken(password: String, salt: String): String {
    val md = MessageDigest.getInstance("MD5")
    val input = (password + salt).toByteArray(UTF_8)

    return md.digest(input).joinToString("") {
        "%02x".format(it)
    }
}

val DEFAULT_CACHE_CONTROL = CacheControl.Builder().maxAge(10, MINUTES).build()
val DEFAULT_HEADERS = Headers.Builder().build()
val DEFAULT_BODY: RequestBody = FormBody.Builder().build()

fun getRequest(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request {
    return Request.Builder()
        .url(url)
        .headers(headers)
        .cacheControl(cache)
        .build()
}

fun postRequest(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request {
    return Request.Builder()
        .url(url)
        .post(body)
        .headers(headers)
        .cacheControl(cache)
        .build()
}

fun Request.toNetworkRequest(): NetworkRequest {
    return NetworkRequest(
        url = this.url.toString(),
        headers = buildMap {
            headers.forEach { put(it.first, it.second) }
        },
        method = NetworkRequest.Method.valueOf(this.method),
        body = this.body?.toString()?.toByteArray(),
    )
}