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
        manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
        // The AccountManager account type must follow the applicationId, otherwise a debug build cannot own accounts
        // already registered by a release build signed with a different key.
        resValue("string", "account_type", appPackageName)
    }

    buildFeatures {
        // Needed for the generated account_type string; AGP 9 disables resValue support by default.
        resValues = true
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

    // Compose Resources `values/strings.xml` files are also valid Android resources: exposing them as an Android res
    // directory is what lets platform-only XML resolve shared strings. Required today by res/xml/authenticator.xml,
    // which references @string/app_name (declared only in sharedUI's composeResources). Removing this line breaks
    // resource linking with "resource string/app_name not found".
    sourceSets.getByName("main").res.directories.add("../sharedUI/src/commonMain/composeResources")

    packaging {
        resources {
            // Both rules come from the Compose Multiplatform project template and are currently inert: beta debug and
            // release both merge java resources and package without them. Kept as a guard against duplicate license
            // files (the excludes) and duplicate `values*` java resources (the merges) appearing via new dependencies.
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
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_dev"
            resValue("string", "account_type", "$appPackageName.dev")
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

// ACCESS_LOCAL_NETWORK (declared in src/debug/AndroidManifest.xml) is a runtime permission, so a fresh debug install
// starts without it and every request to a local backend on 10.0.2.2 times out. Granting it needs no user interaction
// with adb, so hook it to every debug install instead of leaving it as a step to remember. Devices below API 36 do not
// know the permission and fail the grant; that is expected, hence the ignored exit value.
abstract class GrantLocalNetworkAccessTask : DefaultTask() {
    @get:Input
    abstract val adbPath: Property<String>

    @get:Input
    abstract val applicationId: Property<String>

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun grant() {
        val devices = ByteArrayOutputStream()
        exec.exec {
            executable = adbPath.get()
            args = listOf("devices")
            standardOutput = devices
        }
        // A wireless physical device is attached alongside the emulator often enough that a bare `adb shell`
        // would just fail with "more than one device/emulator", and the ignored exit value would hide it.
        val serials = devices.toString()
            .lineSequence()
            .filter { it.endsWith("\tdevice") }
            .map { it.substringBefore('\t') }
            .toList()
        serials.forEach { serial ->
            exec.exec {
                executable = adbPath.get()
                args = listOf(
                    "-s", serial,
                    "shell", "pm", "grant", applicationId.get(), "android.permission.ACCESS_LOCAL_NETWORK",
                )
                isIgnoreExitValue = true
            }
        }
    }
}

val adbExecutable = extensions
    .getByType<com.android.build.api.variant.ApplicationAndroidComponentsExtension>()
    .sdkComponents.adb

tasks.register<GrantLocalNetworkAccessTask>("grantLocalNetworkAccess") {
    description = "Grant ACCESS_LOCAL_NETWORK to the debug build so it can reach a backend on 10.0.2.2"
    adbPath.set(adbExecutable.map { it.asFile.absolutePath })
    applicationId.set("$appPackageName.dev")
}

tasks.configureEach {
    if (name.startsWith("install") && name.endsWith("Debug")) {
        finalizedBy("grantLocalNetworkAccess")
    }
}
