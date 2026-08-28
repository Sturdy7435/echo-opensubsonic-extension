package dev.brahmkshatriya.echo.extension.service.session

import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingCategory
import dev.brahmkshatriya.echo.common.settings.SettingOnClick
import dev.brahmkshatriya.echo.common.settings.SettingSlider
import dev.brahmkshatriya.echo.common.settings.SettingSwitch
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.service.library.LibraryService.startScan

object SettingsSession {
    var current: Settings? = null

    val searchResults get() = current?.getInt("search_results") ?: 20
    val forceGetRequests get() = current?.getBoolean("force_get_requests") ?: false

    val items: List<Setting> = listOf(
        SettingCategory(
            title = "Server",
            key = "category_server",
            items = listOf(
                SettingOnClick(
                    title = "Rescan libraries",
                    key = "initiate_rescan",
                    summary = "Initiates a rescan of the media libraries on the server, if the " +
                            "user has permission to do so.",
                    onClick = { startScan() },
                ),
            )
        ),
        SettingCategory(
            title = "Feed",
            key = "category_feed",
            items = listOf(
                SettingSlider(
                    title = "Search results",
                    key = "search_results",
                    summary = "Maximum amount of results to show in each search tab.",
                    from = 1,
                    to = 50,
                    defaultValue = 20,
                ),
            )
        ),
        SettingCategory(
            title = "Advanced",
            key = "category_advanced",
            items = listOf(
                SettingSwitch(
                    title = "Force GET requests",
                    key = "force_get_requests",
                    summary = "Whether to force usage of GET requests even if POST is supported",
                    defaultValue = forceGetRequests,
                ),
            )
        )
    )
}