package it.maicol07.gamerlogue.auth

import io.ktor.client.plugins.api.ClientPlugin

/** Attaches the platform's backend session to the requests sent to [serverUrl]. */
internal expect fun platformSession(serverUrl: () -> String): ClientPlugin<Unit>
