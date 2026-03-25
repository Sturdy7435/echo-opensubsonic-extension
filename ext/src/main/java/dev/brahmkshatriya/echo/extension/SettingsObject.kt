package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.Settings

object SettingsObject {
    var current: Settings? = null

    val forceGetRequests get() = current?.getBoolean("force_get_requests") ?: false

    val items: List<Setting> = listOf(
        SettingSwitch(
            title = "Force GET requests",
            key = "force_get_requests",
            summary = "Force usage of GET requests even if POST is supported, may expose authentication data in server logs",
            defaultValue = forceGetRequests,
        )
    )
}