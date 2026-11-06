// SPDX-FileCopyrightText: 2024-2026 winters27 <https://github.com/winters27/octo-player>
// SPDX-License-Identifier: GPL-3.0-or-later
// Ported from octo-player (shared/core/.../radio/RadioSong.kt); the title and artist readings are a smaller version
// of Octo's SongIdentity, enough for the radio's spacing.
package chromahub.rhythm.app.features.octo.radio

import java.text.Normalizer

// A song as the radio sees it.
data class RadioSong(
    val id: String,
    val title: String,
    val artists: List<String>,
    val album: String? = null,
    val genres: List<String> = emptyList(),
    val year: Int? = null,
    val durationMs: Long = 0,
    val composers: List<String> = emptyList(),
    val liked: Boolean = false,
    val rating: Int = 0,
    val plays: Long = 0,
    val lastPlayedAt: Long? = null,
) {
    // What the radio compares songs by, worked out when first needed.
    internal val artistKeys: Set<String> by lazy { artists.mapNotNullTo(LinkedHashSet(), ::artistKey) }
    internal val composerKeys: Set<String> by lazy { composers.mapNotNullTo(LinkedHashSet(), ::artistKey) }
    internal val genreKeys: List<String> by lazy { genres.map(::searchKey).filter(String::isNotEmpty).distinct() }
    internal val titleKey: String by lazy { searchKey(titleCore(title)) }
}

private val marks = Regex("\\p{M}+")

// For matching what someone types: lowercase, accents gone.
fun searchKey(text: String): String =
    marks.replace(Normalizer.normalize(text.trim(), Normalizer.Form.NFD), "").lowercase()

private val creditSplit = Regex("""\s+(?:feat\.?|ft\.?|featuring|with|x|&)\s+|\s*[,;/]\s*""", RegexOption.IGNORE_CASE)

// One spelling per artist: the main name of a credit, lowercase, accents gone.
internal fun artistKey(name: String): String? =
    searchKey(name.split(creditSplit).first()).takeIf(String::isNotEmpty)

private val bracketed = Regex("""\s*[(\[][^)\]]*[)\]]""")
private val dashSuffix = Regex("""\s+-\s+.*$""")

// A title without its version marks: "Song (Live)" and "Song - Remastered 2011" are one title.
internal fun titleCore(title: String): String = title.replace(bracketed, "").replace(dashSuffix, "").trim().ifEmpty { title }
