package it.maicol07.gamerlogue.di

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import it.maicol07.gamerlogue.auth.AuthenticationHandler
import it.maicol07.gamerlogue.auth.JvmAuthenticationHandler
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.koin.plugin.module.dsl.module

/**
 * `AuthenticationHandler` is resolved from Koin rather than built by a `@Composable`, and its factory
 * pulls the auth client through a qualifier. The Koin compiler plugin (1.0.2) has been seen dropping
 * definitions silently — see `AppJsonApiConfig` — so a broken binding would only surface as a runtime
 * "no definition found" on the first login attempt. This test is the compile-time-ish check instead.
 */
@OptIn(KoinInternalApi::class)
class PlatformModuleBindingTest : StringSpec({
    "the platform authentication handler resolves from a running Koin instance" {
        startKoin {
            module<HttpModule>()
            module<PlatformModule>()
        }
        try {
            // Compile safety is off for this compilation (see sharedUI/build.gradle.kts): the checker cannot
            // see main's definitions from a test compilation, so this lookup is only verified at runtime.
            KoinPlatform.getKoin().scopeRegistry.rootScope
                .get<AuthenticationHandler>()
                .shouldBeInstanceOf<JvmAuthenticationHandler>()
        } finally {
            stopKoin()
        }
    }
})
