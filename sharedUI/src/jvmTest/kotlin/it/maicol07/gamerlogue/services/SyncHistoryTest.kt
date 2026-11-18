package it.maicol07.gamerlogue.services

import com.russhwolf.settings.MapSettings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class SyncHistoryTest : StringSpec({
    fun run(id: Int, removed: List<SyncGame> = emptyList()) = SyncRun(
        id = id.toString(),
        service = ExternalService.BACKLOGGD,
        kind = SyncKind.LIBRARY,
        trigger = SyncTrigger.PERIODIC,
        startedAt = id.toLong(),
        endedAt = id.toLong(),
        result = SyncResult.OK,
        removedOnService = removed,
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
        history.settleRemoved(ExternalService.BACKLOGGD, 8)

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
})
