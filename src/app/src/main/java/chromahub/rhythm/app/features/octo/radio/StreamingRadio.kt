// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo.radio

import chromahub.rhythm.app.features.streaming.domain.model.StreamingSong
import kotlin.random.Random

/** Fork (Octo): Start radio, mixed from the library and the server's suggestions the way Octo's apps do it. */
object StreamingRadio {
    fun radioSong(song: StreamingSong, liked: Boolean) = RadioSong(
        id = song.id,
        title = song.title,
        artists = song.artistRefs.map { it.name }.filter { it.isNotBlank() }
            .ifEmpty { listOf(song.displayArtist ?: song.artist).filter { it.isNotBlank() } },
        album = song.albumId ?: song.album,
        genres = listOfNotNull(song.genre?.trim()?.takeIf { it.isNotEmpty() }),
        year = song.year,
        durationMs = song.duration,
        liked = liked,
    )

    /** The seed first, then up to [size] - 1 songs like it. Empty when nothing fits; the caller falls back. */
    fun build(
        seed: StreamingSong,
        library: List<StreamingSong>,
        related: List<StreamingSong>,
        likedIds: Set<String>,
        tuning: RadioTuning,
        now: Long,
        size: Int = 50,
        random: Random = Random.Default,
    ): List<StreamingSong> {
        val byId = HashMap<String, StreamingSong>()
        (library + related).forEach { byId.putIfAbsent(it.id, it) }
        val mixed = radioMix(
            RadioInput(
                seeds = listOf(radioSong(seed, seed.id in likedIds)),
                library = library.map { radioSong(it, it.id in likedIds) },
                suggested = related.map { radioSong(it, it.id in likedIds) },
                now = now,
                tuning = tuning,
            ),
            count = size - 1,
            random = random,
        )
        val songs = mixed.mapNotNull { byId[it.id] }
        return if (songs.isEmpty()) emptyList() else listOf(seed) + songs
    }
}
