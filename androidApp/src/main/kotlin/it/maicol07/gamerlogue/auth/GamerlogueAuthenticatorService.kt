package it.maicol07.gamerlogue.auth

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Service that provides the Gamerlogue account authenticator.
 */
class GamerlogueAuthenticatorService : Service() {
    private lateinit var authenticator: GamerlogueAuthenticator

    override fun onCreate() {
        super.onCreate()
        authenticator = GamerlogueAuthenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder {
        return authenticator.iBinder
    }
}

