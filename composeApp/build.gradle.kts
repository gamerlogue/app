@file:OptIn(ExperimentalWasmDsl::class, ExperimentalKotlinGradlePluginApi::class)

import com.google.devtools.ksp.gradle.KspAATask
import io.github.kingsword09.symbolcraft.model.SymbolVariant
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import java.util.Properties

val appPackageName = "it.maicol07.gamerlogue"

val localProperties = Properties().apply {
    load(project.rootProject.file("local.properties").inputStream())
}

plugins {
    alias(libs.plugins.multiplatform) // Must be first
    alias(libs.plugins.android.application)
    alias(libs.plugins.git.semantic.versioning)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.symbolCraft)
}

kotlin {
    androidTarget {
        // https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }

    js {
        browser()
        binaries.executable()
    }

//    wasmJs {
//        browser()
//        binaries.executable()
//    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.ui)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)
            implementation(libs.animation)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.material3.adaptive.navigation3)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            api(libs.koin.annotations)
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
            implementation(libs.kotlinx.datetime.ext)
            implementation(libs.zoomimage.compose)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ui.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.browser)
            implementation(libs.ui.tooling)
            implementation(libs.androidx.activityCompose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.systemUIBarsTweaker)
        }
    }

    sourceSets.named("commonMain").configure {
        kotlin.srcDirs(
            "build/generated/ksp/metadata",
            "build/generated/source/symbolcraft/commonMain/kotlin"
        )
    }
}

android {
    namespace = appPackageName
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36

        applicationId = appPackageName
        versionCode = androidGitSemVer.computeVersionCode()
        versionName = androidGitSemVer.computeVersion()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    flavorDimensions.add("default")

    productFlavors {
        create("beta") {
            dimension = "default"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_beta"
        }
        create("alpha") {
            dimension = "default"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_alpha"
        }
    }

    sourceSets.getByName("main").res.setSrcDirs(
        setOf("src/commonMain/composeResources") + sourceSets.getByName("main").res.srcDirs
    )

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "values**"
        }
    }
    buildTypes {
        getByName("release") {
            // Enables code shrinking, obfuscation, and optimization for only
            // your project's release build type. Make sure to use a build
            // variant with `isDebuggable=false`.
            isMinifyEnabled = true

            // Enables resource shrinking, which is performed by the
            // Android Gradle plugin.
            isShrinkResources = true

            // Includes the default ProGuard rules files that are packaged with
            // the Android Gradle plugin. To learn more, go to the section about
            // R8 configuration files.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isDebuggable = true
            isJniDebuggable = true
            isMinifyEnabled = false
        }
    }
}

ksp {
    arg("KOIN_USE_COMPOSE_VIEWMODEL", "true")
    arg("KOIN_CONFIG_CHECK", "true")
}

// https://developer.android.com/develop/ui/compose/testing#setup
dependencies {
    androidTestImplementation(libs.androidx.uitest.junit4)
    debugImplementation(libs.androidx.uitest.testManifest)
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
    add("kspAndroid", libs.koin.ksp.compiler)
    add("kspJs", libs.koin.ksp.compiler)
    coreLibraryDesugaring(libs.desugarJdkLibs)
}

tasks.withType<KspAATask>().configureEach {
    dependsOn(tasks.named("generateSymbolCraftIcons"))
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

buildConfig {
    buildConfigField("APP_ENV", localProperties.getOrDefault("APP_ENV", "local") as String)
    buildConfigField("IGDB_API_URL", localProperties.getOrDefault("IGDB_API_URL", "https://api.igdb.com/v4/") as String)
}

symbolCraft {
    outputDirectory = "build/generated/source/symbolcraft/commonMain/kotlin"
    cacheEnabled = true
    generatePreview = false

    val icons = listOf(
        "android_wifi_3_bar_alert",
        "arrow_back", "arrow_forward",
        "book_4", "business_center",
        "calendar_month", "celebration", "close", "code", "comedy_mask", "content_copy", "conversion_path",
        "error", "explosion", "explore",
        "family_star", "flutter_dash",
        "grid_4x4",
        "history", "home",
        "joystick",
        "keyboard_arrow_right",
        "info",
        "layers", "lips", "lightbulb", "local_fire_department",
        "music_note", "mystery",
        "newsstand",
        "quiz",
        "rocket",
        "partner_heart", "person", "person_heart", "playground", "playing_cards", "publish",
        "school", "search", "settings", "skeleton", "simulation", "sports_and_outdoors",
        "sports_martial_arts", "sports_baseball", "sports_motorsports", "stadium", "star_shine",
        "strategy", "swords", "sword_rose",
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
