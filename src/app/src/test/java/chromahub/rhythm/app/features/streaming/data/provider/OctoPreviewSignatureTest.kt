// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.streaming.data.provider

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Octo renders a preview as an ordinary song and hardcodes `isExternal=false` (SubsonicResponseBuilder.cs). What it
 * does not fake is the combination below: a path it builds as `{artist}/{album}/{title}.{suffix}`, the estimated
 * 128 kbps m4a (or 950 kbps flac when it waits for lossless), and a fixed 44.1 kHz. A real Navidrome file has
 * its real path (track number, disc folder), its real bitrate and its own sample rate.
 */
class OctoPreviewSignatureTest {

    private fun sig(
        artist: String = "Phoebe Bridgers", album: String = "Lost Weekend", title: String = "Bobby",
        suffix: String? = "m4a", bitRateKbps: Int = 128, samplingRate: Int = 44100,
        path: String? = "Phoebe Bridgers/Lost Weekend/Bobby.m4a",
    ) = OctoPreviewSignature.isPreview(artist, album, title, suffix, bitRateKbps, samplingRate, path)

    @Test
    fun anOctoPreviewMatches() = assertTrue(sig())

    @Test
    fun aLosslessWaitingPreviewMatches() = assertTrue(
        sig(suffix = "flac", bitRateKbps = 950, path = "Phoebe Bridgers/Lost Weekend/Bobby.flac"),
    )

    @Test
    fun aRealLibraryM4aWithItsOwnPathDoesNot() = assertFalse(
        sig(path = "Phoebe Bridgers/Lost Weekend/03 - Bobby.m4a"),
    )

    @Test
    fun aRealLibraryFileAt128kbpsWithAMatchingPathButAnotherSampleRateDoesNot() = assertFalse(sig(samplingRate = 48000))

    @Test
    fun aMatchingPathAtAnotherBitrateDoesNot() = assertFalse(sig(bitRateKbps = 256))

    @Test
    fun noPathMeansNoMatch() = assertFalse(sig(path = null))

    @Test
    fun theComparisonIgnoresCaseOnly() {
        assertTrue(sig(path = "phoebe bridgers/lost weekend/bobby.M4A"))
        assertFalse(sig(path = "Phoebe Bridgers/Lost Weekend/Haunted.m4a"))
    }
}
