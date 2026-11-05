// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OctoParsersTest {
    private fun inner(json: String, name: String) = JSONObject(json).getJSONObject("subsonic-response").getJSONObject(name)

    @Test
    fun anAcquisitionRowIsRead() {
        val list = OctoParsers.acquisitions(inner(
            """{"subsonic-response":{"acquisitions":{"acquisition":[{"id":"dz-1","artist":"Daft Punk","title":"HBFS","album":"Discovery","state":"downloading","progress":0.42,"source":"Soulseek","error":null,"libraryId":null}]}}}""",
            "acquisitions",
        ))
        assertEquals(1, list.size)
        val a = list.single()
        assertEquals(AcquisitionStage.Downloading, a.stage)
        assertEquals(0.42f, a.fraction!!, 0.001f)
        assertNull(a.error)
        assertEquals("Soulseek", a.source)
    }

    @Test
    fun aSingleRowComesAsOneObject() {
        val list = OctoParsers.acquisitions(inner(
            """{"subsonic-response":{"acquisitions":{"acquisition":{"id":"x","artist":"A","title":"T","state":"failed","error":"no peers"}}}}""",
            "acquisitions",
        ))
        assertEquals(AcquisitionStage.Failed, list.single().stage)
        assertTrue(list.single().stage.finished)
        assertEquals("no peers", list.single().error)
    }

    @Test
    fun anUnknownStageIsUnknownNotAnError() {
        assertEquals(AcquisitionStage.Unknown, AcquisitionStage.of("transmogrifying"))
    }

    @Test
    fun foundSongsListsEveryCopyWithItsIdOrIndex() {
        val f = OctoParsers.foundSongs(inner(
            """{"subsonic-response":{"foundSongs":{"id":"s1","state":"done","song":{"artist":"Air","title":"Sexy Boy","libraryId":"nd-7"},"candidate":[{"source":"Soulseek","peer":"p1","file":"a.flac","format":"flac","quality":"FLAC 16","size":31000000,"index":0,"id":"c-1"},{"source":"Soulseek","file":"b.mp3","size":9,"note":"Another version","index":1}]}}}""",
            "foundSongs",
        ))
        assertTrue(f.done)
        assertEquals("nd-7", f.libraryId)
        assertEquals("c-1", f.copies[0].id)
        assertNull(f.copies[1].id)
        assertEquals(1, f.copies[1].index)
        assertEquals("Another version", f.copies[1].note)
    }

    @Test
    fun extensionsMapNamesToVersions() {
        val e = OctoParsers.extensions(JSONObject("""{"x":[{"name":"octoAcquisitions","versions":[1,2]},{"name":"songLyrics","versions":[1]}]}""").getJSONArray("x"))
        assertTrue(2 in e.getValue(OCTO_ACQUISITIONS))
        assertFalse(e.containsKey(OCTO_IMPORTS))
    }

    @Test
    fun aPickAnswersItsStateAndKey() {
        val p = OctoParsers.pick(inner("""{"subsonic-response":{"pick":{"state":"queued","detail":"Getting FLAC","key":"soulseek:3kX9Qm"}}}""", "pick"))
        assertEquals("queued", p.state)
        assertEquals("soulseek:3kX9Qm", p.key)
    }
}
