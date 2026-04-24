package dev.brahmkshatriya.echo.extension.clients.extension

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.service.session.SettingsSession

class ExtensionClientImpl : ExtensionClient {
    override suspend fun getSettingItems(): List<Setting> {
        return SettingsSession.items
    }

    override fun setSettings(settings: Settings) {
        SettingsSession.current = settings
    }
}