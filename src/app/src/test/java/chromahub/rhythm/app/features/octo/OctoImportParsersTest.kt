// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OctoImportParsersTest {
    private val overview = JSONObject(
        """{"lists":[{"id":"spotify-liked","name":"Liked Songs","source":"spotifyLiked","total":10,"have":6,"missing":2,"queued":1,
        "downloading":1,"keepPlaylist":false,"getMissing":true,"canRefresh":true,"newField":1},
        {"id":"t1","name":"Gym","source":"textList"}],
        "trickle":{"state":"waitingForSoulseek","perHour":20,"queued":1,"current":{"key":"spotify:a","title":"Angel","artist":"Massive Attack","state":"downloading","progress":0.4}},
        "libraryProblem":null}""",
    )

    @Test
    fun readsTheListsAndHowMuchOfEachTheLibraryHas() {
        val o = OctoImportParsers.overview(overview)
        val liked = o.lists.first()
        assertEquals("Liked Songs", liked.name)
        assertEquals(0.6f, liked.fraction, 0.001f)
        assertEquals(2, liked.coming)
        assertTrue(liked.getMissing)
        assertTrue(liked.fromSignIn)
        assertFalse(o.lists[1].fromSignIn)
        assertEquals(0f, o.lists[1].fraction, 0f)
    }

    @Test
    fun readsWhatTheTrickleIsDoing() {
        val o = OctoImportParsers.overview(overview)
        assertEquals("Fetching: Massive Attack – Angel", OctoImportParsers.trickleLine(o))
        assertNull(o.libraryProblem)
    }

    @Test
    fun aSingleListIsAnObjectNotAnArray() {
        val o = OctoImportParsers.overview(JSONObject("""{"lists":{"id":"a","name":"Only","total":3,"have":3}}"""))
        assertEquals(listOf("Only"), o.lists.map { it.name })
    }

    @Test
    fun anAnswerCarriesTheServersWordsAndTheListItBecame() {
        val a = OctoImportParsers.answer(JSONObject("""{"ok":false,"message":"That link is not a public playlist or album."}"""))
        assertFalse(a.ok)
        assertEquals("That link is not a public playlist or album.", a.message)
        assertNull(a.listId)
        assertEquals("f1", OctoImportParsers.answer(JSONObject("""{"ok":true,"listId":"f1","count":12}""")).listId)
    }
}

class ImportListLineTest {
    @Test
    fun aListSaysHowMuchIsInTheLibraryAndWhatIsComing() {
        val l = OctoImportParsers.list(JSONObject("""{"id":"a","name":"X","total":10,"have":6,"queued":1,"downloading":1}"""))
        org.junit.Assert.assertEquals("6 of 10 in your library · 2 coming", importListLine(l))
        org.junit.Assert.assertEquals("3 of 3 in your library", importListLine(l.copy(total = 3, have = 3, queued = 0, downloading = 0)))
    }
}
