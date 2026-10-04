import java.net.URI

rootProject.name = "Gamerlogue"

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("android.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev/")
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("android.*")
            }
        }
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev/")
        maven("https://maven.universablockchain.com/")
        maven("https://jitpack.io/")

        maven("https://maven.pkg.github.com/maicol07/Compose-Settings") {
            content { includeGroup("com.github.maicol07.compose-settings") }
            credentials {
                username = providers.gradleProperty("githubPackagesUsername")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("githubPackagesPassword")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
        }

        maven {
            name = "Central Portal Snapshots"
            url = URI("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}
include(":sharedUI")
include(":androidApp")
include(":desktopApp")
include(":webApp")
