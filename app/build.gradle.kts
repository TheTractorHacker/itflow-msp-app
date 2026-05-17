plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.foleyit.itflow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.foleyit.itflow"
        minSdk = 34
        targetSdk = 36
        versionCode = 6
        versionName = "1.6.0"
    }

    buildTypes {
        debug {
            // Beta build — separate app ID so it installs alongside release
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            resValue("string", "app_name", "ITFlow Beta")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    // CameraX + ZXing for barcode scanning
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.zxing.core)
    // Glance widget
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    // WorkManager for widget refresh
    implementation(libs.work.runtime.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
