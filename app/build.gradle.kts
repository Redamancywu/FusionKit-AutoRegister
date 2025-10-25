plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" // ✅ 降级到 2.0.21
}

android {
    namespace = "com.horizon.fusionkit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.horizon.fusionkit"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // KSP 处理器依赖 - 使用 GitHub Packages
    ksp("com.redamancy.fusionkit:autoregister-processor:1.0.2-beta")

    // 注解依赖 - 使用 GitHub Packages
    implementation("com.redamancy.fusionkit:autoregister-processor:1.0.2-beta")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}