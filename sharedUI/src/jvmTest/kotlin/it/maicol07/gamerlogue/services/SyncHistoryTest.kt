package it.maicol07.gamerlogue.services

import com.russhwolf.settings.MapSettings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class SyncHistoryTest : StringSpec({
    fun run(
        id: Int,
        removed: List<SyncGame> = emptyList(),
        toConfirm: List<PendingMatch> = emptyList(),
        result: SyncResult = SyncResult.OK,
    ) = SyncRun(
        id = id.toString(),
        service = ExternalService.BACKLOGGD,
        kind = SyncKind.LIBRARY,
        trigger = SyncTrigger.PERIODIC,
        startedAt = id.toLong(),
        endedAt = id.toLong(),
        result = result,
        removedOnService = removed,
        toConfirm = toConfirm,
    )

    "keeps the newest runs only, and they survive a restart" {
        val settings = MapSettings()
        val history = SyncHistory(settings)
        repeat(55) { history.add(run(it)) }

        val reloaded = SyncHistory(settings).runs.value
        reloaded shouldHaveSize 50
        reloaded.first().id shouldBe "54"
        reloaded.last().id shouldBe "5"
    }

    "opening the history marks every run seen, and a settled removal leaves every run of its service" {
        val history = SyncHistory(MapSettings())
        history.add(run(1, removed = listOf(SyncGame(7, "Kept"), SyncGame(8, "Deleted"))))
        history.add(run(2, removed = listOf(SyncGame(8, "Deleted"))))

        history.markAllSeen()
        history.settleRemoved(ExternalService.BACKLOGGD, SyncKind.LIBRARY, 8, keep = false)

        history.runs.value.all { it.seen } shouldBe true
        history.runs.value.single { it.id == "1" }.removedOnService shouldBe listOf(SyncGame(7, "Kept"))
        history.runs.value.single { it.id == "2" }.removedOnService shouldBe emptyList()
    }

    "covers fill only the games that have none" {
        val history = SyncHistory(MapSettings())
        history.add(run(1, removed = listOf(SyncGame(7, "No cover"), SyncGame(8, "Own cover", coverImageId = "own"))))

        history.fillCovers(mapOf(7 to "found", 8 to "other"))

        history.runs.value.single().removedOnService.map { it.coverImageId } shouldBe listOf("found", "own")
    }

    "a kept game isn't asked about again until a run stops reporting it; a failed run forgets nothing" {
        val settings = MapSettings()
        val history = SyncHistory(settings)
        val kept = SyncGame(7, "Kept")
        history.add(run(1, removed = listOf(kept)))
        history.settleRemoved(ExternalService.BACKLOGGD, SyncKind.LIBRARY, 7, keep = true)

        SyncHistory(settings).add(run(2, removed = listOf(kept))).removedOnService shouldBe emptyList()
        history.add(run(3, result = SyncResult.FAILED))
        history.add(run(4, removed = listOf(kept))).removedOnService shouldBe emptyList()
        history.add(run(5))
        history.add(run(6, removed = listOf(kept))).removedOnService shouldBe listOf(kept)
    }

    "a confirmed match leaves every run, an ignored one never comes back" {
        val history = SyncHistory(MapSettings())
        val confirmed = PendingMatch("a", "A", SyncGame(1, "A"))
        val ignored = PendingMatch("b", "B")
        history.add(run(1, toConfirm = listOf(confirmed, ignored)))

        history.settleMatch(ExternalService.BACKLOGGD, "a", ignore = false)
        history.settleMatch(ExternalService.BACKLOGGD, "b", ignore = true)

        history.runs.value.single().toConfirm shouldBe emptyList()
        history.add(run(2, toConfirm = listOf(confirmed, ignored))).toConfirm shouldBe listOf(confirmed)
    }
})
