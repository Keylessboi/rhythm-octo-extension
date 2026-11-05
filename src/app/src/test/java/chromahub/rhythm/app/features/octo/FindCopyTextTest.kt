// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import org.junit.Assert.assertEquals
import org.junit.Test

class FindCopyTextTest {
    private fun copy(file: String, size: Long = 0, format: String? = null, quality: String? = null, source: String = "soulseek", peer: String? = null) =
        FoundCopy(null, 1, source, peer, file, format, quality, size, null)

    @Test fun theServersIdIsWhatFollowsTheServicePrefix() =
        assertEquals("abc123", FindCopyText.providerId("SUBSONIC::abc123"))

    @Test fun anIdWithoutAPrefixIsKept() = assertEquals("abc123", FindCopyText.providerId("abc123"))

    @Test fun fileNamesDropTheirFolders() {
        assertEquals("01 Song.flac", FindCopyText.fileName(copy("music\\Artist\\01 Song.flac")))
        assertEquals("01 Song.flac", FindCopyText.fileName(copy("music/Artist/01 Song.flac")))
    }

    @Test fun detailJoinsWhatIsKnown() =
        assertEquals("FLAC · 16-bit · 31.4 MB · soulseek · dj42", FindCopyText.detail(copy("a.flac", 31_400_000, "flac", "16-bit", peer = "dj42")))

    @Test fun detailSkipsWhatIsMissing() = assertEquals("soulseek", FindCopyText.detail(copy("a.mp3")))
}
