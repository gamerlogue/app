package it.maicol07.gamerlogue.services

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path

/**
 * Where the sync state lives (baselines, picks, history), apart from the settings: it grows with the libraries, past
 * what a settings value holds (8 KB on the desktop). Each [dataStore] is one JSON document under its name; there must
 * be one per name in the process, which the `@Single` owners guarantee.
 */
interface SyncStorage {
    /**
     * The document [name], [default] before its first write. One that no longer parses fails every read and write
     * with a [CorruptionException] instead of reading as [default]: an empty baseline would mean a first sync.
     */
    fun <T> dataStore(name: String, serializer: KSerializer<T>, default: T): DataStore<T>
}

/** Keeps each document in `name.json` under [directory], its DataStores working in [scope]. */
class FileSyncStorage(
    private val fileSystem: FileSystem,
    private val directory: Path,
    private val scope: CoroutineScope,
) : SyncStorage {
    override fun <T> dataStore(name: String, serializer: KSerializer<T>, default: T): DataStore<T> =
        DataStoreFactory.create(
            storage = OkioStorage(fileSystem, JsonOkioSerializer(serializer, default)) { directory / "$name.json" },
            scope = scope,
        )
}

/** A sync state document as JSON; one that doesn't decode is a [CorruptionException], as DataStore expects. */
internal class JsonOkioSerializer<T>(private val serializer: KSerializer<T>, default: T) : OkioSerializer<T> {
    override val defaultValue: T = default

    override suspend fun readFrom(source: BufferedSource): T = try {
        json.decodeFromString(serializer, source.readUtf8())
    } catch (e: IllegalArgumentException) { // SerializationException included
        throw CorruptionException("Unreadable sync state", e)
    }

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(serializer, t))
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
