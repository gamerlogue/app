package it.maicol07.gamerlogue.auth

import io.ktor.client.fetchOptions
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.decodeURLQueryComponent
import kotlinx.browser.document

internal actual fun HttpRequestBuilder.configurePlatformSession() {
    fetchOptions { credentials = "include" }
    document.cookie
        .split(';')
        .map(String::trim)
        .firstOrNull { it.startsWith("XSRF-TOKEN=") }
        ?.substringAfter('=')
        ?.decodeURLQueryComponent()
        ?.let { headers.append("X-XSRF-TOKEN", it) }
}
