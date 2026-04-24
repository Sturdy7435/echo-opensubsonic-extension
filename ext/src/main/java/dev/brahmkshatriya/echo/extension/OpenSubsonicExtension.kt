package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.extension.clients.album.AlbumClientImpl
import dev.brahmkshatriya.echo.extension.clients.artist.ArtistClientImpl
import dev.brahmkshatriya.echo.extension.clients.extension.ExtensionClientImpl
import dev.brahmkshatriya.echo.extension.clients.homefeed.HomeFeedClientImpl
import dev.brahmkshatriya.echo.extension.clients.login.LoginClientImpl
import dev.brahmkshatriya.echo.extension.clients.track.TrackClientImpl

class OpenSubsonicExtension :
    ExtensionClient by ExtensionClientImpl(),
    LoginClient.CustomInput by LoginClientImpl(),

    HomeFeedClient by HomeFeedClientImpl(),

    AlbumClient by AlbumClientImpl(),
    ArtistClient by ArtistClientImpl(),
    TrackClient by TrackClientImpl()