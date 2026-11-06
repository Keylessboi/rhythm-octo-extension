// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.offline

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/** A play that finished while the server could not be reached; it keeps the time it was really played. */
data class PendingScrobble(val songId: String, val playedAtMs: Long)

/**
 * Fork: plays made offline (a subway ride) are sent to the server later with their own timestamps, so the server's play
 * counts, "recently played" and Octo's recommendations see them. Kept in a small file next to the other offline state.
 */
class ScrobbleQueue(private val file: File) {
    private val gson = Gson()
    private val type = object : TypeToken<List<PendingScrobble>>() {}.type

    @Synchronized
    fun all(): List<PendingScrobble> = runCatching {
        if (!file.exists()) emptyList() else gson.fromJson<List<PendingScrobble>>(file.readText(), type).orEmpty()
    }.getOrDefault(emptyList())

    @Synchronized
    fun add(songId: String, playedAtMs: Long) {
        val queued = all()
        // The player reports one stop twice a moment apart; that is one play.
        if (queued.any { it.songId == songId && kotlin.math.abs(it.playedAtMs - playedAtMs) < DUPLICATE_WINDOW_MS }) return
        write((queued + PendingScrobble(songId, playedAtMs)).takeLast(MAX))
    }

    /** Sends each play through [submit], oldest first; what it reports as sent is dropped, the rest stays queued. */
    suspend fun flush(stopOnFailure: Boolean = false, submit: suspend (songId: String, playedAtMs: Long) -> Boolean) {
        for (play in all()) {
            if (submit(play.songId, play.playedAtMs)) remove(play) else if (stopOnFailure) return
        }
    }

    @Synchronized
    private fun remove(play: PendingScrobble) {
        val rest = all().toMutableList()
        if (rest.remove(play)) write(rest)
    }

    private fun write(list: List<PendingScrobble>) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(gson.toJson(list))
        if (!tmp.renameTo(file)) { file.writeText(gson.toJson(list)); tmp.delete() }
    }

    companion object {
        const val MAX = 2000
        private const val DUPLICATE_WINDOW_MS = 5_000L
    }
}

/** Reports plays through [submit]; a play that cannot be sent goes in the queue, and a good connection drains it. */
class Scrobbles(context: android.content.Context, private val submit: suspend (songId: String, playedAtMs: Long) -> Boolean) {
    private val queue = ScrobbleQueue(File(context.applicationContext.filesDir, "scrobble_queue.json"))

    suspend fun report(songId: String, playedAtMs: Long): Boolean {
        val sent = runCatching { submit(songId, playedAtMs) }.onFailure { android.util.Log.w(TAG, "scrobble failed", it) }.getOrDefault(false)
        android.util.Log.i(TAG, "scrobble $songId sent=$sent queued=${queue.all().size}")
        if (sent) {
            flush()
            return true
        }
        queue.add(songId, playedAtMs)
        return false
    }

    suspend fun flush() = queue.flush(stopOnFailure = true) { id, at ->
        runCatching { submit(id, at) }.onFailure { android.util.Log.w(TAG, "flush failed", it) }.getOrDefault(false)
            .also { android.util.Log.i(TAG, "flush $id sent=$it") }
    }

    private companion object {
        const val TAG = "Scrobbles"
    }
}
