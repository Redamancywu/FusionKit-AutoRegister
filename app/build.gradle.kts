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

// 通过属性切换测试版本：默认 1.0.1，可以用 -PfusionkitVersion=1.0.3 切换
val fusionkitVersion = providers.gradleProperty("fusionkitVersion").orElse("1.0.1").get()

dependencies {
    // 使用 Maven Central 的坐标
    ksp("io.github.redamancywu:FusionKit-AutoRegister-Processor:$fusionkitVersion")
    implementation("io.github.redamancywu:FusionKit-AutoRegister-Processor:$fusionkitVersion")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}