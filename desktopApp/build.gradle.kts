import org.jetbrains.compose.desktop.application.dsl.TargetFormat

val appPackageName = project.findProperty("appPackageName").toString()

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.hot.reload)
    alias(libs.plugins.git.semantic.versioning.base)
}

version = gitSemVer.computeVersion()

// jpackage only accepts a numeric major.minor.patch, so the pre-release identifier and build metadata that git-semver
// appends (e.g. 0.1.0-dev01+abc1234) are dropped here.
val distributionVersion = version.toString().substringBefore('-').substringBefore('+')

dependencies {
    implementation(project(":sharedUI"))
}

compose.desktop {
    application {
        mainClass = "$appPackageName.MainKt"
        jvmArgs("--enable-native-access=ALL-UNNAMED")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
            packageName = appPackageName
            packageVersion = distributionVersion

            linux {
                iconFile.set(project.file("src/main/resources/AppIcon.png"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/AppIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/AppIcon.icns"))
                bundleID = appPackageName
                // jpackage rejects a major version of 0 on macOS, so 0.x builds ship as 1.x until the first 1.0.0 tag.
                packageVersion = distributionVersion.removePrefix("0.").let {
                    if (it == distributionVersion) it else "1.$it"
                }
            }
        }
    }
}

// The compose.desktop block only covers `run` and the packaged distributions; hot-reload runs through its own
// JavaExec task, which needs the same flag.
tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
