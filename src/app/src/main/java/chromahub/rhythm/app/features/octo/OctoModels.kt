// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import org.json.JSONArray
import org.json.JSONObject

/*
 * Fork (Octo): the extra Subsonic calls an Octo server answers, as winters27/octo-player (GPL-3.0) uses them.
 * The wire shapes follow that project's client and fixtures; only what Rhythm shows is read.
 */

const val OCTO_ACQUISITIONS = "octoAcquisitions"
const val OCTO_IMPORTS = "octoImports"
const val OCTO_STATIONS = "octoStations"

enum class AcquisitionStage(val wire: String) {
    Queued("queued"), Searching("searching"), Downloading("downloading"), Verifying("verifying"),
    Importing("importing"), Done("done"), Failed("failed"), Unknown("");

    val finished: Boolean get() = this == Done || this == Failed

    companion object {
        fun of(text: String?): AcquisitionStage {
            val wire = text?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it != Unknown && it.wire == wire } ?: Unknown
        }
    }
}

data class AcquisitionEvent(val at: String, val text: String)

/** One song Octo is downloading for the signed-in user, or finished lately. */
data class Acquisition(
    val id: String,
    val artist: String,
    val title: String,
    val album: String?,
    val state: String,
    val progress: Float?,
    val source: String?,
    val error: String?,
    val libraryId: String?,
    val key: String?,
    val quality: String?,
    val peer: String?,
    val note: String?,
    val ahead: Int?,
    val log: List<AcquisitionEvent>,
) {
    val stage: AcquisitionStage get() = AcquisitionStage.of(state)
    val fraction: Float? get() = progress?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
}

data class FoundCopy(
    val id: String?,
    val index: Int?,
    val source: String,
    val peer: String?,
    val file: String,
    val format: String?,
    val quality: String?,
    val size: Long,
    val note: String?,
)

data class FoundSongs(
    val id: String,
    val state: String,
    val error: String?,
    val title: String,
    val artist: String,
    val libraryId: String?,
    val copies: List<FoundCopy>,
) {
    val done: Boolean get() = state.equals("done", true) || state.equals("failed", true)
}

data class PickResult(val state: String, val detail: String?, val key: String?)

object OctoParsers {
    private fun JSONObject.str(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun JSONObject.floatOrNull(name: String): Float? =
        if (has(name) && !isNull(name)) optDouble(name).toFloat().takeIf { it.isFinite() } else null

    /** The `acquisitions` container: `acquisition` is an array, or one object when there is a single row. */
    fun acquisitions(container: JSONObject?): List<Acquisition> = objects(container?.opt("acquisition")).map(::acquisition)

    fun acquisition(o: JSONObject) = Acquisition(
        id = o.optString("id"),
        artist = o.optString("artist"),
        title = o.optString("title"),
        album = o.str("album"),
        state = o.optString("state"),
        progress = o.floatOrNull("progress"),
        source = o.str("source"),
        error = o.str("error"),
        libraryId = o.str("libraryId"),
        key = o.str("key"),
        quality = o.str("quality"),
        peer = o.str("peer"),
        note = o.str("note"),
        ahead = if (o.has("ahead") && !o.isNull("ahead")) o.optInt("ahead") else null,
        log = objects(o.opt("event")).map { AcquisitionEvent(it.optString("at", it.optString("time")), it.optString("text", it.optString("message"))) },
    )

    fun foundSongs(o: JSONObject): FoundSongs {
        val song = o.optJSONObject("song")
        return FoundSongs(
            id = o.optString("id"),
            state = o.optString("state"),
            error = o.str("error"),
            title = song?.optString("title").orEmpty(),
            artist = song?.optString("artist").orEmpty(),
            libraryId = song?.str("libraryId"),
            copies = objects(o.opt("candidate")).map {
                FoundCopy(
                    id = it.str("id"),
                    index = if (it.has("index") && !it.isNull("index")) it.optInt("index") else null,
                    source = it.optString("source"),
                    peer = it.str("peer"),
                    file = it.optString("file"),
                    format = it.str("format"),
                    quality = it.str("quality"),
                    size = it.optLong("size"),
                    note = it.str("note"),
                )
            },
        )
    }

    fun pick(o: JSONObject) = PickResult(o.optString("state"), o.str("detail"), o.str("key"))

    /** Extension name -> versions, from `openSubsonicExtensions`. */
    fun extensions(array: JSONArray?): Map<String, Set<Int>> {
        val out = HashMap<String, Set<Int>>()
        for (i in 0 until (array?.length() ?: 0)) {
            val e = array!!.optJSONObject(i) ?: continue
            val versions = e.optJSONArray("versions")
            out[e.optString("name")] = (0 until (versions?.length() ?: 0)).map { versions!!.optInt(it) }.toSet()
        }
        return out
    }

    private fun objects(value: Any?): List<JSONObject> = when (value) {
        is JSONArray -> (0 until value.length()).mapNotNull { value.optJSONObject(it) }
        is JSONObject -> listOf(value)
        else -> emptyList()
    }
}
