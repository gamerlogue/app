package it.maicol07.gamerlogue.auth

import io.ktor.client.request.HttpRequestBuilder

internal expect fun HttpRequestBuilder.configurePlatformSession()
