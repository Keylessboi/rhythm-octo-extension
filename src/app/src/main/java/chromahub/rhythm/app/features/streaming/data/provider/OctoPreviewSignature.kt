// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.streaming.data.provider

/**
 * Fork (Octo): recognises a song Octo only previews. Octo sends it as an ordinary song and hardcodes
 * `isExternal=false` on every song it renders (octo `SubsonicResponseBuilder.cs`), so the flag cannot be trusted.
 * What Octo builds for a preview is a fixed shape: `path` = `{artist}/{album}/{title}.{suffix}`, an estimated
 * bitrate (128 kbps m4a, or 950 kbps flac while it waits for lossless) and a fixed 44.1 kHz. A real Navidrome file
 * has its real path (track number, disc folders), its real bitrate and its own sample rate, so all four must match
 * before a song is called a preview: a false match would keep a real song out of the downloads.
 */
object OctoPreviewSignature {
    private val ESTIMATED_KBPS = setOf(128, 950)
    private const val FIXED_SAMPLING_RATE = 44100

    fun isPreview(
        artist: String,
        album: String,
        title: String,
        suffix: String?,
        bitRateKbps: Int,
        samplingRate: Int,
        path: String?,
    ): Boolean {
        if (path.isNullOrBlank() || suffix.isNullOrBlank()) return false
        if (bitRateKbps !in ESTIMATED_KBPS || samplingRate != FIXED_SAMPLING_RATE) return false
        return path.equals("$artist/$album/$title.$suffix", ignoreCase = true)
    }
}
