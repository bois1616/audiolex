plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// Single source of truth for the app version is AppVersion.kt (shown in the
// UI). Read versionName/versionCode out of it here so the APK version can
// never drift from what the StartScreen displays. Bump them there, not here.
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
        versionCode = appVersionCode
        versionName = appVersionName
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    // Only for BuildConfig.DEBUG, which gates the Dev-only Kanaltest entry on
    // the StartScreen (see isDebugBuild). AGP 8 generates BuildConfig only on
    // request; nothing else in the project reads it.
    buildFeatures {
        buildConfig = true
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
