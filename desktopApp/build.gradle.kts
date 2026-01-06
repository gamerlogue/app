import org.jetbrains.compose.desktop.application.dsl.TargetFormat

val appPackageName = project.findProperty("appPackageName").toString()

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.hot.reload)
}

dependencies {
    implementation(project(":sharedUI"))
}

compose.desktop {
    application {
        mainClass = "$appPackageName.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
            packageName = appPackageName
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("src/main/resources/AppIcon.png"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/AppIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/AppIcon.icns"))
                bundleID = appPackageName
            }
        }
    }
}
