import com.google.devtools.ksp.gradle.KspAATask
import io.github.kingsword09.symbolcraft.model.SymbolVariant
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.util.Properties

val appPackageName = project.findProperty("appPackageName").toString()

val localProperties = Properties().apply {
    load(project.rootProject.file("local.properties").inputStream())
}

plugins {
    alias(libs.plugins.kotlin.multiplatform) // Must be first
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.symbolCraft)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcollection-literals",
            "-Xexpect-actual-classes"
        )
    }

    android {
        namespace = "$appPackageName.shared"

        compileSdk {
            version = release(project.findProperty("androidCompileSdk")!!.toString().toInt()) {
                minorApiLevel = project.findProperty("androidCompileSdkMinor")?.toString()?.toInt()
            }
        }
        minSdk = project.findProperty("androidMinSdk")!!.toString().toInt()

        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }

        withHostTest {}
    }

    jvm()

    js {
        browser()
        // The tests reach Skiko through Compose UI, and only an executable bundles its runtime with webpack:
        // without one, checkComposeUiTestConfigurationForJs fails the JS tests (CMP-4906).
        binaries.executable()
    }
//    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            api(libs.compose.resources)
            api(libs.compose.ui.tooling.preview)
            api(libs.compose.material3)

            implementation(libs.compose.animation)
            implementation(libs.compose.material3.adaptive.navigation.suite)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.material3.adaptive.navigation3)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.nav3ksp)
            implementation(libs.nav3ksp.annotation)
            api(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            implementation(libs.okio)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            api(libs.koin.annotations)
            implementation(libs.materialKolor)
            implementation(libs.multiplatform.settings)
            implementation(libs.sketch)
            implementation(libs.sketchHttp)
            implementation(libs.igdbclient.ktor)
            implementation(libs.kotlin.result)
            implementation(libs.mp.stools)
            implementation(libs.kotlinx.datetime.ext)
            implementation(libs.zoomimage.compose)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.spraypaintkt.core)
            implementation(libs.spraypaintkt.ktor)
            implementation(libs.spraypaintkt.annotation)
            implementation(libs.platformtools.core)
            api(libs.platformtools.darkmodedetector)
            implementation(libs.composeSettings.ui)
            implementation(libs.composeSettings.ui.extended)
            implementation(libs.composeSettings.ui.expressive)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.multiplatform.settings.make.observable)
            implementation(libs.flagpack.compose)
            implementation(libs.countries.core)
            implementation(libs.compose.webview)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        // Unit tests run on the JVM (Kotest + JUnit Platform); kept off the JS target.
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            // sealedSubclasses needs kotlin-reflect on the JVM (NavEntriesCoverageTest).
            implementation(kotlin("reflect"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.browser)
            implementation(libs.compose.ui.tooling)
            implementation(libs.androidx.activityCompose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.systemUIBarsTweaker)
            implementation(libs.slf4j.api)
            implementation(libs.slf4j.android)
        }

        jvmMain.dependencies {
            // compose.ui already ships the Skiko runtime for every desktop OS (skiko-awt-runtime-all).
            implementation(libs.compose.ui)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.logback.classic)
            // OS credential store for the desktop session token (Windows Credential Manager,
            // macOS Keychain, Linux Secret Service). JVM-only by design: Android has AccountManager.
            implementation(libs.java.keyring)
        }

        webMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }

    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "SharedUI"
                    isStatic = true
                }
            }
        }

    sourceSets.named("commonMain").configure {
        kotlin.srcDirs(
            "build/generated/ksp/metadata",
            "build/generated/source/symbolcraft/commonMain/kotlin"
        )
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
    add("kspCommonMainMetadata", libs.spraypaintkt.processor)
    add("kspCommonMainMetadata", libs.nav3ksp.processor)
}

// Kotest's JUnit5 runner needs the JUnit Platform on the JVM test task.
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Koin compiler plugin 1.2.x validates every get<T>() in a compilation that calls startKoin, but a test compilation
// cannot see main's definitions, so the DI tests fail with KOIN-D002 (InsertKoinIO/koin-compiler-plugin#58, milestone
// 1.2.2). Those tests resolve the real graph at runtime, so compile safety is switched off for this task only; the
// option can't be set per compilation, hence rewriting it, as suggested in koin-compiler-plugin#105. Drop on 1.2.2+.
tasks.named<org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile<*>>("compileTestKotlinJvm") {
    doFirst {
        val task = this as org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile<*>
        task.pluginOptions.set(
            task.pluginOptions.get().map { config ->
                org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig().apply {
                    config.allOptions().forEach { (pluginId, options) ->
                        options.forEach { option ->
                            val koinSafety = pluginId == "io.insert-koin.compiler.plugin" && option.key == "compileSafety"
                            val override = org.jetbrains.kotlin.gradle.plugin.SubpluginOption("compileSafety", "false")
                            addPluginArgument(pluginId, if (koinSafety) override else option)
                        }
                    }
                }
            }
        )
    }
}

tasks.withType<KspAATask>().configureEach {
    dependsOn(tasks.named("generateSymbolCraftIcons"))
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

buildConfig {
    packageName = appPackageName

    buildConfigField(
        "it.maicol07.gamerlogue.AppEnvironment",
        "APP_ENV",
        "AppEnvironment.${
            (
                localProperties.getOrDefault(
                    "APP_ENV",
                    "local"
                ) as String
                ).uppercase()
        }"
    )

    val composeResourcesDir = file("src/commonMain/composeResources")
    val availableLanguages = listOf("en") + (
        composeResourcesDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("values-") }
            ?.map { it.name.removePrefix("values-") }
            ?: emptyList()
        )
    buildConfigField(
        "kotlin.collections.Map<String, androidx.compose.ui.text.intl.Locale>",
        "AVAILABLE_LANGUAGES",
        "mapOf(${availableLanguages.joinToString(", ") { "\"$it\" to Locale(\"$it\")" }})"
    )
}

symbolCraft {
    outputDirectory = "build/generated/source/symbolcraft/commonMain/kotlin"
    cacheEnabled = true
    generatePreview = false

    val icons = listOf(
        "add",
        "android_wifi_3_bar_alert",
        "arrow_back",
        "arrow_forward",
        "book_4",
        "bookmark",
        "business_center",
        "calendar_month",
        "category",
        "celebration",
        "check",
        "check_circle",
        "close",
        "code",
        "comedy_mask",
        "content_copy",
        "contrast",
        "conversion_path",
        "date_range",
        "delete",
        "description",
        "devices",
        "download",
        "edit",
        "error",
        "explosion",
        "explore",
        "family_star",
        "filter_list",
        "flutter_dash",
        "grid_4x4",
        "group",
        "history",
        "home",
        "hourglass",
        "info",
        "inventory_2",
        "joystick",
        "keyboard_arrow_right",
        "layers",
        "language",
        "linked_services",
        "link",
        "lips",
        "lightbulb",
        "local_fire_department",
        "login",
        "logout",
        "more_vert",
        "music_note",
        "mystery",
        "newsstand",
        "open_in_new",
        "palette",
        "partner_heart",
        "pause_circle",
        "person",
        "person_heart",
        "playground",
        "play_circle",
        "playing_cards",
        "publish",
        "quiz",
        "rate_review",
        "refresh",
        "rocket",
        "schedule",
        "school",
        "search",
        "search_off",
        "settings",
        "share",
        "skeleton",
        "simulation",
        "sort",
        "sports_and_outdoors",
        "sports_martial_arts",
        "sports_baseball",
        "sports_motorsports",
        "stadium",
        "star",
        "star_shine",
        "storefront",
        "strategy",
        "style",
        "swords",
        "sword_rose",
        "sync",
        "tactic",
        "tag",
        "theater_comedy",
        "timer",
        "toys_and_games",
        "trophy",
        "tune",
        "arrow_downward",
        "arrow_upward",
        "domain",
        "dns",
        "settings_backup_restore",
        "warning",
        "upcoming",
        "visibility",
        "wand_stars",
        "web_traffic"
    )

    @Suppress("SpreadOperator")
    materialSymbols(*icons.toTypedArray()) {
        bothFills(500, SymbolVariant.ROUNDED)
//        bothFills(700, SymbolVariant.ROUNDED)
    }

    val mdiIcons = listOf(
        "knife",
        "ninja",
        "pistol",
        "tank",
        "unicorn-variant"
    )
    @Suppress("SpreadOperator")
    externalIcons(*mdiIcons.toTypedArray(), libraryName = "mdi") {
        // Pinned and redirect-free: esm.sh now 302s `@latest`, which SymbolCraft does not follow.
        urlTemplate = "https://cdn.jsdelivr.net/npm/@mdi/svg@7.4.47/svg/{name}.svg"
    }

    val brandIcons = listOf(
        "android",
        "apple",
        "atari",
        "commodore",
        "discord",
        "epicgames",
        "facebook",
        "fandom",
        "github",
        "gogdotcom",
        "instagram",
        "ios",
        "linux",
        "macos",
        "meta",
        "oculus",
        "playstation",
        "playstation2",
        "playstation3",
        "playstation4",
        "playstation5",
        "playstationvita",
        "reddit",
        "sega",
        "steam",
        "steamdeck",
        "twitch",
        "ubisoft",
        "wikipedia",
        "x",
        "youtube"
    )
    @Suppress("SpreadOperator")
    externalIcons(*brandIcons.toTypedArray(), libraryName = "simple-icons") {
        urlTemplate = "https://simpleicons.org/icons/{name}.svg"
    }

    val brandLogos = listOf(
        "windows",
        "xbox"
    )
    @Suppress("SpreadOperator")
    externalIcons(*brandLogos.toTypedArray(), libraryName = "svgl") {
        urlTemplate = "https://api.svgl.app/svg/{name}.svg"
    }

    applyOkioJsTestWorkaround()
}

// https://github.com/square/okio/issues/1163
fun Project.applyOkioJsTestWorkaround() {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        val applyNodePolyfillPlugin by lazy {
            tasks.register("applyNodePolyfillPlugin") {
                description = "Applies the NodePolyfillPlugin to the webpack config for JS tests, if not already applied."
                val applyPluginFile = projectDir
                    .resolve("webpack.config.d/applyNodePolyfillPlugin.js")
                onlyIf {
                    !applyPluginFile.exists()
                }
                doLast {
                    applyPluginFile.parentFile.mkdirs()
                    applyPluginFile.writeText(
                        """
                        const NodePolyfillPlugin = require("node-polyfill-webpack-plugin");
                        config.plugins.push(new NodePolyfillPlugin());
                        """.trimIndent(),
                    )
                }
            }
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets {
                targets.configureEach {
                    compilations.configureEach {
                        if (platformType == KotlinPlatformType.js && name == "test") {
                            tasks
                                .getByName(compileKotlinTaskName)
                                .dependsOn(applyNodePolyfillPlugin)
                        }
                    }
                }
                jsTest {
                    dependencies {
                        implementation(devNpm("node-polyfill-webpack-plugin", "^2.0.1"))
                    }
                }
            }
        }
    }
}
