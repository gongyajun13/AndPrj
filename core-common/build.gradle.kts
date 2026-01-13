import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.jun.core.common"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }
    
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_19)
        }
    }
}

dependencies {
    // Core Android - 使用 api 暴露给依赖此模块的其他模块
    api(libs.androidx.core.ktx)
    api(libs.androidx.material)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.appcompat)
    
    // Coroutines - 使用 api 暴露给依赖此模块的其他模块
    api(libs.kotlinx.coroutines.android)
    
    // Logging - 使用 api 暴露给依赖此模块的其他模块
    api(libs.timber)
    
    // 设备兼容框架：https://github.com/getActivity/DeviceCompat
    api(libs.devicecompat)
    
    // 权限请求框架：https://github.com/getActivity/XXPermissions
    api(libs.xxpermissions)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

