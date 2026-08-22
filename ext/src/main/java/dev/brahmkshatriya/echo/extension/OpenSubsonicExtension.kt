package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.extension.clients.album.AlbumClientImpl
import dev.brahmkshatriya.echo.extension.clients.artist.ArtistClientImpl
import dev.brahmkshatriya.echo.extension.clients.extension.ExtensionClientImpl
import dev.brahmkshatriya.echo.extension.clients.follow.FollowClientImpl
import dev.brahmkshatriya.echo.extension.clients.homefeed.HomeFeedClientImpl
import dev.brahmkshatriya.echo.extension.clients.libraryfeed.LibraryFeedClientImpl
import dev.brahmkshatriya.echo.extension.clients.like.LikeClientImpl
import dev.brahmkshatriya.echo.extension.clients.login.LoginClientImpl
import dev.brahmkshatriya.echo.extension.clients.playlist.PlaylistCombinedClient
import dev.brahmkshatriya.echo.extension.clients.playlist.PlaylistCombinedClientImpl
import dev.brahmkshatriya.echo.extension.clients.radio.RadioClientImpl
import dev.brahmkshatriya.echo.extension.clients.searchfeed.SearchFeedClientImpl
import dev.brahmkshatriya.echo.extension.clients.share.ShareClientImpl
import dev.brahmkshatriya.echo.extension.clients.track.TrackClientImpl

class OpenSubsonicExtension :
    ExtensionClient by ExtensionClientImpl(),
    LoginClient.CustomInput by LoginClientImpl(),

    HomeFeedClient by HomeFeedClientImpl(),
    SearchFeedClient by SearchFeedClientImpl(),
    LibraryFeedClient by LibraryFeedClientImpl(),

    TrackClient by TrackClientImpl(),
    AlbumClient by AlbumClientImpl(),
    PlaylistCombinedClient by PlaylistCombinedClientImpl(),
    ArtistClient by ArtistClientImpl(),
    RadioClient by RadioClientImpl(),

    FollowClient by FollowClientImpl(),
    LikeClient by LikeClientImpl(),
    ShareClient by ShareClientImpl()
