package dev.brahmkshatriya.echo.extension.api

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extension.api.album.getAlbum
import dev.brahmkshatriya.echo.extension.api.artist.createArtistFeed
import dev.brahmkshatriya.echo.extension.api.artist.getArtist
import dev.brahmkshatriya.echo.extension.api.login.getUserSession
import dev.brahmkshatriya.echo.extension.api.login.keyLogin
import dev.brahmkshatriya.echo.extension.api.login.passwordLogin
import dev.brahmkshatriya.echo.extension.api.login.setUserSession
import dev.brahmkshatriya.echo.extension.api.track.getStreamableMedia
import dev.brahmkshatriya.echo.extension.api.track.getTrack

class OpenSubsonicApi {
    suspend fun onPasswordLogin(data: Map<String, String?>): List<User> {
        return passwordLogin(data)
    }

    suspend fun onKeyLogin(data: Map<String, String?>): List<User> {
        return keyLogin(data)
    }

    fun setUser(user: User?) {
        setUserSession(user)
    }

    fun getUser(): User? {
        return getUserSession()
    }

    fun loadTrack(track: Track): Track {
        return getTrack(track)
    }

    fun loadStreamableMedia(streamable: Streamable, isDownload: Boolean): Streamable.Media {
        return getStreamableMedia(streamable, isDownload)
    }

    suspend fun loadArtist(artist: Artist): Artist {
        return getArtist(artist)
    }

    suspend fun loadArtistFeed(artist: Artist): Feed<Shelf> {
        return createArtistFeed(artist)
    }

    suspend fun loadAlbum(album: Album): Album {
        return getAlbum(album)
    }
}
