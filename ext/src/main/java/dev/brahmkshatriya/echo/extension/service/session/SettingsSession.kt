package dev.brahmkshatriya.echo.extension.service.session

import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.Settings

object SettingsSession {
    var current: Settings? = null

    val forceGetRequests get() = current?.getBoolean("force_get_requests") ?: false

    val items: List<Setting> = listOf(
        SettingSwitch(
            title = "Force GET requests",
            key = "force_get_requests",
            summary = "Whether to force usage of GET requests even if POST is supported, may be useful for debugging",
            defaultValue = forceGetRequests,
        ),
    )
}