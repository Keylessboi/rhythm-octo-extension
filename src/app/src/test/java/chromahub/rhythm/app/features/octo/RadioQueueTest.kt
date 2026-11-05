// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import org.junit.Assert.assertEquals
import org.junit.Test

class RadioQueueTest {
    @Test
    fun theSeedComesFirstAndEachSongOnce() {
        assertEquals(listOf("a", "b", "c"), RadioQueue.build("a", listOf("b", "a", "c", "b"), { it }))
    }

    @Test
    fun theQueueIsCappedAtTheSize() {
        assertEquals(listOf("a", "b", "c"), RadioQueue.build("a", listOf("b", "c", "d", "e"), { it }, size = 3))
    }

    @Test
    fun aSeedWithNothingSimilarIsAQueueOfOne() {
        assertEquals(listOf("a"), RadioQueue.build("a", emptyList(), { it }))
    }
}
