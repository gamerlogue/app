package it.maicol07.gamerlogue.extensions

import androidx.compose.ui.text.intl.Locale
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js

@OptIn(ExperimentalWasmJsInterop::class)
private val langType: JsAny = js("({ type: 'language' })")

@OptIn(ExperimentalWasmJsInterop::class)
private val regionType: JsAny = js("({ type: 'region' })")

@OptIn(ExperimentalWasmJsInterop::class)
actual fun Locale.getDisplayLanguage(inLocale: Locale): String? {
    val targetLanguage = language
    val localeTag = inLocale.asLanguageTag()
    return Intl.DisplayNames(localeTag, langType)
        .of(targetLanguage)
        .takeIf { it.isNotBlank() } ?: targetLanguage
}

@Suppress("unused")
private external class Intl {
    @Suppress("unused")
    class Locale(tag: String) {
        val baseName: String
        val language: String
        val region: String
        val script: String
        val variants: String
    }

    /**
     * @param options Should contain type (Possible values are "language", "region", "script", "currency", "calendar", and "dateTimeField")
     */
    @OptIn(ExperimentalWasmJsInterop::class)
    class DisplayNames(locale: Locale, options: JsAny) {
        constructor(locale: String, options: JsAny)
        fun of(code: String): String
    }
}

