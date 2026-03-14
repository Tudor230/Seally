plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun String.toGradleStringLiteral(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val livekitUrl = (project.findProperty("LIVEKIT_URL") as String?) ?: ""
val livekitToken = (project.findProperty("LIVEKIT_TOKEN") as String?) ?: ""
val livekitLandmarkTopic = (project.findProperty("LIVEKIT_LANDMARK_TOPIC") as String?) ?: "pose.normalized.v1"

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
        resValue("string", "livekit_token", livekitToken.toGradleStringLiteral())
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
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation("com.google.mediapipe:tasks-vision:0.10.32")
    implementation("io.livekit:livekit-android:2.18.2")
    implementation("io.livekit:livekit-android-camerax:2.18.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
