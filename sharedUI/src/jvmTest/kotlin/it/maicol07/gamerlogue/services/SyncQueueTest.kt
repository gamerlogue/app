package it.maicol07.gamerlogue.services

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SyncQueueTest : StringSpec({
    val steam = DueSync(ExternalService.STEAM, SyncKind.LIBRARY)
    val psn = DueSync(ExternalService.PLAYSTATION, SyncKind.WISHLIST)

    "counts X of N over a batch and a second one queued meanwhile, then starts over" {
        var queue = SyncQueue(waiting = listOf(steam, psn)).next(steam)
        (queue.done + 1 to queue.total) shouldBe (1 to 2)
        queue = queue.copy(waiting = queue.waiting + steam) // a second trigger while the first runs
        queue = queue.next(psn)
        (queue.done + 1 to queue.total) shouldBe (2 to 3)
        queue = queue.next(null) // first batch ends, the second still waits for the lock
        queue.isEmpty shouldBe false
        queue = queue.next(steam)
        (queue.done + 1 to queue.total) shouldBe (3 to 3)
        queue.next(null) shouldBe SyncQueue()
    }
})
