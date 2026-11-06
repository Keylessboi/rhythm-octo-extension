// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.features.streaming.domain.model.StreamingPlaylist
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.StreamingMusicViewModel

/** Fork (Octo): which playlists are Octo's stations, and in what order they are offered. */
object OctoStations {
    private val first = listOf("Starter Radio", "Your Radio", "Your Mix", "Discovery Radio", "Discovery Mix")

    fun of(playlists: List<StreamingPlaylist>): List<StreamingPlaylist> =
        playlists.filter { it.isOctoRadio && it.songCount > 0 }
            .sortedWith(compareBy({ first.indexOf(it.name).let { i -> if (i < 0) first.size else i } }, { it.name.lowercase() }))
}

/** A row of Octo's stations; a tap plays one. Draws nothing when the server has none (or is not Octo). */
@Composable
fun OctoStationsRow(viewModel: StreamingMusicViewModel, modifier: Modifier = Modifier) {
    val playlists by viewModel.savedPlaylists.collectAsState()
    val stations = remember(playlists) { OctoStations.of(playlists) }
    if (stations.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var tuning by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Octo stations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { tuning = true }) { Text("Tune radio") }
        }
        if (tuning) chromahub.rhythm.app.features.octo.radio.RadioTuningDialog { tuning = false }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(stations, key = { it.id }) { station ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.clickable { viewModel.playPlaylist(station) },
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(station.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${station.songCount} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
    }
}
