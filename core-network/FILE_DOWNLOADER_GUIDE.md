# FileDownloader 使用指南

## 📦 概述

`FileDownloader` 是基于 core-network 框架的文件下载工具类，提供了完整的文件下载功能，包括进度追踪、速度计算、取消下载等。

**特性：**
- ✅ 使用框架提供的 `OkHttpClient`（支持所有拦截器）
- ✅ 使用 `NetworkClient.RequestConfig` 配置请求（请求头、查询参数等）
- ✅ 使用 `Flow` 发送下载进度和状态
- ✅ 支持取消下载
- ✅ 统一的错误处理
- ✅ 自动计算下载速度
- ✅ 线程安全

### 为什么没有直接使用 NetworkClient？

`NetworkClient` 主要用于 JSON API 请求，它的设计目标是：
1. **JSON 解析**：使用 Retrofit + Moshi 自动解析 JSON 响应
2. **一次性读取**：调用 `response.string()` 一次性读取整个响应体到内存
3. **类型转换**：将响应体解析为指定的类型 `T`

而文件下载的需求不同：
1. **二进制数据**：下载的是二进制文件（APK、图片等），不是 JSON
2. **流式处理**：大文件不能一次性读取到内存（会 OOM），需要边读边写
3. **进度回调**：需要实时更新下载进度
4. **不需要解析**：不需要 JSON 解析

因此，`FileDownloader` 采用了以下设计：
- ✅ **使用 NetworkClient 的 OkHttpClient**：享受所有拦截器（日志、认证、重试等）
- ✅ **使用 NetworkClient 的 RequestConfig**：统一配置请求头、查询参数等
- ✅ **直接使用 OkHttpClient**：进行流式下载，不经过 Retrofit 的 JSON 解析
- ✅ **Flow 状态管理**：提供响应式的下载状态流

---

## 🚀 快速开始

### 1. 依赖注入 OkHttpClient

`FileDownloader` 需要 `OkHttpClient` 实例，可以通过依赖注入获取：

```kotlin
@AndroidEntryPoint
class AppUpdateActivity : BaseActivity<ActivityAppUpdateBinding>() {
    
    @Inject
    lateinit var okHttpClient: OkHttpClient
    
    private val fileDownloader: FileDownloader by lazy {
        FileDownloader(okHttpClient)
    }
}
```

### 2. 基本使用

```kotlin
lifecycleScope.launch {
    fileDownloader.download(url, file)
        .collect { state ->
            when (state) {
                is DownloadState.Preparing -> {
                    // 准备下载
                }
                
                is DownloadState.Downloading -> {
                    // 更新进度
                    progressBar.progress = state.progress
                    speedText.text = "${state.speed.formatFileSize()}/s"
                }
                
                is DownloadState.Completed -> {
                    // 下载完成
                }
                
                is DownloadState.Failed -> {
                    // 下载失败
                }
                
                is DownloadState.Cancelled -> {
                    // 下载已取消
                }
            }
        }
}
```

### 3. 使用 RequestConfig 配置请求

`FileDownloader` 支持使用 `NetworkClient.RequestConfig` 来配置请求，与 `NetworkClient` 保持一致：

```kotlin
import com.jun.core.network.client.requestConfig

// 使用 RequestConfig 配置请求
val config = requestConfig {
    header("Authorization", "Bearer token")
    header("User-Agent", "MyApp/1.0")
    queryParam("version", "1.0.0")
    queryParam("platform", "android")
    pathParam("id", "123")
}

lifecycleScope.launch {
    fileDownloader.download(url, file, config)
        .collect { state ->
            // 处理下载状态
        }
}
```

这样可以使用与 `NetworkClient` 相同的配置方式，保持代码风格一致。

---

## ✨ 核心功能

### 1. 下载状态

`DownloadState` 是一个密封类，包含以下状态：

- `Preparing` - 准备下载
- `Downloading` - 下载中（包含进度、已下载字节、总字节、速度）
- `Completed` - 下载完成（包含文件）
- `Failed` - 下载失败（包含错误信息）
- `Cancelled` - 下载已取消

### 2. 下载文件

```kotlin
// 基础下载
val downloadJob = lifecycleScope.launch {
    fileDownloader.download(url, file)
        .collect { state ->
            when (state) {
                is DownloadState.Downloading -> {
                    // 更新 UI
                    updateProgress(state)
                }
                is DownloadState.Completed -> {
                    // 处理下载完成
                    handleDownloadComplete(state.file)
                }
                is DownloadState.Failed -> {
                    // 处理下载失败
                    handleDownloadError(state.error)
                }
                else -> {}
            }
        }
}

// 使用 RequestConfig 配置请求
val config = requestConfig {
    header("Authorization", "Bearer token")
    queryParam("version", "1.0.0")
}

val downloadJobWithConfig = lifecycleScope.launch {
    fileDownloader.download(url, file, config)
        .collect { state ->
            // 处理下载状态
        }
}

// 取消下载
downloadJob.cancel()
```

### 3. 简化版本（不接收详细进度）

如果不需要详细的下载进度，可以使用 `downloadSimple`：

```kotlin
lifecycleScope.launch {
    fileDownloader.downloadSimple(url, file)
        .collect { state ->
            when (state) {
                is DownloadState.Completed -> {
                    // 下载完成
                }
                is DownloadState.Failed -> {
                    // 下载失败
                }
                else -> {}
            }
        }
}
```

---

## 📝 完整示例

### 示例 1：App 升级下载

```kotlin
@AndroidEntryPoint
class AppUpdateActivity : BaseActivity<ActivityAppUpdateBinding>() {
    
    @Inject
    lateinit var okHttpClient: OkHttpClient
    
    private val fileDownloader: FileDownloader by lazy {
        FileDownloader(okHttpClient)
    }
    
    private var downloadJob: Job? = null
    private val apkFile: File by lazy {
        File(getExternalFilesDir("apk"), "app_update.apk")
    }
    
    private fun startDownload() {
        // 使用 RequestConfig 配置请求（可选）
        val config = requestConfig {
            // 如果需要认证，可以添加请求头
            // header("Authorization", "Bearer token")
            // 如果需要查询参数
            // queryParam("version", "1.0.0")
        }
        
        downloadJob = lifecycleScope.launch {
            fileDownloader.download(apkDownloadUrl, apkFile, config)
                .collect { state ->
                    when (state) {
                        is DownloadState.Preparing -> {
                            binding.tvStatus.text = "准备下载..."
                        }
                        
                        is DownloadState.Downloading -> {
                            // 更新进度
                            if (state.progress >= 0) {
                                binding.progressBar.progress = state.progress
                                binding.tvProgressText.text = "${state.progress}%"
                            }
                            
                            // 更新状态信息
                            val downloaded = state.downloadedBytes.formatFileSize()
                            val speed = state.speed.formatFileSize()
                            if (state.totalBytes > 0) {
                                val total = state.totalBytes.formatFileSize()
                                binding.tvStatus.text = "下载中: $downloaded / $total ($speed/s)"
                            } else {
                                binding.tvStatus.text = "下载中: $downloaded ($speed/s)"
                            }
                        }
                        
                        is DownloadState.Completed -> {
                            // 下载完成
                            binding.progressBar.progress = 100
                            binding.btnInstall.visibility = View.VISIBLE
                            showSuccess("下载完成")
                        }
                        
                        is DownloadState.Failed -> {
                            // 下载失败
                            showError("下载失败: ${state.error}")
                        }
                        
                        is DownloadState.Cancelled -> {
                            // 下载已取消
                            showMessage("下载已取消")
                        }
                    }
                }
        }
    }
    
    private fun cancelDownload() {
        downloadJob?.cancel()
    }
}
```

### 示例 2：图片下载

```kotlin
class ImageDownloader(
    private val okHttpClient: OkHttpClient
) {
    private val fileDownloader = FileDownloader(okHttpClient)
    
    suspend fun downloadImage(
        url: String,
        file: File
    ): Result<File> = suspendCoroutine { continuation ->
        CoroutineScope(Dispatchers.Main).launch {
            fileDownloader.download(url, file)
                .collect { state ->
                    when (state) {
                        is DownloadState.Completed -> {
                            continuation.resume(Result.success(state.file))
                        }
                        is DownloadState.Failed -> {
                            continuation.resume(Result.failure(Exception(state.error)))
                        }
                        else -> {}
                    }
                }
        }
    }
}
```

---

## 🎯 最佳实践

### 1. 使用 Flow 收集（推荐）

```kotlin
// ✅ 推荐：使用 Flow 收集
lifecycleScope.launch {
    fileDownloader.download(url, file)
        .collect { state ->
            // 处理状态
        }
}
```

### 2. 在 Activity/Fragment 中使用

```kotlin
// ✅ 推荐：使用 lifecycleScope
class MyActivity : BaseActivity<Binding>() {
    private var downloadJob: Job? = null
    
    private fun download() {
        downloadJob = lifecycleScope.launch {
            fileDownloader.download(url, file)
                .collect { state ->
                    // 更新 UI
                }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        downloadJob?.cancel()
    }
}
```

### 3. 错误处理

```kotlin
lifecycleScope.launch {
    fileDownloader.download(url, file)
        .catch { e ->
            // 处理异常
            Timber.e(e, "下载异常")
        }
        .collect { state ->
            when (state) {
                is DownloadState.Failed -> {
                    // 处理下载失败
                    showError(state.error)
                }
                else -> {}
            }
        }
}
```

### 4. 取消下载

```kotlin
private var downloadJob: Job? = null

private fun startDownload() {
    downloadJob = lifecycleScope.launch {
        fileDownloader.download(url, file)
            .collect { state ->
                // 处理状态
            }
    }
}

private fun cancelDownload() {
    downloadJob?.cancel()
    downloadJob = null
}
```

---

## ⚠️ 注意事项

1. **文件路径**：确保文件保存路径的目录存在，`FileDownloader` 会自动创建目录
2. **文件覆盖**：如果文件已存在，`FileDownloader` 会自动删除旧文件
3. **取消下载**：取消下载会删除部分下载的文件
4. **线程安全**：`FileDownloader` 是线程安全的，可以在任何线程使用
5. **进度更新**：下载进度每秒更新一次，避免频繁更新 UI
6. **下载速度**：下载速度基于每秒的字节增量计算

---

## 🔍 技术细节

### 下载流程

1. **准备阶段**：创建目录，发送 `Preparing` 状态
2. **下载阶段**：流式读取响应体，每秒更新一次进度，发送 `Downloading` 状态
3. **完成阶段**：下载完成，发送 `Completed` 状态
4. **错误处理**：捕获异常，发送 `Failed` 或 `Cancelled` 状态

### 性能优化

- 使用 8KB 缓冲区进行流式读取
- 进度更新限制为每秒一次
- 自动检查协程取消状态
- 使用 `FileOutputStream.use` 确保资源释放

---

## ✨ 总结

`FileDownloader` 提供了：

- ✅ **基于框架**：使用 core-network 的 `OkHttpClient` 和 `RequestConfig`
- ✅ **Flow 支持**：响应式下载状态管理
- ✅ **进度追踪**：实时下载进度和速度
- ✅ **错误处理**：统一的错误处理机制
- ✅ **取消支持**：支持取消下载
- ✅ **线程安全**：可在任何线程使用
- ✅ **配置统一**：与 NetworkClient 使用相同的 RequestConfig

### 与 NetworkClient 的关系

`FileDownloader` 和 `NetworkClient` 是互补的关系：
- **NetworkClient**：用于 JSON API 请求，自动解析响应
- **FileDownloader**：用于文件下载，流式处理，支持进度回调

两者都使用相同的：
- `OkHttpClient`（享受所有拦截器）
- `RequestConfig`（统一配置方式）
- 错误处理机制

**建议：在需要文件下载的场景中使用 `FileDownloader`！**

