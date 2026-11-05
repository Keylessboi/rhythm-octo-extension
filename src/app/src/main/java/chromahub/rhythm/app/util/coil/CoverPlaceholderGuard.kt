// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.util.coil

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Fork: when a cover cannot be made in time (Octo's cover aggregator, a slow Navidrome), the server answers 200 with
 * its own placeholder picture (the "navidrome" disc). Coil cached that as the song's cover, so the real cover never
 * came back until the cache was cleared. The guard learns what the server's placeholder looks like by asking for a
 * cover that cannot exist, and turns any response with the same bytes into a 404: the app shows its own fallback,
 * and nothing is cached for the next try.
 */
class CoverPlaceholderRegistry {
    private val hashes = ConcurrentHashMap<String, String>()
    private val none = "-"

    /** [probe] fetches the server's picture for a cover that does not exist (null if it answered no picture); asked once per [host]. */
    fun isPlaceholder(host: String, bytes: ByteArray, probe: () -> ByteArray?): Boolean {
        val known = hashes.getOrPut(host) { probe()?.let(::sha256) ?: none }
        return known != none && known == sha256(bytes)
    }

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

class CoverPlaceholderInterceptor : Interceptor {
    private val registry = CoverPlaceholderRegistry()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.url.encodedPath.contains("getCoverArt") || request.url.queryParameter("id") == PROBE_ID) {
            return chain.proceed(request)
        }
        val response = chain.proceed(request)
        if (!response.isSuccessful) return response
        val type = response.body.contentType()
        val bytes = runCatching { response.body.bytes() }.getOrNull() ?: return response
        val host = request.url.host + ":" + request.url.port
        val placeholder = runCatching {
            registry.isPlaceholder(host, bytes) {
                val probeUrl = request.url.newBuilder().setQueryParameter("id", PROBE_ID).build()
                chain.proceed(request.newBuilder().url(probeUrl).build()).use { r ->
                    if (r.isSuccessful && r.body.contentType()?.type == "image") r.body.bytes() else null
                }
            }
        }.getOrDefault(false)
        return if (placeholder) {
            response.newBuilder().code(404).message("Server placeholder cover")
                .body("".toResponseBody("text/plain".toMediaType())).build()
        } else {
            response.newBuilder().body(bytes.toResponseBody(type)).build()
        }
    }

    companion object {
        const val PROBE_ID = "rhythm-no-such-cover"
    }
}
