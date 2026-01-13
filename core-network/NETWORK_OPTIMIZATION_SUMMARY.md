# NetworkClient 优化总结

## 🎯 已完成的优化

### 1. ✅ NetworkInterceptorManager 增强

**优化内容**：
- 添加了 `registerBaseUrlInterceptor()` 和 `registerAuthTokenProvider()` 方法，支持链式调用
- 添加了 `getConfigSummary()` 方法，用于调试和配置检查
- 改进了错误提示，当未注册拦截器时提供明确的错误信息

**使用示例**：
```kotlin
// 链式注册
val interceptor = interceptorManager.registerBaseUrlInterceptor(
    BaseUrlInterceptor("https://api.example.com/")
)

// 获取配置摘要（用于调试）
Timber.d(interceptorManager.getConfigSummary())
```

### 2. ✅ RequestConfigBuilder 便捷方法

**优化内容**：
- 添加了 `authorization()` 方法，快速设置 Authorization 请求头
- 添加了 `contentType()` 方法，快速设置 Content-Type 请求头
- 添加了 `headers()` 方法，支持批量添加请求头

**使用示例**：
```kotlin
// 优化前
requestConfig {
    header("Authorization", "Bearer token123")
    header("Content-Type", "application/json")
}

// 优化后（更简洁）
requestConfig {
    authorization("token123")
    contentType()
}

// 批量添加请求头
requestConfig {
    headers(mapOf(
        "X-Client-Version" to "1.0.0",
        "X-Platform" to "Android"
    ))
}
```

### 3. ✅ Flow 支持

**优化内容**：
- 为所有 HTTP 方法添加了 Flow 版本（`getFlow`, `postFlow`, `putFlow`, `deleteFlow`, `patchFlow`）
- 特别优化了 `CACHE_AND_NETWORK` 策略，可以 emit 缓存和网络结果
- 在 `BaseViewModel` 中添加了 `executeAsyncFlow()` 方法

**使用示例**：
```kotlin
// Flow 版本，支持响应式 UI 刷新
executeAsyncFlow(
    flow = networkClient.getFlow<List<Article>>(
        url = "/api/articles",
        config = requestConfig {
            cache(cache)
            cacheKey("articles")
            cachePolicy(CachePolicy.CACHE_AND_NETWORK)  // ✅ 先显示缓存，再更新网络
        }
    )
)
```

### 4. ✅ 单例化 NetworkClient

**优化内容**：
- 在 `NetworkModule` 中提供 `NetworkClient` 单例
- 在 `NetworkModule` 中提供 `NetworkInterceptorManager` 单例
- 避免重复创建对象，提升性能和资源利用

**使用示例**：
```kotlin
// 直接通过依赖注入使用，无需手动创建
@HiltViewModel
class UserViewModel @Inject constructor(
    private val networkClient: NetworkClient,  // ✅ 单例
    private val interceptorManager: NetworkInterceptorManager  // ✅ 单例
) : BaseViewModel<UiState<List<User>>>() {
    // ...
}
```

## 🚀 进一步优化建议

### 1. 扩展函数优化（可选）

可以添加更多便捷的扩展函数：

```kotlin
// 为 String 添加扩展函数，简化 URL 构建
fun String.withQueryParams(vararg params: Pair<String, String>): String {
    // ...
}

// 为 NetworkClient 添加扩展函数，简化常用操作
fun NetworkClient.getWithCache<T>(
    url: String,
    cache: NetworkCache<String, T>,
    cacheKey: String
): AppResult<T> {
    // ...
}
```

### 2. 请求重试策略优化（可选）

可以添加更灵活的重试策略：

```kotlin
// 支持指数退避
data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelayMillis: Long = 1000,
    val maxDelayMillis: Long = 10000,
    val backoffMultiplier: Double = 2.0,
    val retryableExceptions: List<Class<out Throwable>> = emptyList()
)
```

### 3. 请求去重优化（可选）

可以添加更智能的去重策略：

```kotlin
// 支持基于请求内容的去重
class SmartDeduplicationInterceptor(
    private val windowMillis: Long,
    private val includeBody: Boolean = false  // 是否包含请求体
) : Interceptor {
    // ...
}
```

### 4. 网络状态监听优化（可选）

可以添加网络状态变化监听：

```kotlin
// 监听网络状态变化，自动调整请求策略
interface NetworkStateListener {
    fun onNetworkAvailable()
    fun onNetworkUnavailable()
}
```

### 5. 请求优先级（可选）

可以添加请求优先级支持：

```kotlin
enum class RequestPriority {
    LOW, NORMAL, HIGH, URGENT
}

// 在 RequestConfig 中添加
val priority: RequestPriority = RequestPriority.NORMAL
```

## 📊 优化效果对比

| 优化项 | 优化前 | 优化后 | 提升 |
|--------|--------|--------|------|
| 对象创建 | 每次创建新实例 | 单例，全局共享 | ✅ 减少内存占用 |
| 配置便捷性 | 需要手动设置每个 header | 提供便捷方法 | ✅ 代码更简洁 |
| Flow 支持 | 不支持 | 完整支持 | ✅ 响应式 UI 刷新 |
| 拦截器管理 | 手动管理 | 统一管理器 | ✅ 更易维护 |
| 错误提示 | 不明确 | 明确的错误信息 | ✅ 更易调试 |

## 🎯 最佳实践

### 1. 使用单例 NetworkClient

```kotlin
// ✅ 推荐：通过依赖注入使用
@HiltViewModel
class UserViewModel @Inject constructor(
    private val networkClient: NetworkClient
) : BaseViewModel<UiState<List<User>>>() {
    // ...
}

// ❌ 不推荐：每次创建新实例
val networkClient = networkClient { ... }
```

### 2. 使用便捷方法

```kotlin
// ✅ 推荐：使用便捷方法
requestConfig {
    authorization("token123")
    contentType()
    queryParam("page", "1")
}

// ❌ 不推荐：手动设置每个 header
requestConfig {
    header("Authorization", "Bearer token123")
    header("Content-Type", "application/json")
    queryParam("page", "1")
}
```

### 3. 使用 Flow 进行响应式更新

```kotlin
// ✅ 推荐：使用 Flow 版本（特别是 CACHE_AND_NETWORK 策略）
executeAsyncFlow(
    flow = networkClient.getFlow<List<Article>>(
        url = "/api/articles",
        config = requestConfig {
            cache(cache)
            cacheKey("articles")
            cachePolicy(CachePolicy.CACHE_AND_NETWORK)
        }
    )
)

// ⚠️ 简单场景可以使用同步版本
executeAsync(
    block = { networkClient.get<List<Article>>("/api/articles") }
)
```

### 4. 使用 NetworkInterceptorManager 管理拦截器

```kotlin
// ✅ 推荐：通过管理器动态调整
interceptorManager.switchBaseUrl("https://api.production.com/")

// ❌ 不推荐：直接访问拦截器实例
baseUrlInterceptor.setBaseUrl("https://api.production.com/")
```

## 📝 总结

通过以上优化，`NetworkClient` 现在具备：

1. ✅ **单例化**：避免重复创建，提升性能
2. ✅ **Flow 支持**：完整的响应式 UI 刷新支持
3. ✅ **便捷方法**：更简洁的 API 使用
4. ✅ **统一管理**：通过 `NetworkInterceptorManager` 统一管理拦截器
5. ✅ **动态调整**：支持运行时动态调整 BaseUrl 和 Token

这些优化使得 `NetworkClient` 更加易用、高效和灵活，完全符合现代 Android 开发的最佳实践。







