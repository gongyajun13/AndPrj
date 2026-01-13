pluginManagement {
    repositories {
        // 优先使用官方仓库（KSP 插件需要）
        google()
        mavenCentral()
        gradlePluginPortal()
        // 阿里云 Gradle 插件镜像
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        }
        // 阿里云 Google 镜像
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // 阿里云 Maven 中央仓库镜像
        maven {
            url = uri("https://maven.aliyun.com/repository/central")
        }
        // JitPack 远程仓库：https://jitpack.io
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云 Maven 公共仓库（包含所有常用仓库）
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        // 阿里云 Google 镜像
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
        // 阿里云 Maven 中央仓库镜像
        maven {
            url = uri("https://maven.aliyun.com/repository/central")
        }
        // 备用仓库
        google()
        mavenCentral()
        // JitPack 远程仓库：https://jitpack.io
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AndPrj"
include(":app")
include(":core-common")
include(":core-network")
include(":core-database")
include(":core-domain")
include(":core-ui")
 