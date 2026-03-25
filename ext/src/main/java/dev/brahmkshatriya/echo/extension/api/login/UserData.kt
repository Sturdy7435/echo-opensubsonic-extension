package dev.brahmkshatriya.echo.extension.api.login

import dev.brahmkshatriya.echo.common.models.ImageHolder
import java.util.EnumSet
import kotlin.collections.joinToString

data class Server(
    val url: String,
    val extensions: EnumSet<Extension>?
) {
    enum class Extension(val id: String) {
        ApiKeyAuthentication("apiKeyAuthentication"),
        GetPodcastEpisode("getPodcastEpisode"),
        FormPost("formPost"),
        IndexBasedQueue("indexBasedQueue"),
        SongLyrics("songLyrics"),
        Template("template"),
        TranscodeOffset("transcodeOffset"),
        Transcoding("transcoding");

        companion object {
            private val ID_TO_NAME: Map<String, Extension> = entries.associateBy { it.id }

            fun serialize(extensions: EnumSet<Extension>?): String? {
                if (extensions == null) {
                    return null
                }
                return extensions.joinToString(",") { it.id }
            }

            fun deserialize(s: String?): EnumSet<Extension>? {
                if (s == null) {
                    return null
                }

                val set = EnumSet.noneOf(Extension::class.java)
                if (s.isNotBlank()) {
                    s.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .mapNotNull { id -> ID_TO_NAME[id] }
                        .forEach { set.add(it) }
                }
                return set
            }
        }
    }
}

data class UserData(
    val username: String,
    val email: String?,
    val avatar: ImageHolder?,
    val server: Server?,
    val password: String?,
    val apiKey: String?,
) {
    companion object {
        val EMPTY = UserData("", null, null, null, null, null)
    }
}