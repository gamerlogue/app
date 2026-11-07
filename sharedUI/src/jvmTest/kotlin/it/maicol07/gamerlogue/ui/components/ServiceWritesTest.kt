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

    /** Driven by the same [ProgressState] the real session uses. */
    var state = ProgressState()
        private set

    override fun beginBatch(index: Int, count: Int, completed: Int, total: Int, size: Int) {
        state = state.begin(index, count, completed, total, size)
        batches += state.batch!!
        progress += completed to total
    }

    override fun finishBatch(done: Int, total: Int) {
        state = state.finish(done, total)
        progress += done to total
    }

    override fun endBatch() {
        state = state.end()
    }

    override suspend fun awaitLogin(connector: ServiceConnector) = Unit
    override fun reportProgress(done: Int, total: Int) {
        state = state.report(done, total)
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

    "a script report inside a batch is relative to it, and ending the batch clears it" {
        val begun = ProgressState().begin(2, 3, 10, 23, 10)
        begun.progress shouldBe StepProgress(10, 23)
        val reported = begun.report(4, 23)
        reported.batch?.step shouldBe StepProgress(4, 10)
        reported.progress shouldBe StepProgress(14, 23)
        val finished = reported.finish(20, 23)
        finished.batch?.step shouldBe StepProgress(10, 10)
        finished.progress shouldBe StepProgress(20, 23)
        finished.end() shouldBe ProgressState(StepProgress(20, 23), null)
        ProgressState().report(3, 0) shouldBe ProgressState(StepProgress(3, 0), null)
    }

    "a new phase resets progress unless it is a batched write phase" {
        val batched = ProgressState().begin(1, 2, 0, 5, 3)
        batched.nextStep(phaseChanged = true, writing = true) shouldBe batched
        batched.nextStep(phaseChanged = false, writing = false) shouldBe batched
        batched.nextStep(phaseChanged = true, writing = false) shouldBe ProgressState()
        batched.end().nextStep(phaseChanged = true, writing = true) shouldBe ProgressState()
    }

    "writing in batches leaves no batch behind, also when a write fails" {
        val done = BulkWriteSession(null)
        done.writeBatches((1..23).toList(), { WebStep("https://example.invalid", it.joinToString(",")) }) { _, _ -> }
        done.state.batch shouldBe null
        done.state.progress shouldBe StepProgress(23, 23)
        val failed = BulkWriteSession(5)
        shouldThrow<ScriptWriteException> {
            failed.writeBatches((1..23).toList(), { WebStep("https://example.invalid", it.joinToString(",")) }) { _, _ -> }
        }
        failed.state.batch shouldBe null
    }

    "writeEach sums the confirmed counts and ends its batch" {
        val session = BulkWriteSession(null)
        session.writeEach(listOf(1, 2, 3)) { if (it == 2) 0 else 1 } shouldBe 2
        session.batches.map { it.index } shouldBe listOf(1, 2, 3)
        session.state.batch shouldBe null
        session.state.progress shouldBe StepProgress(3, 3)
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
