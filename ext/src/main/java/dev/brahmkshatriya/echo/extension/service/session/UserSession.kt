package dev.brahmkshatriya.echo.extension.service.session

import dev.brahmkshatriya.echo.extension.models.UserData

object UserSession {
    @Volatile
    var currentUser: UserData = UserData.EMPTY
}