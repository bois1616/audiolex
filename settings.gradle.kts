rootProject.name = "audiolex"

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
    }
}

// No toolchain-provisioning plugin here, on purpose, and no `plugins { }`
// block at all: F-Droid's scanner rejects toolchain resolvers because they
// download a Java runtime from an uncontrolled source, and F-Droid builds this
// app from source. `jvmToolchain(21)` in :core/:composeApp resolves against an
// already-installed JDK 21 instead. Which plugin this was and why it is gone:
// docs/fdroid-anmeldung.md, Schritt 2 -- deliberately named there and not
// here, because the scanner matches the plugin id as plain text in Gradle
// files, a comment mentioning it included.

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

include(":core")
include(":composeApp")
