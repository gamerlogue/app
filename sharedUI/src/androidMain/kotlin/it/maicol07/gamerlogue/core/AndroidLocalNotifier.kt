package it.maicol07.gamerlogue.core

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation3.runtime.NavKey
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.notifications__channel_events
import gamerlogue.sharedui.generated.resources.notifications__channel_followers
import gamerlogue.sharedui.generated.resources.notifications__channel_following
import gamerlogue.sharedui.generated.resources.notifications__channel_mentions
import gamerlogue.sharedui.generated.resources.notifications__channel_releases
import gamerlogue.sharedui.generated.resources.notifications__channel_social
import gamerlogue.sharedui.generated.resources.sync__notification_channel
import gamerlogue.sharedui.generated.resources.sync__notification_channel_login
import it.maicol07.gamerlogue.shared.R
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTreeLayout
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** Shows the app's notifications on their system channels; a tap opens the app on the notification's target. */
class AndroidLocalNotifier(private val context: Context) : LocalNotifier {
    override val isSupported = true

    override val openSystemSettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override suspend fun notify(channel: NotificationChannel, tag: String, title: String, text: String, target: NavKey?) {
        createNotificationChannels(context)
        val sync = channel == NotificationChannel.SYNC || channel == NotificationChannel.SYNC_LOGIN
        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(if (sync) android.R.drawable.stat_notify_sync else R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .apply { target?.let { setContentIntent(openAppIntent(context, it, tag.hashCode())) } }
            .build()
        if (canPostNotifications(context)) NotificationManagerCompat.from(context).notify(tag, 0, notification)
    }
}

/**
 * Creates (or renames, after a language change) every [NotificationChannel], so the system settings list them all
 * from the first start and a push naming one finds it.
 */
suspend fun createNotificationChannels(context: Context) {
    val channels = NotificationChannel.entries.map { channel ->
        val importance = if (channel == NotificationChannel.SYNC) {
            NotificationManagerCompat.IMPORTANCE_LOW
        } else {
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        }
        NotificationChannelCompat.Builder(channel.id, importance).setName(getString(channel.label())).build()
    }
    NotificationManagerCompat.from(context).createNotificationChannelsCompat(channels)
}

private fun NotificationChannel.label(): StringResource = when (this) {
    NotificationChannel.SOCIAL -> Res.string.notifications__channel_social
    NotificationChannel.FOLLOWERS -> Res.string.notifications__channel_followers
    NotificationChannel.MENTIONS -> Res.string.notifications__channel_mentions
    NotificationChannel.FOLLOWING -> Res.string.notifications__channel_following
    NotificationChannel.RELEASES -> Res.string.notifications__channel_releases
    NotificationChannel.EVENTS -> Res.string.notifications__channel_events
    NotificationChannel.SYNC -> Res.string.sync__notification_channel
    NotificationChannel.SYNC_LOGIN -> Res.string.sync__notification_channel_login
}

/** Without the permission (Android 13+, not granted yet) nothing shows, and nothing else changes. */
fun canPostNotifications(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
    PackageManager.PERMISSION_GRANTED

/**
 * Opens the app on [target]. [requestCode] tells the notifications' intents apart: they differ only in the extra,
 * which doesn't count, so with one code the last would retarget them all.
 */
fun openAppIntent(context: Context, target: NavKey, requestCode: Int): PendingIntent? {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
    intent.putExtra(EXTRA_NAV_KEY, navKeyJson.encodeToString(PolymorphicSerializer(NavKey::class), target))
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getActivity(context, requestCode, intent, flags)
}

/** The destination a notification's tap asks for; null when none, or when another app sent something else. */
fun Intent.notificationTarget(): NavKey? = getStringExtra(EXTRA_NAV_KEY)?.let {
    try {
        navKeyJson.decodeFromString(PolymorphicSerializer(NavKey::class), it)
    } catch (_: SerializationException) {
        null
    }
}

private const val EXTRA_NAV_KEY = "it.maicol07.gamerlogue.NAV_KEY"

private val navKeyJson = Json {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) { with(RootNavTreeLayout) { polymorphicSerializationSubClasses() } }
    }
}
