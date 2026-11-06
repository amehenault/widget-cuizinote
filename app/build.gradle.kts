plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Numéro de version : numéro de compilation GitHub Actions (1 en local)
val numero = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "ca.cuizinote.widget"
    compileSdk = 35

    defaultConfig {
        applicationId = "ca.cuizinote.widget"
        minSdk = 26
        targetSdk = 35
        versionCode = numero
        versionName = "1.0.$numero"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Application personnelle installée hors Play Store : signée avec la clé de compilation.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}
