plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
//    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.krishdev.searchassist"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.krishdev.searchassist"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "NONE"
            }
        }
    }
    
    // Enable Android App Bundle
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        compose = false
        buildConfig = true
    }
//    composeOptions {
//        kotlinCompilerExtensionVersion = "1.5.8" // Updated for Kotlin 2.0
//    }
    packaging {
        resources {
            excludes += setOf(
                "**/kotlin/**",
                "**/*.txt",
                "**/*.version",
                "**/NOTICE",
                "**/LICENSE",
                "**/DEPENDENCIES",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

//composeCompiler {
//
//}

dependencies {
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.ui)            // Jetpack Compose UI
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.runner)
    debugImplementation(libs.androidx.ui.tooling)       // Tooling
    debugImplementation(libs.androidx.ui.test.manifest) // Test Manifest
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}