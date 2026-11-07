package it.maicol07.gamerlogue.services

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus.BACKLOG
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus.COMPLETED
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus.PAUSED
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus.PLAYING

/** The three-way merge decides what an unattended sync writes on both sides, so a wrong branch loses data. */
class PlanSyncTest : StringSpec({
    val all = setOf(PLAYING, COMPLETED, BACKLOG, PAUSED)
    fun ref(status: GameLibraryStatus) = ExternalGameRef("u", status = status)

    "a game on one side only is added to the other" {
        planSync(mapOf(1 to PLAYING), mapOf(2 to ref(BACKLOG)), emptyMap(), all).changes shouldContainExactly listOf(
            SyncChange(1, null, PLAYING, null, SyncSide.LOCAL),
            SyncChange(2, "u", null, BACKLOG, SyncSide.REMOTE),
        )
    }

    "the side still equal to the last sync loses" {
        val base = mapOf(1 to SyncedGame("u", PLAYING), 2 to SyncedGame("u", PLAYING))
        val plan = planSync(mapOf(1 to COMPLETED, 2 to PLAYING), mapOf(1 to ref(PLAYING), 2 to ref(BACKLOG)), base, all)
        plan.changes.map { it.gameId to it.winner } shouldContainExactly listOf(1 to SyncSide.LOCAL, 2 to SyncSide.REMOTE)
    }

    "both sides changed, or no last sync, is a conflict" {
        planSync(mapOf(1 to COMPLETED), mapOf(1 to ref(PAUSED)), emptyMap(), all).changes.single().winner shouldBe null
    }

    "a game removed on one side since the last sync is left alone" {
        planSync(mapOf(1 to PLAYING), emptyMap(), mapOf(1 to SyncedGame("u", PLAYING)), all).changes.shouldBeEmpty()
    }

    "a status the tracker can't store is not synced, and agreed games become the new baseline" {
        val local = mapOf(1 to PAUSED, 2 to BACKLOG)
        val plan = planSync(local, mapOf(1 to ref(PLAYING), 2 to ref(BACKLOG)), emptyMap(), setOf(PLAYING, BACKLOG))
        plan.changes.shouldBeEmpty()
        plan.agreed shouldBe mapOf(2 to SyncedGame("u", BACKLOG))
    }

    "a tracker status the tracker side can't store is left alone instead of imported" {
        val plan = planSync(emptyMap(), mapOf(1 to ref(PAUSED)), emptyMap(), setOf(PLAYING, BACKLOG))
        plan.changes.shouldBeEmpty()
        plan.agreed.isEmpty() shouldBe true
    }

    "several tracker rows of one game resolve to the lowest uid, whatever their order" {
        val rows = listOf(1 to ExternalGameRef("b"), 1 to ExternalGameRef("a"), 2 to ExternalGameRef("c"))
        val expected = mapOf(1 to ExternalGameRef("a"), 2 to ExternalGameRef("c"))
        rows.onePerGame() shouldBe expected
        rows.reversed().onePerGame() shouldBe expected
    }
})
