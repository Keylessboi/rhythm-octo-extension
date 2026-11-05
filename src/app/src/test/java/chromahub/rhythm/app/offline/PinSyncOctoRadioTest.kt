// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.offline

import chromahub.rhythm.spatial.media3.offline.PinPlaylist
import org.junit.Assert.assertEquals
import org.junit.Test

class PinSyncOctoRadioTest {
    private val listing = listOf(
        PinPlaylist("a", "Your Mix", 20, octoRadio = true),
        PinPlaylist("b", "Gym", 5),
        PinPlaylist("c", "Starter Radio", 20, octoRadio = true),
    )

    @Test
    fun octoRadioPlaylistsAreKeptWithTheUsersChoices() {
        assertEquals(setOf("Gym", "Your Mix", "Starter Radio"), PinSync.keptNames(setOf("Gym"), listing, true))
    }

    @Test
    fun withTheSwitchOffOnlyTheUsersChoicesAreKept() {
        assertEquals(setOf("Gym"), PinSync.keptNames(setOf("Gym"), listing, false))
    }

    @Test
    fun anUnreadListingKeepsTheChoicesAsTheyWere() {
        assertEquals(setOf("Gym"), PinSync.keptNames(setOf("Gym"), null, true))
    }

    @Test
    fun anOctoPlaylistTheServerNoLongerListsIsDroppedSoItsSongsAreReleased() {
        // "OutKast Radio" was kept once; Octo rotated it away. It is not in the listing now, so it is not wanted.
        val kept = setOf("Gym", "OutKast Radio", "Your Mix")
        val wanted = PinSync.keptNames(kept, listing, true, knownOcto = setOf("Your Mix"))
        assertEquals(setOf("Gym", "Your Mix", "Starter Radio"), wanted)
    }

    @Test
    fun anOrdinaryPlaylistNamedLikeARadioIsStillCaughtByItsName() {
        assertEquals(true, PinSync.looksLikeOctoRadio("Trap Radio"))
        assertEquals(false, PinSync.looksLikeOctoRadio("Gym"))
    }
}
