package dev.brahmkshatriya.echo.extension.models

import java.util.EnumSet

data class ServerData(
    val url: String,
    val extensions: EnumSet<Extension>?,
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
            val ID_TO_NAME: Map<String, Extension> = entries.associateBy { it.id }
            val EMPTY: EnumSet<Extension> = EnumSet.noneOf(Extension::class.java)

            fun serialize(extensions: EnumSet<Extension>?): String? {
                return extensions?.joinToString(",") { it.id }
            }

            fun deserialize(s: String?): EnumSet<Extension>? {
                return s?.let { str ->
                    Extension.EMPTY.apply {
                        if (str.isNotBlank()) {
                            str.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .mapNotNull { id -> ID_TO_NAME[id] }
                                .forEach { add(it) }
                        }
                    }
                }
            }
        }
    }
}