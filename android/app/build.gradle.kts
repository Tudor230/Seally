plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
}

fun String.toGradleStringLiteral(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val livekitUrl = (project.findProperty("LIVEKIT_URL") as String?) ?: ""
val livekitApiKey = (project.findProperty("LIVEKIT_API_KEY") as String?) ?: ""
val livekitApiSecret = (project.findProperty("LIVEKIT_API_SECRET") as String?) ?: ""
val livekitLandmarkTopic = (project.findProperty("LIVEKIT_LANDMARK_TOPIC") as String?) ?: "pose.binary.v2"

android {
    namespace = "com.example.seally"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.seally"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "livekit_url", livekitUrl.toGradleStringLiteral())
        resValue("string", "livekit_api_key", livekitApiKey.toGradleStringLiteral())
        resValue("string", "livekit_api_secret", livekitApiSecret.toGradleStringLiteral())
        resValue("string", "livekit_landmark_topic", livekitLandmarkTopic.toGradleStringLiteral())
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        // Needed for java.time on minSdk < 26
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        resValues = true
    }
    packaging {
        resources {
            excludes += "META-INF/LICENSE*"
        }
    }
    androidResources {
        noCompress += "task"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)

    // Needed for HorizontalPager (Compose Foundation)
    implementation("androidx.compose.foundation:foundation")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation("com.google.mediapipe:tasks-vision:0.10.32")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
    implementation(libs.play.services.mlkit.barcode.scanning)
    implementation(libs.androidx.compose.foundation)

    // Needed for java.time on minSdk < 26
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.vision.common)
    implementation("io.livekit:livekit-android:2.18.2")
    implementation("io.livekit:livekit-android-camerax:2.18.2")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Persist small user settings/profile fields
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
