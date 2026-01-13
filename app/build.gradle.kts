import com.google.devtools.ksp.gradle.KspExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)  // 添加 KSP
}

android {
    namespace = "com.jun.andprj"

    defaultConfig {
        applicationId = "com.jun.andprj"
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
}

// 配置 KSP
configure<KspExtension> {
    // 允许 KSP 处理增量编译
    allowSourcesFromOtherPlugins = true
}

dependencies {
    // Core Modules - 框架核心模块（使用 api 传递依赖）
    implementation(project(":core-common"))
    implementation(project(":core-network"))
    implementation(project(":core-database"))
    implementation(project(":core-domain"))
    implementation(project(":core-ui"))
    
    // 注意：以下依赖已通过 core 模块的 api 传递，无需重复声明
    // - androidx.core.ktx (通过 core-common 或 core-ui)
    // - androidx.activity.ktx (通过 core-ui)
    // - androidx.appcompat (通过 core-common 或 core-ui)
    // - androidx.material (通过 core-common 或 core-ui)
    // - androidx.constraintlayout (通过 core-common 或 core-ui)
    // - androidx.recyclerview (通过 core-ui)
    // - androidx.lifecycle.* (通过 core-ui)
    // - retrofit, okhttp, moshi (通过 core-network)
    // - room, datastore (通过 core-database)
    // - kotlinx.coroutines.android (通过所有 core 模块)
    // - coil (通过 core-ui)
    // - timber (通过 core-common)
    
    // Hilt - 虽然 core 模块已通过 api 传递，但 Hilt 插件需要显式声明
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // 刷新视图
    implementation(libs.refresh.layout.kernel) //核心必须依赖
    implementation(libs.refresh.header.classics) //经典刷新头
    implementation(libs.refresh.footer.classics) //经典加载
    // banner
    implementation(libs.banner)
}


// 如果遇到版本冲突，可以添加这个
configurations.all {
    resolutionStrategy {
        // 强制使用特定版本的 kotlinx-metadata-jvm
        force("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")

        // 排除冲突的依赖
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-metadata-jvm")
    }
}