// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import chromahub.rhythm.app.core.domain.model.SourceType
import chromahub.rhythm.app.features.streaming.domain.model.StreamingPlaylist
import org.junit.Assert.assertEquals
import org.junit.Test

class OctoStationsTest {
    private fun p(name: String, octo: Boolean = true, count: Int = 10) = StreamingPlaylist(
        id = name, name = name, description = null, artworkUri = null, songCount = count,
        isEditable = false, sourceType = SourceType.SUBSONIC, isOctoRadio = octo,
    )

    @Test
    fun onlyOctoStationsWithSongsAreOffered() {
        val r = OctoStations.of(listOf(p("Gym", octo = false), p("Trap Radio"), p("Empty Radio", count = 0)))
        assertEquals(listOf("Trap Radio"), r.map { it.name })
    }

    @Test
    fun yourMixAndDiscoveryComeBeforeTheArtistRadios() {
        val r = OctoStations.of(listOf(p("OutKast Radio"), p("Discovery Radio"), p("Your Radio"), p("Trap Radio")))
        assertEquals(listOf("Your Radio", "Discovery Radio", "OutKast Radio", "Trap Radio"), r.map { it.name })
    }
}
