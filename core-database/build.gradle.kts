import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.google.devtools.ksp.gradle.KspExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.jun.core.database"
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

configure<KspExtension> {
    allowSourcesFromOtherPlugins = true
}

dependencies {
    // Core Android - 使用 api 暴露给依赖此模块的其他模块
    api(libs.androidx.core.ktx)
    
    // Hilt - 使用 api 暴露给依赖此模块的其他模块
    api(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Database - 使用 api 暴露给依赖此模块的其他模块（app 模块可能需要直接使用）
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    api(libs.androidx.datastore.preferences)
    
    // Coroutines - 使用 api 暴露给依赖此模块的其他模块
    api(libs.kotlinx.coroutines.android)
    
    // Core Common - 使用 api 传递依赖
    api(project(":core-common"))
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

