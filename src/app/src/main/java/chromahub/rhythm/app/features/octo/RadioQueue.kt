// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

/** Fork: the queue of "Start radio from this song": the seed first, then songs like it, each song once. */
object RadioQueue {
    fun <T> build(seed: T, related: List<T>, idOf: (T) -> String, size: Int = 50): List<T> {
        val seen = HashSet<String>().apply { add(idOf(seed)) }
        return listOf(seed) + related.filter { seen.add(idOf(it)) }.take(size - 1)
    }
}
