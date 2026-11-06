// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo.radio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Fork (Octo): the radio's four dials, as in Octo's apps; saved the moment a choice is made. */
@Composable
fun RadioTuningDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var tuning by remember { mutableStateOf(RadioSettings.load(context)) }
    fun change(next: RadioTuning) { tuning = next; RadioSettings.save(context, next) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Radio tuning") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Dial("Music from outside your library", tuning.discovery.detail) {
                    RadioDiscovery.entries.forEach { o -> Chip(o.label, o == tuning.discovery) { change(tuning.copy(discovery = o)) } }
                }
                Dial("How far it strays", tuning.adventure.detail) {
                    RadioAdventure.entries.forEach { o -> Chip(o.label, o == tuning.adventure) { change(tuning.copy(adventure = o)) } }
                }
                Dial("How soon artists come back", tuning.variety.detail) {
                    RadioVariety.entries.forEach { o -> Chip(o.label, o == tuning.variety) { change(tuning.copy(variety = o)) } }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Favorites more often")
                    Switch(checked = tuning.favorites, onCheckedChange = { change(tuning.copy(favorites = it)) })
                }
            }
        },
    )
}

@Composable
private fun Dial(title: String, detail: String, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { chips() }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) =
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = Modifier.padding(vertical = 2.dp))
