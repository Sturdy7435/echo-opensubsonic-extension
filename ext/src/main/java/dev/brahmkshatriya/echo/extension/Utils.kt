package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit.MINUTES
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.text.Charsets.UTF_8

@OptIn(ExperimentalSerializationApi::class)
object KebabCaseToCamelCase : JsonNamingStrategy {
    override fun serialNameForJson(
        descriptor: SerialDescriptor,
        elementIndex: Int,
        serialName: String,
    ): String {
        val parts = serialName.split('-')
        return buildString {
            append(parts[0].lowercase())
            for (i in 1 until parts.size) {
                val p = parts[i]
                if (p.isNotEmpty()) append(p.replaceFirstChar { it.uppercaseChar() })
            }
        }
    }
}

val rng = SecureRandom()
fun generateSalt(length: Int = 8): String {
    val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '-' + '_'

    return buildString(length) {
        for (i in 0 until length) {
            val char = charPool[rng.nextInt(charPool.size)]
            append(char)
        }
    }
}

fun computeToken(password: String, salt: String): String {
    val md = MessageDigest.getInstance("MD5")
    val input = (password + salt).toByteArray(UTF_8)

    return md.digest(input).toString()
}

val DEFAULT_CACHE_CONTROL = CacheControl.Builder().maxAge(10, MINUTES).build()
val DEFAULT_HEADERS = Headers.Builder().build()
val DEFAULT_BODY: RequestBody = FormBody.Builder().build()

suspend fun OkHttpClient.get(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Response {
    return newCall(
        Request.Builder()
            .url(url)
            .headers(headers)
            .cacheControl(cache)
            .build(),
    ).await()
}

/*
suspend fun OkHttpClient.post(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Response {
    return newCall(
        Request.Builder()
            .url(url)
            .post(body)
            .headers(headers)
            .cacheControl(cache)
            .build(),
    ).await()
}
*/