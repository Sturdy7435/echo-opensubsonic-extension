package dev.brahmkshatriya.echo.extension

object UserSession {
    @Volatile
    var current: UserData = UserData.EMPTY
}