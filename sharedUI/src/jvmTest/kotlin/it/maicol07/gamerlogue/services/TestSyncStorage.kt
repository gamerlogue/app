package it.maicol07.gamerlogue.services

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.uuid.Uuid

/**
 * A [SyncStorage] in memory, empty, its stores working on [dispatcher] (a test one keeps virtual time in step). Its
 * directory is unique: DataStore refuses two open stores on one path in the process, whatever the file system.
 */
fun testSyncStorage(dispatcher: CoroutineDispatcher = Dispatchers.IO): SyncStorage = FileSyncStorage(
    fileSystem = FakeFileSystem(),
    directory = testSyncDirectory(),
    scope = CoroutineScope(dispatcher + SupervisorJob()),
)

fun testSyncDirectory(): Path = "/sync-${Uuid.random()}".toPath()

/**
 * One run of the app on the sync state in [directory] of [fileSystem]: [block] gets the storage, whose stores close
 * after it, so the next run reads back only what was written.
 */
suspend fun <T> withSyncStorage(fileSystem: FileSystem, directory: Path, block: suspend (SyncStorage) -> T): T {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    try {
        return block(FileSyncStorage(fileSystem, directory, scope))
    } finally {
        scope.coroutineContext.job.cancelAndJoin()
    }
}
