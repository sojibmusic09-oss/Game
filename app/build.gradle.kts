import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

fun propOrEnv(key: String, default: String = ""): String {
    return localProperties.getProperty(key) ?: System.getenv(key) ?: default
}

android {
    namespace = "com.aistudio.bdas.qmkxrp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aistudio.bdas.qmkxrp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val projectDebugKeystore = file("${rootProject.projectDir}/debug.keystore")
        val homeDebugKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
        val resolvedDebugKeystore = if (homeDebugKeystore.exists()) homeDebugKeystore else projectDebugKeystore

        create("debugConfig") {
            storeFile = resolvedDebugKeystore
            storePassword = propOrEnv("BDAS_DEBUG_STORE_PASSWORD", "android")
            keyAlias = propOrEnv("BDAS_DEBUG_KEY_ALIAS", "androiddebugkey")
            keyPassword = propOrEnv("BDAS_DEBUG_KEY_PASSWORD", "android")
        }

        val releaseStoreFilePath = propOrEnv("BDAS_RELEASE_STORE_FILE", "release.keystore")
        val releaseStoreFile = file(releaseStoreFilePath)
        create("release") {
            if (releaseStoreFile.exists()) {
                storeFile = releaseStoreFile
                storePassword = propOrEnv("BDAS_RELEASE_STORE_PASSWORD", "")
                keyAlias = propOrEnv("BDAS_RELEASE_KEY_ALIAS", "")
                keyPassword = propOrEnv("BDAS_RELEASE_KEY_PASSWORD", "")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debugConfig")
            isMinifyEnabled = false
        }

        getByName("release") {
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile != null) {
                signingConfig = releaseConfig
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
