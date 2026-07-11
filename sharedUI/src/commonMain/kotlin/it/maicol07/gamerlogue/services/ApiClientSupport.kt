package it.maicol07.gamerlogue.services

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Shared JSON reader for the off-WebView store API clients ([PsnApi]/[XboxApi]/[EpicApi]). */
internal val apiJson = Json { ignoreUnknownKeys = true }

/** HTTP Basic credentials (`base64(id:secret)`) for the OAuth token exchanges (PSN/Epic). */
@OptIn(ExperimentalEncodingApi::class)
internal fun basicAuth(clientId: String, clientSecret: String): String =
    Base64.encode("$clientId:$clientSecret".encodeToByteArray())

/** String value of [key], or null when absent / not a JSON primitive. */
internal fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

/** String value of [key], or fail with [message] — for required fields whose absence is a real error. */
internal fun JsonObject.requireString(key: String, message: String): String = string(key) ?: error(message)
