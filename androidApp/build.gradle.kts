import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream

val appPackageName = project.findProperty("appPackageName").toString()

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
    alias(libs.plugins.git.semantic.versioning)
}

android {
    namespace = appPackageName
    compileSdk {
        version = release(project.findProperty("androidCompileSdk")!!.toString().toInt()) {
            minorApiLevel = project.findProperty("androidCompileSdkMinor")?.toString()?.toInt()
        }
    }

    defaultConfig {
        minSdk = project.findProperty("androidMinSdk")!!.toString().toInt()
        targetSdk = project.findProperty("androidTargetSdk")!!.toString().toInt()

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

    sourceSets.getByName("main").res.directories.add("../sharedUI/src/commonMain/composeResources")

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "values**"
        }
    }

    signingConfigs {
        create("release") {
            val keyStoreFile = File(System.getenv("ANDROID_KEYSTORE_PATH") ?: "release.keystore")
            if (keyStoreFile.exists()) {
                storeFile = keyStoreFile
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.koin.android)

    androidTestImplementation(libs.androidx.uitest.junit4)
    debugImplementation(libs.androidx.uitest.testManifest)
    coreLibraryDesugaring(libs.desugarJdkLibs)
}

// The emulator's virtual Wi-Fi does not forward the 10.0.2.2 magic host, so a debug build pointed at
// http://localhost reaches the local backend only through an adb reverse tunnel. adb reverse does not
// survive an emulator/adb restart, so re-establish it after every debug install. It targets the running
// emulator-* device (a physical device reaches the backend over the LAN, not this tunnel); no emulator
// running just skips it.
abstract class AdbReverseTask : DefaultTask() {
    @get:Input
    abstract val adbPath: Property<String>

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun reverse() {
        val devices = ByteArrayOutputStream()
        exec.exec {
            executable = adbPath.get()
            args = listOf("devices")
            standardOutput = devices
        }
        val emulator = devices.toString()
            .lineSequence()
            .firstOrNull { it.startsWith("emulator-") }
            ?.substringBefore('\t')
        if (emulator == null) {
            logger.lifecycle("adbReverseLocalhost: no emulator running, skipping")
            return
        }
        exec.exec {
            executable = adbPath.get()
            args = listOf("-s", emulator, "reverse", "tcp:80", "tcp:80")
            isIgnoreExitValue = true
        }
    }
}

val adbExecutable = extensions
    .getByType<com.android.build.api.variant.ApplicationAndroidComponentsExtension>()
    .sdkComponents.adb

tasks.register<AdbReverseTask>("adbReverseLocalhost") {
    description = "Tunnel emulator localhost:80 to host:80 for local backend access"
    adbPath.set(adbExecutable.map { it.asFile.absolutePath })
}

tasks.configureEach {
    if (name.startsWith("install") && name.endsWith("Debug")) {
        finalizedBy("adbReverseLocalhost")
    }
}
