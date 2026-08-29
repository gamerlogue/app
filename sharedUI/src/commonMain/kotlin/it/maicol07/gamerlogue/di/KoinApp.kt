package it.maicol07.gamerlogue.di

import org.koin.core.annotation.KoinApplication

/**
 * Anchor for the Koin compiler plugin: `koinConfiguration<KoinApp>()` assembles every `@Module` in the
 * project from it.
 *
 * It lives here rather than next to the composable that consumes it so `App.kt` does not have to import
 * both `org.koin.compose.KoinApplication` (the composable) and `org.koin.core.annotation.KoinApplication`
 * (this annotation) under the same name. Aliasing the annotation instead would be the riskier fix: the
 * plugin resolves it in FIR, and a resolution miss shows up as a definition that is silently absent
 * rather than as a compile error.
 */
@KoinApplication
internal object KoinApp
