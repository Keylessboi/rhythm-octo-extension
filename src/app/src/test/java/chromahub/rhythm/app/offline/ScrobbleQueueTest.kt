// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.offline

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ScrobbleQueueTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun queue() = ScrobbleQueue(java.io.File(tmp.root, "scrobbles.json"))

    @Test
    fun aPlayMadeOfflineIsKeptWithItsOwnTime() {
        val q = queue()
        q.add("song1", 1_000L)
        q.add("song2", 2_000L)
        assertEquals(listOf("song1" to 1_000L, "song2" to 2_000L), q.all().map { it.songId to it.playedAtMs })
    }

    @Test
    fun theQueueSurvivesARestart() {
        queue().add("song1", 1_000L)
        assertEquals(1, queue().all().size)
    }

    @Test
    fun flushSendsInOrderAndKeepsWhatFailed() = runBlocking {
        val q = queue()
        q.add("a", 1L); q.add("b", 2L); q.add("c", 3L)
        val sent = mutableListOf<String>()
        q.flush { id, _ -> if (id == "b") false else { sent += id; true } }
        assertEquals(listOf("a", "c"), sent)
        assertEquals(listOf("b"), q.all().map { it.songId })
    }

    @Test
    fun flushStopsAtTheFirstFailureSoAnOfflinePhoneDoesNotHammerTheServer() = runBlocking {
        val q = queue()
        q.add("a", 1L); q.add("b", 2L)
        var calls = 0
        q.flush(stopOnFailure = true) { _, _ -> calls++; false }
        assertEquals(1, calls)
        assertEquals(2, q.all().size)
    }

    @Test
    fun theQueueIsCappedAtTheNewestPlays() {
        val q = queue()
        repeat(ScrobbleQueue.MAX + 5) { q.add("s$it", it.toLong()) }
        assertEquals(ScrobbleQueue.MAX, q.all().size)
        assertTrue(q.all().none { it.songId == "s0" })
    }

    @Test
    fun theSameStopReportedTwiceIsOnePlay() {
        val q = queue()
        q.add("a", 1_000L); q.add("a", 1_140L); q.add("a", 60_000L)
        assertEquals(listOf(1_000L, 60_000L), q.all().map { it.playedAtMs })
    }
}
