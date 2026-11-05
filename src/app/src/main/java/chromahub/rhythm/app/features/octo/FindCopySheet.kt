// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Fork (Octo): plain words for one candidate copy of a song, as the Octo apps list them. */
object FindCopyText {
    /** The server's id for a song whose app id is `SERVICE::providerId`. */
    fun providerId(songId: String): String = songId.substringAfter("::")

    fun size(bytes: Long): String = when {
        bytes <= 0 -> ""
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        else -> "${bytes / 1_000} KB"
    }

    fun detail(copy: FoundCopy): String =
        listOfNotNull(copy.format?.uppercase(), copy.quality, size(copy.size).ifEmpty { null }, copy.source, copy.peer)
            .joinToString(" · ")

    fun fileName(copy: FoundCopy): String = copy.file.substringAfterLast('/').substringAfterLast('\\')
}

/**
 * Fork (Octo): searches Soulseek/Lidarr again for [songId] and lets the user choose which copy Octo keeps, for when the
 * first one it took was a bad rip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindCopySheet(songId: String, title: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val client = remember { OctoClient(context) }
    val scope = rememberCoroutineScope()
    var found by remember { mutableStateOf<FoundSongs?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(songId) {
        client.findSongs(FindCopyText.providerId(songId)).onFailure { error = it.message ?: "Search failed"; return@LaunchedEffect }
            .onSuccess { found = it }
        while (found?.done == false) {
            delay(1_500)
            client.foundSongs(found!!.id).onSuccess { found = it }.onFailure { error = it.message; return@LaunchedEffect }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Find another copy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val f = found
            when {
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                f == null -> CircularProgressIndicator()
                else -> {
                    if (!f.done) Text("Still searching… ${f.copies.size} found", style = MaterialTheme.typography.bodySmall)
                    if (f.done && f.copies.isEmpty()) Text(f.error ?: "No other copies found")
                    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(f.copies, key = { it.id ?: "i${it.index}${it.file}" }) { copy ->
                            Column(
                                Modifier.fillMaxWidth().clickable {
                                    scope.launch {
                                        client.pick(f.id, copy)
                                            .onSuccess { Toast.makeText(context, it.detail ?: "Octo is getting this copy", Toast.LENGTH_LONG).show(); onDismiss() }
                                            .onFailure { Toast.makeText(context, it.message ?: "Could not pick this copy", Toast.LENGTH_LONG).show() }
                                    }
                                }.padding(vertical = 4.dp),
                            ) {
                                Text(FindCopyText.fileName(copy), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(FindCopyText.detail(copy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
