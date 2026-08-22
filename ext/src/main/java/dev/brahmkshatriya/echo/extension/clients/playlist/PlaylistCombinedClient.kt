package dev.brahmkshatriya.echo.extension.clients.playlist

import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditPrivacyClient

/**
 * Allows to easily implement PlaylistEditCoverClient or similar in case OpenSubsonic start
 * supporting them, while keeping `OpenSubsonicExtension.kt` clean.
 */
interface PlaylistCombinedClient : PlaylistClient, PlaylistEditClient, PlaylistEditPrivacyClient
