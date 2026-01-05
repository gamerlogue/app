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

        maven {
            name = "Central Portal Snapshots"
            url = URI("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}
include(":composeApp")

