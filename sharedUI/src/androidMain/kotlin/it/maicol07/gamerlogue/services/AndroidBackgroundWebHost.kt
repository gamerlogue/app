package it.maicol07.gamerlogue.services

import android.content.Context
import it.maicol07.gamerlogue.ui.components.WebSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Background syncs on a [HeadlessWebSession]: a WebView attached to no window, on the main thread it needs. */
class AndroidBackgroundWebHost(private val context: Context) : BackgroundWebHost {
    override suspend fun <T> withSession(block: suspend (WebSession) -> T): T = withContext(Dispatchers.Main) {
        val session = HeadlessWebSession(context.applicationContext)
        try {
            block(session)
        } finally {
            session.destroy()
        }
    }
}
