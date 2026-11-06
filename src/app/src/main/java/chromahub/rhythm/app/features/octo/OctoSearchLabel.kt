// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

/** Fork (Octo): a search hit Octo only previews says so, instead of looking like part of the library. */
object OctoSearchLabel {
    fun subtitle(artist: String, album: String, isExternal: Boolean, notInLibrary: String): String {
        val base = "$artist • $album"
        // First, so a long album name cannot push it off the row.
        return if (isExternal) "$notInLibrary • $base" else base
    }
}
