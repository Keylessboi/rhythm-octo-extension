// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import org.junit.Assert.assertEquals
import org.junit.Test

class OctoSearchLabelTest {
    @Test
    fun aPreviewIsLabelledNotInLibrary() =
        assertEquals("Kanye West • Graduation • Not in library", OctoSearchLabel.subtitle("Kanye West", "Graduation", true, "Not in library"))

    @Test
    fun anOwnedSongKeepsItsPlainSubtitle() =
        assertEquals("Kanye West • Graduation", OctoSearchLabel.subtitle("Kanye West", "Graduation", false, "Not in library"))
}
