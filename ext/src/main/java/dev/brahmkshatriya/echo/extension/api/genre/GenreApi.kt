package dev.brahmkshatriya.echo.extension.api.genre

import dev.brahmkshatriya.echo.extension.api.request.authenticatedRequest
import dev.brahmkshatriya.echo.extension.api.request.parseAs
import dev.brahmkshatriya.echo.extension.api.request.runRequest
import dev.brahmkshatriya.echo.extension.dto.endpoints.GetGenresDto

suspend fun getGenres(): List<String> {
    val genresData = runRequest(
        authenticatedRequest(
            endpoint = "getGenres",
            parameters = mapOf(),
        )
    ).parseAs<GetGenresDto>().subsonicResponse

    return genresData.genres?.genre?.map { it.name } ?: listOf()
}