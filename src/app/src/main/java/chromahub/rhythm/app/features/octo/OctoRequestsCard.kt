// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.core.utils.NetworkUtils
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One line of words for an acquisition: where it is, never a raw state. */
fun acquisitionStatus(a: Acquisition): String = when (a.stage) {
    AcquisitionStage.Queued -> a.ahead?.takeIf { it > 0 }?.let { "Waiting · $it ahead" } ?: "Waiting"
    AcquisitionStage.Searching -> "Looking for a copy" + (a.note?.let { " · $it" } ?: "")
    AcquisitionStage.Downloading -> "Downloading" + (a.fraction?.let { " ${(it * 100).toInt()}%" } ?: "") + (a.source?.let { " · $it" } ?: "")
    AcquisitionStage.Verifying -> "Checking the file"
    AcquisitionStage.Importing -> "Adding to your library"
    AcquisitionStage.Done -> "In your library" + (a.quality?.let { " · $it" } ?: "")
    AcquisitionStage.Failed -> "Failed" + (a.error?.let { ": $it" } ?: "")
    AcquisitionStage.Unknown -> a.state.ifBlank { "Working" }
}

/**
 * Fork (Octo): what the Octo server is fetching for the user (songs hearted or added while previewing), as the Octo
 * apps' downloads drawer shows it. Draws nothing on a plain Navidrome, offline, or when nothing was asked for.
 */
@Composable
fun OctoRequestsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val offlineMode by AppSettings.getInstance(context).offlineMode.collectAsState()
    var rows by remember { mutableStateOf<List<Acquisition>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val client = remember { OctoClient(context) }

    LaunchedEffect(offlineMode) {
        if (offlineMode || !NetworkUtils.isNetworkAvailable(context)) { rows = emptyList(); return@LaunchedEffect }
        val exts = client.extensions()
        if (exts.getOrNull()?.get(OCTO_ACQUISITIONS) == null) return@LaunchedEffect
        while (true) {
            client.acquisitions().onSuccess { rows = it }
            // Quick while something is moving, lazy once everything has settled.
            delay(if (rows.any { !it.stage.finished }) 5_000L else 30_000L)
        }
    }
    if (rows.isEmpty()) return

    val working = rows.count { !it.stage.finished }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (working > 0) "Octo is getting $working ${if (working == 1) "song" else "songs"}" else "Octo's recent requests",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                )
                if (rows.any { it.stage.finished }) {
                    TextButton(onClick = {
                        scope.launch { client.clearAcquisitions().onSuccess { client.acquisitions().onSuccess { rows = it } } }
                    }) { Text("Clear finished") }
                }
            }
            rows.take(8).forEach { a ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${a.artist} – ${a.title}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        acquisitionStatus(a),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (a.stage == AcquisitionStage.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    if (a.stage == AcquisitionStage.Downloading) {
                        a.fraction?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
                            ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            if (rows.size > 8) Text("and ${rows.size - 8} more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
