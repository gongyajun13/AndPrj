# App 升级功能优化建议

## 1. 下载进度缓存优化 ⚡

### 问题
- 目前每次下载状态更新都会调用 `cacheManager.updateDownloadProgress()`
- 可能导致频繁的 DataStore 写入，影响性能

### 优化方案
- 添加节流机制（throttle），例如每 1 秒或每 5% 进度更新一次缓存
- 使用 `flow.throttle()` 或自定义节流逻辑

### 实现示例
```kotlin
// 在 ViewModel 中添加节流逻辑
private var lastCacheUpdateTime = 0L
private val CACHE_UPDATE_INTERVAL = 1000L // 1秒更新一次

is DownloadState.Downloading -> {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastCacheUpdateTime >= CACHE_UPDATE_INTERVAL) {
        cacheManager.updateDownloadProgress(state.downloadedBytes, state.totalBytes)
        lastCacheUpdateTime = currentTime
    }
    // ... 更新 UI 状态
}
```

---

## 2. 网络状态检查 🌐

### 问题
- 下载前没有检查网络连接状态
- 可能导致无效的下载请求

### 优化方案
- 在 `checkUpdate()` 和 `startDownload()` 前检查网络状态
- 使用已有的 `NetworkMonitor` 工具类

### 实现示例
```kotlin
@Inject
lateinit var networkMonitor: NetworkMonitor

fun checkUpdate() {
    if (!networkMonitor.isNetworkAvailable()) {
        updateAppUpdateState(AppUpdateUiState.CheckFailed("网络不可用，请检查网络连接"))
        return
    }
    // ... 继续检查更新
}
```

---

## 3. 强制更新处理 🔒

### 问题
- 强制更新时，用户仍然可以取消下载
- 应该阻止用户取消强制更新

### 优化方案
- 在 `UpdateAvailable` 状态中检查 `isForceUpdate`
- 强制更新时隐藏取消按钮，禁用返回键

### 实现示例
```kotlin
// 在 Activity 中
is AppUpdateUiState.UpdateAvailable -> {
    if (state.isForceUpdate) {
        binding.btnCancel.visibility = View.GONE
        binding.btnCancel.isEnabled = false
    }
}

// 在 ViewModel 中
fun cancelDownload() {
    val currentState = _appUpdateState.value
    if (currentState is AppUpdateUiState.UpdateAvailable && currentState.isForceUpdate) {
        Timber.w("强制更新不允许取消")
        return
    }
    // ... 取消下载逻辑
}
```

---

## 4. 错误处理优化 🛡️

### 问题
- 错误信息不够详细
- 没有区分不同类型的错误（网络错误、超时、文件系统错误等）

### 优化方案
- 细化错误类型，提供更友好的错误提示
- 根据错误类型提供不同的处理建议

### 实现示例
```kotlin
sealed class DownloadError {
    data class NetworkError(val message: String) : DownloadError()
    data class TimeoutError(val message: String) : DownloadError()
    data class FileSystemError(val message: String) : DownloadError()
    data class UnknownError(val message: String) : DownloadError()
}

// 在下载失败时
catch (e: SocketTimeoutException) {
    val error = DownloadError.TimeoutError("下载超时，请检查网络连接")
    updateAppUpdateState(AppUpdateUiState.DownloadFailed(error.message, canRetry = true))
}
```

---

## 5. 文件完整性验证 🔍

### 问题
- 目前只是简单的大小比较
- 无法检测文件是否损坏

### 优化方案
- 如果服务器提供 MD5/SHA256 校验值，添加文件校验
- 在 `AppUpdateCacheData` 中添加 `fileHash` 字段

### 实现示例
```kotlin
data class AppUpdateCacheData(
    // ... 现有字段
    val fileHash: String? = null, // MD5 或 SHA256
    val hashAlgorithm: String? = null // "MD5" 或 "SHA256"
)

// 验证文件完整性
fun verifyFileIntegrity(file: File, expectedHash: String, algorithm: String): Boolean {
    val actualHash = when (algorithm) {
        "MD5" -> FileUtils.getFileMD5(file)
        "SHA256" -> FileUtils.getFileSHA256(file)
        else -> return false
    }
    return actualHash.equals(expectedHash, ignoreCase = true)
}
```

---

## 6. 检查更新 API 集成 🔌

### 问题
- 目前是模拟数据（TODO 标记）
- 需要替换为真实的 API 调用

### 优化方案
- 创建 `UpdateApi` 接口
- 使用 Retrofit 调用服务器 API
- 处理 API 响应和错误

### 实现示例
```kotlin
interface UpdateApi {
    @GET("/api/update/check")
    suspend fun checkUpdate(
        @Query("versionCode") versionCode: Int,
        @Query("versionName") versionName: String
    ): ApiResponse<UpdateInfo>
}

data class UpdateInfo(
    val latestVersion: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val updateInfo: String,
    val isForceUpdate: Boolean,
    val fileSize: Long,
    val fileHash: String? = null,
    val hashAlgorithm: String? = null
)
```

---

## 7. 代码优化 🧹

### 问题
- 有一些重复代码
- 状态管理可以更清晰

### 优化方案
- 提取重复的 UI 更新逻辑
- 优化状态转换逻辑

### 实现示例
```kotlin
// 提取公共方法
private fun updateProgressUI(
    downloadedBytes: Long,
    totalBytes: Long,
    speed: Long = 0L,
    estimatedTime: Long = -1L
) {
    val progress = if (totalBytes > 0) {
        ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
    } else {
        0
    }
    // ... 更新 UI
}
```

---

## 8. 下载速度显示优化 📊

### 问题
- 下载速度可能波动较大
- 用户体验不够平滑

### 优化方案
- 使用移动平均算法平滑下载速度
- 显示平均速度而不是瞬时速度

### 实现示例
```kotlin
private val speedHistory = mutableListOf<Long>()
private val MAX_SPEED_HISTORY = 10

private fun calculateAverageSpeed(currentSpeed: Long): Long {
    speedHistory.add(currentSpeed)
    if (speedHistory.size > MAX_SPEED_HISTORY) {
        speedHistory.removeAt(0)
    }
    return speedHistory.average().toLong()
}
```

---

## 9. 后台下载支持 📱

### 问题
- 目前只能在 Activity 中下载
- 用户切换到后台时下载可能中断

### 优化方案
- 使用 WorkManager 或 Foreground Service 支持后台下载
- 添加下载通知

---

## 10. 下载重试机制 🔄

### 问题
- 下载失败后需要手动重试
- 没有自动重试机制

### 优化方案
- 添加自动重试逻辑（最多重试 3 次）
- 使用指数退避策略

### 实现示例
```kotlin
private var retryCount = 0
private val MAX_RETRY_COUNT = 3

private suspend fun downloadWithRetry(url: String, file: File) {
    var attempt = 0
    while (attempt < MAX_RETRY_COUNT) {
        try {
            fileDownloader.download(url, file).collect { state ->
                // ... 处理状态
            }
            break // 成功，退出循环
        } catch (e: Exception) {
            attempt++
            if (attempt < MAX_RETRY_COUNT) {
                val delay = (2.0.pow(attempt) * 1000).toLong() // 指数退避
                delay(delay)
            } else {
                // 重试失败
                updateAppUpdateState(AppUpdateUiState.DownloadFailed("下载失败，已重试 $MAX_RETRY_COUNT 次"))
            }
        }
    }
}
```

---

## 优先级建议

1. **高优先级**：
   - 下载进度缓存优化（性能影响）
   - 网络状态检查（用户体验）
   - 检查更新 API 集成（功能完整性）

2. **中优先级**：
   - 强制更新处理（业务需求）
   - 错误处理优化（用户体验）
   - 代码优化（可维护性）

3. **低优先级**：
   - 文件完整性验证（如果服务器支持）
   - 下载速度显示优化（体验优化）
   - 后台下载支持（高级功能）
   - 下载重试机制（容错性）

