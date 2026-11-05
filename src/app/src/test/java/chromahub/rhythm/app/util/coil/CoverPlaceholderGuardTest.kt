// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.util.coil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverPlaceholderGuardTest {
    private val disc = byteArrayOf(1, 2, 3, 4)
    private val real = byteArrayOf(9, 9, 9)

    @Test
    fun theServersPlaceholderIsRecognisedAndARealCoverIsNot() {
        val registry = CoverPlaceholderRegistry()
        assertTrue(registry.isPlaceholder("h:1", disc) { disc })
        assertFalse(registry.isPlaceholder("h:1", real) { disc })
    }

    @Test
    fun theProbeRunsOncePerServer() {
        val registry = CoverPlaceholderRegistry()
        var probes = 0
        repeat(3) { registry.isPlaceholder("h:1", real) { probes++; disc } }
        assertEquals(1, probes)
    }

    @Test
    fun aServerThatAnswersNoPictureForTheProbeNeverMatches() {
        val registry = CoverPlaceholderRegistry()
        assertFalse(registry.isPlaceholder("h:2", disc) { null })
    }
}
