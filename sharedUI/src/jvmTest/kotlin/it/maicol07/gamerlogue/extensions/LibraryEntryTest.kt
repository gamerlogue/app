package it.maicol07.gamerlogue.extensions

import at.released.igdbclient.model.Game
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus

class LibraryEntryTest : StringSpec({
    "quickDraft builds entry from game and status" {
        val entry = LibraryEntry.quickDraft(Game(id = 7L), GameLibraryStatus.PLAYING, user = null)

        entry.gameId shouldBe 7
        entry.status shouldBe GameLibraryStatus.PLAYING
        entry.owned shouldBe false
        entry.user.shouldBeNull()
    }

    "quickDraft reuses the existing entry id" {
        val existing = LibraryEntry().apply { id = "42" }
        val draft = LibraryEntry.quickDraft(Game(id = 1L), GameLibraryStatus.BACKLOG, user = null, existing = existing)

        draft.id shouldBe "42"
        draft.status shouldBe GameLibraryStatus.BACKLOG
    }
})
