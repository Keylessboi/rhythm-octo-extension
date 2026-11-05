// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.offline

import chromahub.rhythm.app.features.streaming.domain.model.StreamingSong

/**
 * Fork (Octo): which songs may be kept on the device or put in a server playlist. An Octo preview
 * ([StreamingSong.isExternal]) is a YouTube stream the proxy does not own yet: downloading it would store a
 * preview-grade copy under an id that stops existing once Octo grabs the real file, and a playlist entry
 * would point at that id. Starring the preview is what makes Octo grab it; after the rescan it is an ordinary
 * library song and passes both checks.
 */
object DownloadEligibility {
    fun isDownloadable(song: StreamingSong): Boolean = !song.isExternal

    fun downloadable(songs: List<StreamingSong>): List<StreamingSong> = songs.filter(::isDownloadable)

    /** Songs that may join a playlist, and the ones refused (previews). */
    fun splitForPlaylist(songs: List<StreamingSong>): Pair<List<StreamingSong>, List<StreamingSong>> =
        songs.partition(::isDownloadable)
}
