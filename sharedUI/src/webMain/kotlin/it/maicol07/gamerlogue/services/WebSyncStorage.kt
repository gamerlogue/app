package it.maicol07.gamerlogue.services

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.WebLocalStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer

/** Keeps each document in the browser's `localStorage`. Store syncs don't run here, so this only reads defaults. */
class WebSyncStorage(private val scope: CoroutineScope) : SyncStorage {
    override fun <T> dataStore(name: String, serializer: KSerializer<T>, default: T): DataStore<T> =
        DataStoreFactory.create(
            storage = WebLocalStorage(JsonOkioSerializer(serializer, default), "sync.$name"),
            scope = scope,
        )
}
