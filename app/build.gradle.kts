plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.proyectof"
    compileSdk = 36   // ← Sintaxis correcta (36 aún no existe estable)

    defaultConfig {
        applicationId = "com.example.proyectof"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }


    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}


configurations.all {
    resolutionStrategy {
        force("androidx.camera:camera-core:1.3.1")
        force("androidx.camera:camera-camera2:1.3.1")
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")

    // MediaPipe (HandLandmarker) ← ESTO FALTABA
    implementation("com.google.mediapipe:tasks-vision:0.10.9")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}