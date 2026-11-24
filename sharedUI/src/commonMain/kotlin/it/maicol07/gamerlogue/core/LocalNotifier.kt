package it.maicol07.gamerlogue.core

import androidx.navigation3.runtime.NavKey

/**
 * The kinds of notification the app shows. On Android each is a system channel, so the user picks which ones to get,
 * and how, in the system settings instead of the app's. [id] is the channel id, which the backend's pushes name too
 * (Notification → Push in `docs/CONTRACTS.md`): it must never change, or the user's choices for it are lost.
 */
enum class NotificationChannel(val id: String) {
    /** Likes, reactions, comments and replies: the channel every push went to before there were others. */
    SOCIAL("social"),
    FOLLOWERS("followers"),
    MENTIONS("mentions"),
    FOLLOWING("following"),
    RELEASES("releases"),
    EVENTS("events"),
    SYNC("sync"),
    SYNC_LOGIN("sync_login");

    companion object {
        /** The channel of a backend notification of [type], the same the backend names in its push. */
        fun ofNotificationType(type: String?): NotificationChannel = when (type) {
            "new_follower" -> FOLLOWERS
            "mention" -> MENTIONS
            "followed_user_activity" -> FOLLOWING
            else -> SOCIAL
        }
    }
}

/** Shows notifications through the platform's own system: Android's, the desktop tray's; none in the browser. */
interface LocalNotifier {
    /** Whether notifications show at all, so a feature that is only a notification can hide where they don't. */
    val isSupported: Boolean

    /** Opens the system page of the app's notifications (Android: its channels); null where there's none. */
    val openSystemSettings: (() -> Unit)?

    /**
     * Shows one on [channel], in place of the one with the same [tag]. Tapping it opens [target] where the platform
     * tells taps apart (Android); else it just shows.
     */
    suspend fun notify(channel: NotificationChannel, tag: String, title: String, text: String, target: NavKey?)
}
