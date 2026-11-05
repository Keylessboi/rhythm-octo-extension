// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.offline

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.Normalizer

/**
 * Fork (Octo): hearting a preview is a request to fetch it, not a favourite: Navidrome has never seen the preview's
 * id, and once the file lands (after Octo's rescan) it is an ordinary library song that is present but not starred.
 * A [Grab] remembers the request; [OctoGrabMatcher] recognises the landed song, and the worker hearts that one.
 */
data class Grab(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val requestedAtMs: Long,
)

/** A song the server returned when asked for a grab's title. */
data class GrabCandidate(val id: String, val title: String, val artist: String, val album: String, val isExternal: Boolean)

object OctoGrabMatcher {
    /** A request older than this is given up on: the grab failed or the song is not coming. */
    const val EXPIRY_MS = 7L * 24 * 60 * 60 * 1000

    fun expired(grab: Grab, nowMs: Long): Boolean = nowMs - grab.requestedAtMs > EXPIRY_MS

    /** The owned (non-preview) candidate that is the grabbed song; the one on the same album wins over a reissue. */
    fun match(grab: Grab, candidates: List<GrabCandidate>): GrabCandidate? {
        val same = candidates.filter {
            !it.isExternal && it.id != grab.songId &&
                norm(it.title) == norm(grab.title) && norm(it.artist) == norm(grab.artist)
        }
        return same.firstOrNull { norm(it.album) == norm(grab.album) } ?: same.firstOrNull()
    }

    fun norm(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
}

/** The pending grabs, in a file of the app's own. */
class OctoGrabStore(private val file: File) {
    private val gson = Gson()
    private val type = object : TypeToken<List<Grab>>() {}.type

    @Synchronized
    fun all(): List<Grab> = runCatching {
        if (!file.exists()) emptyList() else gson.fromJson<List<Grab>>(file.readText(), type).orEmpty()
    }.getOrDefault(emptyList())

    @Synchronized
    fun add(grab: Grab) = write(all().filter { it.songId != grab.songId } + grab)

    @Synchronized
    fun remove(songId: String) = write(all().filter { it.songId != songId })

    private fun write(list: List<Grab>) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(gson.toJson(list))
        if (!tmp.renameTo(file)) { file.writeText(gson.toJson(list)); tmp.delete() }
    }
}

object OctoGrabs {
    private const val TAG = "OctoGrabs"
    private val lock = Any()

    private fun store(context: android.content.Context) =
        OctoGrabStore(java.io.File(context.applicationContext.filesDir, "octo_grabs.json"))

    /** Called when the user hearts [song]: a preview is remembered as a pending grab, anything else is ignored. */
    fun noteHeart(context: android.content.Context, song: chromahub.rhythm.app.features.streaming.domain.model.StreamingSong?) {
        if (song == null || !song.isExternal) return
        synchronized(lock) { store(context).add(Grab(song.id, song.title, song.artist, song.album, System.currentTimeMillis())) }
    }

    /** Searches for each pending grab; the landed song is hearted and the grab forgotten, an old one is dropped. */
    suspend fun resolve(context: android.content.Context, repo: chromahub.rhythm.app.features.streaming.domain.repository.StreamingMusicRepository) {
        val grabs = synchronized(lock) { store(context).all() }
        if (grabs.isEmpty()) return
        val client = chromahub.rhythm.app.features.streaming.data.provider.SubsonicApiClient(context.applicationContext)
        val now = System.currentTimeMillis()
        for (grab in grabs) {
            if (OctoGrabMatcher.expired(grab, now)) { synchronized(lock) { store(context).remove(grab.songId) }; continue }
            val found = client.searchSongs("${grab.artist} ${grab.title}", 20).getOrNull() ?: continue
            val match = OctoGrabMatcher.match(grab, found.map {
                GrabCandidate("SUBSONIC::${it.providerId}", it.title, it.artist, it.album, it.isExternal)
            }) ?: continue
            runCatching {
                repo.likeSong(match.id)
                OfflineWiring.requestFavoriteSync(context)
                synchronized(lock) { store(context).remove(grab.songId) }
                android.util.Log.d(TAG, "hearted the landed song ${match.id} for ${grab.songId}")
            }
        }
    }
}
