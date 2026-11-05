// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.offline

import chromahub.rhythm.app.core.domain.model.SourceType
import chromahub.rhythm.app.features.streaming.domain.model.StreamingSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** An Octo preview is a YouTube stream, not the song: it is never kept on the device or put in a playlist. */
class DownloadEligibilityTest {

    private fun song(id: String, external: Boolean) = StreamingSong(
        id = id, title = id, artist = "a", album = "b", duration = 1000L, artworkUri = null,
        sourceType = SourceType.SUBSONIC, streamingUrl = null, previewUrl = null, isExternal = external,
    )

    @Test
    fun aLibrarySongIsDownloadable() = assertTrue(DownloadEligibility.isDownloadable(song("1", external = false)))

    @Test
    fun anExternalPreviewIsNot() = assertFalse(DownloadEligibility.isDownloadable(song("2", external = true)))

    @Test
    fun filterKeepsOnlyLibrarySongsInOrder() {
        val all = listOf(song("1", false), song("2", true), song("3", false))
        assertEquals(listOf("1", "3"), DownloadEligibility.downloadable(all).map { it.id })
    }

    @Test
    fun onlyLibrarySongsMayJoinAPlaylist() {
        val all = listOf(song("1", false), song("2", true))
        val (allowed, refused) = DownloadEligibility.splitForPlaylist(all)
        assertEquals(listOf("1"), allowed.map { it.id })
        assertEquals(listOf("2"), refused.map { it.id })
    }
}
