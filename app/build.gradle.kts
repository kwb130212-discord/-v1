plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kwb130212.macrov1"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.kwb130212.macrov1.app2026.v13"
        minSdk = 24
        targetSdk = 36
        versionCode = 14
        versionName = "1.4"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
