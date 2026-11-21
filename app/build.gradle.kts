plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.andrewtran1995.liftlog"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.andrewtran1995.liftlog"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    // One permanent key (see signing/README.md) so each new build installs over the last one.
    signingConfigs {
        create("shared") {
            storeFile = rootProject.file("signing/license-to-lift.jks")
            storePassword = "licensetolift"
            keyAlias = "license-to-lift"
            keyPassword = "licensetolift"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            signingConfig = signingConfigs.getByName("shared")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

base {
    archivesName = "license-to-lift" // APKs come out as license-to-lift-release.apk
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Installs the startup/scroll profiles that Compose and other libraries ship, so a sideloaded
    // APK gets ahead-of-time compiled hot paths instead of running them interpreted at first.
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.haze)
    implementation(libs.haze.glass)

    testImplementation(libs.junit)
}
