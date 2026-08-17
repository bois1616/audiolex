plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// AppVersion.kt is the single source of truth for the version the UI shows.
// Read it here and assert it against the literals in `defaultConfig` below --
// see the comment there for why those literals have to exist at all.
val appVersionFile = file("src/commonMain/kotlin/de/hexenwoche/audiolex/AppVersion.kt")
val appVersionText = appVersionFile.readText()
val appVersionName = Regex("""VERSION_NAME\s*=\s*"([^"]+)"""")
    .find(appVersionText)?.groupValues?.get(1)
    ?: error("VERSION_NAME not found in ${appVersionFile.path}")
val appVersionCode = Regex("""VERSION_CODE\s*=\s*(\d+)""")
    .find(appVersionText)?.groupValues?.get(1)?.toInt()
    ?: error("VERSION_CODE not found in ${appVersionFile.path}")

kotlin {
    jvmToolchain(21)
    androidTarget()
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(project(":core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.room.runtime)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "de.hexenwoche.audiolex.generated.resources"
}

android {
    namespace = "de.hexenwoche.audiolex"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.hexenwoche.audiolex"
        minSdk = 29
        targetSdk = 35
        // These two MUST stay literals. F-Droid's `fdroid checkupdates` reads
        // the version out of this file with a regex that only matches a number
        // or a quoted string right after the keyword
        // (`\b[Vv]ersionCode\s*=?\s*["'(]*([0-9][0-9_]*)`), and it does not
        // evaluate Gradle. With `versionCode = appVersionCode` here it reported
        // "Couldn't find any version information" and F-Droid's automatic
        // update detection (UpdateCheckMode: Tags) never saw a new release --
        // measured against fdroidserver 2.4.5 on 2026-08-17.
        //
        // The duplication that costs is drift, so the check below makes drift
        // impossible: the build fails before it can produce an APK whose
        // version differs from what AppVersion.kt shows on the StartScreen.
        // Bump both places; the build tells you if you forgot one.
        versionCode = 42
        versionName = "0.33.6"
        check(versionCode == appVersionCode && versionName == appVersionName) {
            "Version drift: build.gradle.kts says $versionName ($versionCode), " +
                "AppVersion.kt says $appVersionName ($appVersionCode). " +
                "Both have to be bumped -- see the comment above."
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

compose.desktop {
    application {
        mainClass = "de.hexenwoche.audiolex.MainKt"
    }
}
