// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import android.content.Context
import chromahub.rhythm.app.features.streaming.data.provider.SubsonicApiClient

/**
 * Fork (Octo): the calls only an Octo server answers. [supports] says whether the server it is pointed at is one;
 * every other call fails cleanly against a plain Navidrome.
 */
class OctoClient(private val api: SubsonicApiClient) {
    constructor(context: Context) : this(SubsonicApiClient(context.applicationContext))

    suspend fun extensions(): Result<Map<String, Set<Int>>> =
        api.call("getOpenSubsonicExtensions").map { OctoParsers.extensions(it.optJSONArray("openSubsonicExtensions")) }

    suspend fun supports(name: String, version: Int = 1): Boolean =
        extensions().getOrNull()?.get(name)?.let { version in it } ?: false

    suspend fun acquisitions(): Result<List<Acquisition>> =
        api.call("getAcquisitions").map { OctoParsers.acquisitions(it.optJSONObject("acquisitions")) }

    suspend fun acquisition(key: String): Result<Acquisition> =
        api.call("getAcquisition", mapOf("key" to key)).map { OctoParsers.acquisition(it.getJSONObject("acquisition")) }

    suspend fun clearAcquisitions(key: String? = null): Result<Int> =
        api.call("clearAcquisitions", key?.let { mapOf("key" to it) }.orEmpty())
            .map { it.optJSONObject("cleared")?.optInt("count") ?: 0 }

    /** Starts a Soulseek/Lidarr search for one song; [FoundSongs.done] says when the copies are all in. */
    suspend fun findSongs(id: String): Result<FoundSongs> =
        api.call("findSongs", mapOf("id" to id)).map { OctoParsers.foundSongs(it.getJSONObject("foundSongs")) }

    suspend fun foundSongs(search: String): Result<FoundSongs> =
        api.call("getFoundSongs", mapOf("search" to search)).map { OctoParsers.foundSongs(it.getJSONObject("foundSongs")) }

    suspend fun pick(search: String, copy: FoundCopy): Result<PickResult> {
        val which = copy.id?.let { "copy" to it } ?: copy.index?.let { "candidate" to it.toString() }
            ?: return Result.failure(IllegalArgumentException("This copy cannot be picked. Search again."))
        return api.call("pickFoundSong", mapOf("search" to search, which.first to which.second))
            .map { OctoParsers.pick(it.getJSONObject("pick")) }
    }
}
