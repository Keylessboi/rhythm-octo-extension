// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import org.json.JSONArray
import org.json.JSONObject

/** One imported list (a Spotify playlist, a link, a file) and how much of it the library already has. */
data class ImportList(
    val id: String,
    val name: String,
    val source: String,
    val total: Int,
    val have: Int,
    val missing: Int,
    val queued: Int,
    val downloading: Int,
    val getMissing: Boolean,
    val keepPlaylist: Boolean,
) {
    /** How much of it the library has, 0 to 1. */
    val fraction: Float get() = if (total <= 0) 0f else (have.toFloat() / total).coerceIn(0f, 1f)

    /** Songs on their way: waiting in the trickle or being fetched. */
    val coming: Int get() = queued + downloading

    /** A list read through a Spotify account sign-in: it stays as it was last read. */
    val fromSignIn: Boolean get() = source == "spotifyLiked" || source == "spotifyPlaylist"
}

data class ImportsOverview(
    val lists: List<ImportList>,
    val trickleState: String,
    val trickleQueued: Int,
    val current: String?,
    val libraryProblem: String?,
)

data class ImportAnswer(val ok: Boolean, val message: String, val listId: String?)

object OctoImportParsers {
    private fun JSONObject.str(name: String): String? = if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun objects(value: Any?): List<JSONObject> = when (value) {
        is JSONArray -> (0 until value.length()).mapNotNull { value.optJSONObject(it) }
        is JSONObject -> listOf(value)
        else -> emptyList()
    }

    fun overview(o: JSONObject): ImportsOverview {
        val trickle = o.optJSONObject("trickle")
        val current = trickle?.optJSONObject("current")
        return ImportsOverview(
            lists = objects(o.opt("lists")).map(::list),
            trickleState = trickle?.optString("state").orEmpty(),
            trickleQueued = trickle?.optInt("queued") ?: 0,
            current = current?.let { "${it.optString("artist")} – ${it.optString("title")}" },
            libraryProblem = o.str("libraryProblem"),
        )
    }

    fun list(o: JSONObject) = ImportList(
        id = o.optString("id"),
        name = o.optString("name"),
        source = o.optString("source"),
        total = o.optInt("total"),
        have = o.optInt("have"),
        missing = o.optInt("missing"),
        queued = o.optInt("queued"),
        downloading = o.optInt("downloading"),
        getMissing = o.optBoolean("getMissing"),
        keepPlaylist = o.optBoolean("keepPlaylist"),
    )

    fun answer(o: JSONObject) = ImportAnswer(o.optBoolean("ok"), o.optString("message"), o.str("listId"))

    /** One line for what the trickle is doing, in plain words. */
    fun trickleLine(o: ImportsOverview): String? = when {
        o.current != null -> "Fetching: ${o.current}"
        o.trickleQueued > 0 -> "${o.trickleQueued} waiting to be fetched"
        else -> null
    }
}
