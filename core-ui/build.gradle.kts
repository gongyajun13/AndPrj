import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.google.devtools.ksp.gradle.KspExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.jun.core.ui"
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
    
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_19)
        }
    }
}

configure<KspExtension> {
    allowSourcesFromOtherPlugins = true
}

dependencies {
    // Core Android - 使用 api 暴露给依赖此模块的其他模块
    api(libs.androidx.core.ktx)
    api(libs.androidx.activity.ktx)
    api(libs.androidx.fragment.ktx)
    api(libs.androidx.appcompat)
    api(libs.androidx.material)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.recyclerview)
    api(libs.androidx.viewpager2)
    
    // Hilt - 使用 api 暴露给依赖此模块的其他模块
    api(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Lifecycle - 使用 api 暴露给依赖此模块的其他模块
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    
    // Coroutines - 使用 api 暴露给依赖此模块的其他模块
    api(libs.kotlinx.coroutines.android)
    
    // Logging - 使用 api 暴露给依赖此模块的其他模块
    api(libs.timber)
    
    // Image Loading - 使用 api 暴露给依赖此模块的其他模块
    api(libs.coil)
    
    // X5 WebView - 可选依赖
    // 注意：X5 SDK 需要手动下载 AAR 文件并添加到 libs 目录
    // 下载地址：https://x5.tencent.com/docs/access.html
    // 下载后，取消下面的注释并添加本地依赖：
     api(files("libs/tbs_sdk-44382-202411081743-release.aar"))
    // 
    // 当前使用 compileOnly，代码可以编译通过
    // 如果 X5 SDK 不可用，会自动回退到系统 WebView
    // compileOnly("com.tencent.tbs:tbssdk:4.3.0.93")
    
    // Core Common - 使用 api 传递依赖
    api(project(":core-common"))
    
    // Core Domain - 使用 api 传递依赖
    api(project(":core-domain"))
    
    // Core Network - 用于 WebView 下载功能
    api(project(":core-network"))
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

