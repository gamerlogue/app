package it.maicol07.gamerlogue.auth

import io.ktor.client.fetchOptions
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.http.decodeURLQueryComponent
import kotlinx.browser.document

/** Laravel's "page expired": the XSRF token is missing or no longer matches the session. */
private const val CSRF_MISMATCH = 419

/**
 * Sanctum SPA auth: the session cookie travels with every backend request, and the XSRF-TOKEN cookie is
 * echoed in X-XSRF-TOKEN. On a 419 the CSRF cookie is refreshed and the request is sent once more; a
 * second 419 reaches the caller as is.
 */
internal actual fun platformSession(serverUrl: () -> String) = createClientPlugin("PlatformSession") {
    // A custom IGDB endpoint may not allow credentials, and must not receive the token anyway.
    fun HttpRequestBuilder.isBackend() = url.buildString().startsWith("${serverUrl()}/")

    onRequest { request, _ -> if (request.isBackend()) request.includeSession() }
    on(Send) { request ->
        val call = proceed(request)
        if (call.response.status.value != CSRF_MISMATCH || !request.isBackend()) return@on call
        client.get("${serverUrl()}/sanctum/csrf-cookie")
        request.includeSession()
        proceed(request)
    }
}

private fun HttpRequestBuilder.includeSession() {
    fetchOptions { credentials = "include" }
    document.cookie
        .split(';')
        .map(String::trim)
        .firstOrNull { it.startsWith("XSRF-TOKEN=") }
        ?.substringAfter('=')
        ?.decodeURLQueryComponent()
        ?.let { headers["X-XSRF-TOKEN"] = it }
}
