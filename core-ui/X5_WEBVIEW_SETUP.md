# X5 WebView 集成指南

## 📦 获取 X5 SDK

X5 SDK 无法从 Maven 仓库直接获取，需要手动下载：

1. **访问腾讯 X5 官网**：https://x5.tencent.com/docs/access.html
2. **下载最新版本的 X5 SDK**（通常是 AAR 或 JAR 文件）
3. **将下载的文件放到项目的 `libs` 目录**（如果不存在则创建）

## 🔧 配置依赖

### 方式一：使用本地 AAR 文件（推荐）

1. 将下载的 X5 SDK AAR 文件放到 `core-ui/libs/` 目录
2. 在 `core-ui/build.gradle.kts` 中添加：

```kotlin
dependencies {
    // X5 WebView - 本地 AAR 文件
    api(files("libs/tbs_sdk_xxx.aar"))
    // 或者如果有多个文件：
    // api(fileTree("libs") { include("*.aar") })
}
```

### 方式二：使用本地 JAR 文件

如果下载的是 JAR 文件：

```kotlin
dependencies {
    // X5 WebView - 本地 JAR 文件
    api(files("libs/tbs_sdk_xxx.jar"))
}
```

### 方式三：使用 compileOnly（仅编译时依赖）

如果暂时无法获取 X5 SDK，可以使用 `compileOnly`：

```kotlin
dependencies {
    // X5 WebView - 仅编译时依赖（运行时需要用户自行添加）
    compileOnly("com.tencent.tbs:tbssdk:4.3.0.93")
}
```

**注意**：使用 `compileOnly` 时，代码可以编译通过，但运行时如果 X5 SDK 不存在，会回退到系统 WebView。

## ✅ 验证集成

集成完成后，运行应用并查看日志：

```
[X5WebViewHelper] X5 内核初始化成功，版本: xxx
```

如果看到此日志，说明 X5 内核已成功集成。

## 📝 注意事项

1. **权限要求**：确保在 `AndroidManifest.xml` 中已添加必要权限
2. **首次使用**：X5 内核首次使用时可能需要下载，建议在应用启动时预初始化
3. **包体积**：X5 SDK 会增加约 5-10MB 的包体积
4. **兼容性**：X5 内核主要面向国内用户，海外用户建议使用系统 WebView

## 🔄 回退方案

如果无法获取 X5 SDK，框架已提供回退机制：
- 如果 X5 内核不可用，会自动使用系统 WebView
- 所有 API 保持一致，无需修改业务代码



