package it.maicol07.gamerlogue.auth

import io.ktor.client.plugins.api.createClientPlugin

// Bearer tokens carry the session here.
internal actual fun platformSession(serverUrl: () -> String) = createClientPlugin("PlatformSession") {}
