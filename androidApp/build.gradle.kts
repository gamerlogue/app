import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val appPackageName = project.findProperty("appPackageName").toString()

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.git.semantic.versioning)
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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
        setOf("../sharedUI/src/commonMain/composeResources") + sourceSets.getByName("main").res.srcDirs
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

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

dependencies {
    implementation(project(":sharedUI"))
    implementation(libs.androidx.activityCompose)

    androidTestImplementation(libs.androidx.uitest.junit4)
    debugImplementation(libs.androidx.uitest.testManifest)
    coreLibraryDesugaring(libs.desugarJdkLibs)
}
