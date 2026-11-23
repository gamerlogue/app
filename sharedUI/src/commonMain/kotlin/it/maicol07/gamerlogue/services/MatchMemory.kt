package it.maicol07.gamerlogue.services

import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.koin.core.annotation.Single

/**
 * The IGDB games the user picked by hand for a service's games, by service uid, so the next match of that uid is
 * exact instead of a name guess to confirm again. Kept on this device only: most services have no field per game to
 * carry the id (Backloggery does, through its notes marker). Kept across a disconnect, since uids are the service's.
 */
@Single
class MatchMemory(storage: SyncStorage) {
    private val store = storage.dataStore(
        name = "match-picks",
        serializer = MapSerializer(ExternalService.serializer(), MapSerializer(String.serializer(), Int.serializer())),
        default = emptyMap(),
    )

    /** IGDB id by uid of the games picked for [service]. */
    suspend fun picks(service: ExternalService): Map<String, Int> = store.data.first()[service].orEmpty()

    /** Merge [picks] (IGDB id by uid) into those stored for [service]; a newer pick replaces an older one. */
    suspend fun remember(service: ExternalService, picks: Map<String, Int>) {
        if (picks.isEmpty()) return
        store.updateData { it + (service to it[service].orEmpty() + picks) }
    }
}
