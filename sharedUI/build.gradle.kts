@file:OptIn(ExperimentalWasmDsl::class)

import com.google.devtools.ksp.gradle.KspAATask
import io.github.kingsword09.symbolcraft.model.SymbolVariant
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
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
}

kotlin {
    android {
        namespace = appPackageName
        compileSdk = 36
        minSdk = 23
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }

    jvm()

    js { browser() }
//    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            api(libs.compose.resources)
            api(libs.compose.ui.tooling.preview)
            api(libs.compose.material3)

            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.animation)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.material3.adaptive.navigation3)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.navigation3.runtime)
            api(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.materialKolor)
            implementation(libs.multiplatformSettings)
            implementation(libs.sketch)
            implementation(libs.sketchHttp)
            implementation(libs.igdbclient.ktor)
            implementation(libs.kotlin.result)
            implementation(libs.mp.stools)
            api(libs.kotlinx.datetime.ext)
            implementation(libs.zoomimage.compose)
            implementation(libs.spraypaintkt.core)
            implementation(libs.spraypaintkt.ktor)
            implementation(libs.spraypaintkt.annotation)
            implementation(libs.platformtools.core)
            api(libs.platformtools.darkmodedetector)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
        }

        androidMain.dependencies {
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
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.ui)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.logback.classic)
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
        "AppEnvironment.${(
            localProperties.getOrDefault(
                "APP_ENV",
                "local"
            ) as String
            ).uppercase()}"
    )
    buildConfigField("IGDB_API_URL", localProperties.getOrDefault("IGDB_API_URL", "https://api.igdb.com/v4/") as String)
    buildConfigField("GAMERLOGUE_URL", localProperties.getOrDefault("GAMERLOGUE_URL", "") as String)
}

symbolCraft {
    outputDirectory = "build/generated/source/symbolcraft/commonMain/kotlin"
    cacheEnabled = true
    generatePreview = false

    val icons = listOf(
        "add", "android_wifi_3_bar_alert", "arrow_back", "arrow_forward",
        "book_4", "bookmark", "business_center",
        "calendar_month", "celebration", "check", "check_circle", "close", "code", "comedy_mask", "content_copy", "conversion_path",
        "date_range", "delete",
        "edit", "error", "explosion", "explore",
        "family_star", "flutter_dash",
        "grid_4x4",
        "history", "home",
        "joystick",
        "keyboard_arrow_right",
        "info",
        "layers", "lips", "lightbulb", "local_fire_department", "login",
        "music_note", "mystery",
        "newsstand",
        "quiz",
        "rocket",
        "partner_heart", "pause_circle", "person", "person_heart", "playground", "play_circle", "playing_cards", "publish",
        "school", "search", "settings", "skeleton", "simulation", "sports_and_outdoors",
        "sports_martial_arts", "sports_baseball", "sports_motorsports", "stadium", "star",
        "star_shine", "strategy", "swords", "sword_rose",
        "tactic", "theater_comedy", "toys_and_games",
        "upcoming",
        "wand_stars", "web_traffic"
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
        urlTemplate = "https://esm.sh/@mdi/svg@latest/svg/{name}.svg"
    }

//    val brandIcons = listOf(
//        "github"
//    )
//    @Suppress("SpreadOperator")
//    externalIcons(*brandIcons.toTypedArray(), libraryName = "simple-icons") {
//        urlTemplate = "https://simpleicons.org/icons/{name}.svg"
//    }
}
