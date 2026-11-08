package it.maicol07.gamerlogue.services

import com.russhwolf.settings.ObservableSettings
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

/**
 * The IGDB games the user picked by hand for a service's games, by service uid, so the next match of that uid is
 * exact instead of a name guess to confirm again. Kept on this device only: most services have no field per game to
 * carry the id (Backloggery does, through its notes marker). Kept across a disconnect, since uids are the service's.
 */
@Single
class MatchMemory(private val settings: ObservableSettings) {
    private val serializer = MapSerializer(String.serializer(), Int.serializer())

    /** IGDB id by uid of the games picked for [service]. */
    fun picks(service: ExternalService): Map<String, Int> =
        settings.getStringOrNull(service.settingsKey(KEY))
            ?.let { runCatching { Json.decodeFromString(serializer, it) }.getOrNull() }
            .orEmpty()

    /** Merge [picks] (IGDB id by uid) into those stored for [service]; a newer pick replaces an older one. */
    fun remember(service: ExternalService, picks: Map<String, Int>) {
        if (picks.isEmpty()) return
        settings.putString(service.settingsKey(KEY), Json.encodeToString(serializer, picks(service) + picks))
    }

    private companion object {
        const val KEY = "matchPicks"
    }
}
