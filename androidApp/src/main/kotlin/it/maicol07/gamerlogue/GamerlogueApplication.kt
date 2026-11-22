package it.maicol07.gamerlogue

import android.app.Application
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.push.firebase.FirebasePush
import it.maicol07.gamerlogue.di.startKoinIfNeeded

// Here rather than in AppActivity: FCM can start the process to deliver a push without any activity.
class GamerlogueApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Before anything else: a process started for a background sync has no activity to start it.
        startKoinIfNeeded(this)
        KMPNotifier.initialize(
            NotificationPlatformConfiguration.Android(
                notificationIconResId = R.mipmap.ic_launcher_monochrome,
                notificationChannelData = NotificationPlatformConfiguration.Android.NotificationChannelData(
                    id = "social",
                    name = getString(R.string.notification_channel_social),
                ),
            ),
            FirebasePush,
        )
    }
}
