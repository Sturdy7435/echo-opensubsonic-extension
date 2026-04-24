package dev.brahmkshatriya.echo.extension.models

import dev.brahmkshatriya.echo.common.models.ImageHolder

data class UserData(
    val username: String,
    val email: String?,
    val avatar: ImageHolder?,
    val server: ServerData?,
    val password: String?,
    val apiKey: String?,
) {
    companion object {
        val EMPTY = UserData("", null, null, null, null, null)
    }
}