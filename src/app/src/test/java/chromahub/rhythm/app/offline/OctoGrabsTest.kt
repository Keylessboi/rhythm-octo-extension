// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OctoGrabsTest {
    @get:Rule val tmp = TemporaryFolder()
    private val grab = Grab("SUBSONIC::prev", "Outsidë", "Yeat", "2 Alivë", 1_000L)

    private fun c(id: String, title: String = "Outsidë", artist: String = "Yeat", album: String = "2 Alivë", ext: Boolean = false) =
        GrabCandidate(id, title, artist, album, ext)

    @Test
    fun theLandedSongMatchesDespiteAccentsAndCase() {
        val m = OctoGrabMatcher.match(grab, listOf(c("SUBSONIC::new", title = "OUTSIDE", album = "2 alive")))
        assertEquals("SUBSONIC::new", m?.id)
    }

    @Test
    fun aPreviewIsNeverTheLandedSong() {
        assertNull(OctoGrabMatcher.match(grab, listOf(c("SUBSONIC::other", ext = true), c("SUBSONIC::prev"))))
    }

    @Test
    fun anotherSongByTheArtistDoesNotMatch() {
        assertNull(OctoGrabMatcher.match(grab, listOf(c("SUBSONIC::x", title = "Other"))))
    }

    @Test
    fun theSameAlbumWinsOverAReissue() {
        val m = OctoGrabMatcher.match(grab, listOf(c("SUBSONIC::deluxe", album = "2 Alivë (Deluxe)"), c("SUBSONIC::orig")))
        assertEquals("SUBSONIC::orig", m?.id)
    }

    @Test
    fun aRequestExpiresAfterAWeek() {
        assertFalse(OctoGrabMatcher.expired(grab, 1_000L + OctoGrabMatcher.EXPIRY_MS))
        assertTrue(OctoGrabMatcher.expired(grab, 1_001L + OctoGrabMatcher.EXPIRY_MS))
    }

    @Test
    fun theStoreKeepsAndForgetsGrabs() {
        val store = OctoGrabStore(File(tmp.root, "g.json"))
        store.add(grab); store.add(grab.copy(requestedAtMs = 5))
        assertEquals(listOf(5L), store.all().map { it.requestedAtMs })
        store.remove("SUBSONIC::prev")
        assertTrue(store.all().isEmpty())
    }
}
