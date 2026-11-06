// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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

/** One line for a list: how much of it the library has, and what is on its way. */
fun importListLine(l: ImportList): String =
    "${l.have} of ${l.total} in your library" + (if (l.coming > 0) " · ${l.coming} coming" else "")

/**
 * Fork (Octo): the lists the user brought to Octo (a Spotify playlist, a link, a file) with how much of each the library
 * has, a switch to fetch what is missing, and a way to add a link. Draws nothing on a plain Navidrome or when offline.
 */
@Composable
fun OctoImportsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val offlineMode by AppSettings.getInstance(context).offlineMode.collectAsState()
    var overview by remember { mutableStateOf<ImportsOverview?>(null) }
    var adding by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val client = remember { OctoClient(context) }

    LaunchedEffect(offlineMode) {
        if (offlineMode || !NetworkUtils.isNetworkAvailable(context)) { overview = null; return@LaunchedEffect }
        if (client.extensions().getOrNull()?.get(OCTO_IMPORTS) == null) return@LaunchedEffect
        while (true) {
            client.imports().onSuccess { overview = it }
            delay(if (overview?.lists?.any { it.coming > 0 } == true) 10_000L else 60_000L)
        }
    }
    val o = overview ?: return

    fun act(action: String, params: Map<String, String>, then: () -> Unit = {}) = scope.launch {
        client.importAction(action, params)
            .onSuccess { a -> if (a.message.isNotBlank()) Toast.makeText(context, a.message, Toast.LENGTH_LONG).show(); then() }
            .onFailure { Toast.makeText(context, it.message ?: "Octo did not answer", Toast.LENGTH_LONG).show() }
        client.imports().onSuccess { overview = it }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Your imports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { adding = true }) { Text("Add link") }
            }
            o.libraryProblem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            if (o.lists.isEmpty()) Text("Nothing imported yet. Add a link to a public playlist or album.", style = MaterialTheme.typography.bodySmall)
            o.lists.take(8).forEach { l ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(l.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Switch(checked = l.getMissing, onCheckedChange = { on -> act("fetch", mapOf("id" to l.id, "on" to on.toString())) })
                    }
                    Text(importListLine(l) + if (l.getMissing) "" else " · not fetching the rest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { l.fraction }, modifier = Modifier.fillMaxWidth())
                }
            }
            OctoImportParsers.trickleLine(o)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Add a link") },
            text = {
                OutlinedTextField(link, { link = it }, label = { Text("Public playlist or album link") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(enabled = link.isNotBlank(), onClick = { act("addLink", mapOf("url" to link.trim())) { link = "" }; adding = false }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } },
        )
    }
}
