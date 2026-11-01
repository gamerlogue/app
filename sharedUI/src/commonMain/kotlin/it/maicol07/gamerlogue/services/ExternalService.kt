package it.maicol07.gamerlogue.services

import it.maicol07.gamerlogue.services.ServiceCategory.CONSOLE
import it.maicol07.gamerlogue.services.ServiceCategory.PC
import it.maicol07.gamerlogue.services.ServiceCategory.TRACKER
import kotlinx.serialization.Serializable

/** What kind of service an [ExternalService] is; groups the linked-services list into sections. */
enum class ServiceCategory { CONSOLE, PC, TRACKER }

/**
 * Identity of an external gaming platform — a stable key for DI lookup, navigation, settings keys and
 * UI iteration. All per-service data and behavior live on its [ServiceConnector]; icon/label are
 * mapped in the UI layer.
 *
 * [TRACKER]s are backlog trackers rather than stores: their "library" is the user's tracked games, each
 * with a play status ([ExternalGameRef.status]), so their games aren't imported as owned.
 */
@Serializable
enum class ExternalService(val category: ServiceCategory) {
    STEAM(PC),
    PLAYSTATION(CONSOLE),
    XBOX(CONSOLE),
    GOG(PC),
    EPIC(PC),
    NINTENDO(CONSOLE),
    UBISOFT(PC),
    BACKLOGGERY(TRACKER),
    BACKLOGGD(TRACKER),
    HOWLONGTOBEAT(TRACKER),
    GROUVEE(TRACKER),
    INFINITE_BACKLOG(TRACKER),
    STASH(TRACKER),
}

/** Settings key of a per-service value (link flags, profile, Stash handle). */
fun ExternalService.settingsKey(suffix: String) = "service.$name.$suffix"
