package it.maicol07.gamerlogue.ui.components

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.services.DataSource
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.ScriptWriteException
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.StatusChange
import it.maicol07.gamerlogue.services.WebStep
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus

private class BulkWriteSession(private val failAt: Int?) : WebSession {
    override val currentUrl: String? = null
    val sent = mutableListOf<Int>()
    val progress = mutableListOf<Pair<Int, Int>>()
    val batches = mutableListOf<BatchProgress>()
    override fun beginBatch(index: Int, count: Int, completed: Int, total: Int, size: Int) {
        batches += BatchProgress(index, count, completed, total, StepProgress(0, size))
        reportProgress(completed, total)
    }

    override suspend fun awaitLogin(connector: ServiceConnector) = Unit
    override fun reportProgress(done: Int, total: Int) {
        progress += done to total
    }

    override suspend fun run(step: WebStep): List<ExternalGameRef> {
        val refs = mutableListOf<ExternalGameRef>()
        for (id in step.script.split(',').map(String::toInt)) {
            if (id == failAt) throw ScriptWriteException("Connection interrupted", refs)
            sent += id
            refs += ExternalGameRef(id.toString(), gameId = id)
        }
        return refs
    }

    override suspend fun <T> read(source: DataSource<T>, phase: SyncPhase): T = error("Unexpected read")
    override suspend fun confirmPush(games: List<LibrarySync.OutgoingGame>): List<LibrarySync.OutgoingGame> =
        error("Unexpected confirmation")
}

class ServiceWritesTest : StringSpec({
    "script progress is relative to its batch and cannot exceed its bounds" {
        val batch = BatchProgress(3, 3, 20, 23, StepProgress(0, 3))
        batch.withDone(2).step shouldBe StepProgress(2, 3)
        batch.withDone(2).overall shouldBe StepProgress(22, 23)
        batch.withDone(99).overall shouldBe StepProgress(23, 23)
        batch.withDone(-1).overall shouldBe StepProgress(20, 23)
        BatchProgress(2, 3, 10, 23, StepProgress(0, 10)).overall shouldBe StepProgress(10, 23)
    }

    "library additions carry the confirmed platform into the write and preserve existing updates" {
        val addition = StatusChange(
            1,
            "New game",
            null,
            null,
            GameLibraryStatus.COMPLETED,
            platformNames = mapOf(6 to listOf("PC"), 167 to listOf("PlayStation 5")),
            owned = true,
        )
        val update = addition.copy(gameId = 2, uid = "remote-2", from = GameLibraryStatus.PLAYING)
        val skipped = addition.copy(gameId = 3, name = "Skipped game")
        val session = object : WebSession by BulkWriteSession(null) {
            override suspend fun confirmPush(games: List<LibrarySync.OutgoingGame>): List<LibrarySync.OutgoingGame> {
                games.map { it.gameId } shouldBe listOf(1, 3)
                games.first().platformNames shouldBe addition.platformNames
                return listOf(games.first().copy(platformIds = listOf(167)))
            }
        }
        session.confirmLibraryAdditions(listOf(addition, update, skipped)) shouldBe
            listOf(addition.copy(platformIds = listOf(167)), update)
    }

    "existing tracker entries do not require another platform confirmation" {
        val update = StatusChange(
            2,
            "Existing game",
            "remote-2",
            GameLibraryStatus.PLAYING,
            GameLibraryStatus.COMPLETED,
        )
        BulkWriteSession(null).confirmLibraryAdditions(listOf(update)) shouldBe listOf(update)
    }

    "a thousand writes checkpoint before the next batch and report total progress" {
        val session = BulkWriteSession(null)
        val recorded = mutableListOf<Int>()
        val sizes = mutableListOf<Int>()
        val count = session.writeBatches((1..1000).toList(), { batch ->
            recorded shouldBe session.sent
            sizes += batch.size
            WebStep("https://example.invalid", batch.joinToString(","))
        }) { _, refs -> recorded += refs.map { it.gameId!! } }
        count shouldBe 1000
        recorded shouldBe (1..1000).toList()
        sizes.max() shouldBe 10
        session.progress.last() shouldBe (1000 to 1000)
    }

    "an interrupted batch records its confirmed prefix and sends nothing further" {
        val session = BulkWriteSession(24)
        val recorded = mutableListOf<Int>()
        shouldThrow<ScriptWriteException> {
            session.writeBatches((1..1000).toList(), {
                WebStep("https://example.invalid", it.joinToString(","))
            }) { _, refs -> recorded += refs.map { it.gameId!! } }
        }
        session.sent shouldBe (1..23).toList()
        recorded shouldBe session.sent
        session.progress.last() shouldBe (23 to 1000)
    }

    "the final partial batch reports its size and cumulative offset" {
        val session = BulkWriteSession(null)
        session.writeBatches((1..23).toList(), {
            WebStep("https://example.invalid", it.joinToString(","))
        }) { _, _ -> }
        session.batches shouldBe listOf(
            BatchProgress(1, 3, 0, 23, StepProgress(0, 10)),
            BatchProgress(2, 3, 10, 23, StepProgress(0, 10)),
            BatchProgress(3, 3, 20, 23, StepProgress(0, 3)),
        )
        session.progress.last() shouldBe (23 to 23)
    }
})
