pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("com.android.settings") version "8.13.2"
    // id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

/*
android {
    execution {
        profiles {
            create("high-mem") {
                r8 {
                    runInSeparateProcess = true
                    jvmOptions += listOf("-Xms2048m", "-Xmx8192m")
                }
            }
            defaultProfile = "high-mem"
        }
    }
}
*/

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Soundly"
include(":app")
include(":SoundlyCloud")
// include(":core") // Descomenta esto cuando el comando 'git submodule add' funcione con éxito

include(":baselineprofile")
