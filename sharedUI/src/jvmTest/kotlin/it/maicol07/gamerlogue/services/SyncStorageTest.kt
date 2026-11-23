package it.maicol07.gamerlogue.services

import androidx.datastore.core.CorruptionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import okio.fakefilesystem.FakeFileSystem

class SyncStorageTest : StringSpec({
    "an unreadable document fails instead of reading as an empty baseline" {
        val fileSystem = FakeFileSystem()
        val directory = testSyncDirectory()
        fileSystem.createDirectories(directory)
        fileSystem.write(directory / "wishlist-sync.json") { writeUtf8("{not json") }

        withSyncStorage(fileSystem, directory) { storage ->
            shouldThrow<CorruptionException> { WishlistSyncState(storage).base(ExternalService.STEAM) }
        }
    }
})
