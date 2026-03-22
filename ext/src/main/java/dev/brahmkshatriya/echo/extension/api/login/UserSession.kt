package dev.brahmkshatriya.echo.extension.api.login

object UserSession {
    @Volatile
    var current: UserData = UserData.EMPTY
}