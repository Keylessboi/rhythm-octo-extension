// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo.radio

import chromahub.rhythm.app.core.domain.model.SourceType
import chromahub.rhythm.app.features.streaming.domain.model.StreamingSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StreamingRadioTest {
    private fun song(id: String, artist: String, genre: String = "Hip-Hop") = StreamingSong(
        id = id, title = "t$id", artist = artist, album = "al$id", duration = 200_000L, artworkUri = null,
        sourceType = SourceType.SUBSONIC, streamingUrl = null, previewUrl = null, genre = genre, year = 2010,
    )

    @Test
    fun theSeedIsFirstAndEverySongIsOnlyOnce() {
        val seed = song("seed", "A")
        val library = listOf(seed) + (1..30).map { song("l$it", "B$it") }
        val related = (1..30).map { song("r$it", "C$it") }
        val out = StreamingRadio.build(seed, library, related, emptySet(), RadioTuning(), 1_800_000_000_000L, random = Random(3))
        assertEquals("seed", out.first().id)
        assertEquals(out.size, out.map { it.id }.toSet().size)
        assertTrue(out.size in 2..50)
    }

    @Test
    fun onlyMyLibraryNeverPlaysASongOutsideIt() {
        val seed = song("seed", "A")
        val library = listOf(seed) + (1..30).map { song("l$it", "B$it") }
        val related = (1..30).map { song("r$it", "C$it") }
        val out = StreamingRadio.build(seed, library, related, emptySet(), RadioTuning(discovery = RadioDiscovery.LibraryOnly), 1_800_000_000_000L, random = Random(5))
        assertTrue(out.drop(1).all { it.id.startsWith("l") })
    }

    @Test
    fun mostlyNewTakesMostOfTheRadioFromSuggestions() {
        val seed = song("seed", "A")
        val library = listOf(seed) + (1..60).map { song("l$it", "B$it") }
        val related = (1..60).map { song("r$it", "C$it") }
        val out = StreamingRadio.build(seed, library, related, emptySet(), RadioTuning(discovery = RadioDiscovery.MostlyNew), 1_800_000_000_000L, random = Random(7))
        assertTrue(out.drop(1).count { it.id.startsWith("r") } > out.size / 2)
    }

    @Test
    fun aSeedWithNothingToMatchGivesNoRadio() {
        val seed = song("seed", "A", genre = "")
        assertTrue(StreamingRadio.build(seed, listOf(seed), emptyList(), emptySet(), RadioTuning(), 1L).isEmpty())
    }

    @Test
    fun settingsFallBackToTheDefaultsForUnknownNames() {
        assertEquals(RadioTuning(), RadioSettings.parse("Gone", null, "Nope", false))
        assertEquals(RadioDiscovery.Lots, RadioSettings.parse("Lots", null, null, false).discovery)
        assertFalse(RadioSettings.parse(null, null, null, false).favorites)
    }
}
