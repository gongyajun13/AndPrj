# 网络请求优化功能使用指南

## 📦 新增优化功能

框架已添加了多种网络请求优化功能，提升性能和用户体验。

---

## 🚀 优化功能列表

### 1. HTTP 缓存拦截器 (CacheInterceptor)

提供 HTTP 缓存功能，减少重复的网络请求。

```kotlin
import com.jun.core.network.interceptor.CacheInterceptor

// 创建缓存拦截器
val cacheInterceptor = CacheInterceptor(
    maxAge = 60,              // 缓存最大存活时间（秒）
    maxStale = 7 * 24 * 60 * 60 // 离线缓存最大存活时间（秒，默认7天）
)

// 添加到 OkHttpClient
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(cacheInterceptor)
    .cache(OkHttpCacheHelper.createCache(context)) // 需要配置缓存
    .build()
```

**功能：**
- ✅ 自动为成功响应添加缓存头
- ✅ 可配置缓存存活时间
- ✅ 支持离线缓存

### 2. 离线缓存拦截器 (OfflineCacheInterceptor)

当网络不可用时，自动使用缓存数据。

```kotlin
import com.jun.core.network.interceptor.OfflineCacheInterceptor

// 创建离线缓存拦截器
val offlineCacheInterceptor = OfflineCacheInterceptor(
    maxStale = 7 * 24 * 60 * 60 // 离线缓存最大存活时间（秒）
)

// 添加到 OkHttpClient（应该在网络拦截器之前）
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(offlineCacheInterceptor)
    .addNetworkInterceptor(cacheInterceptor) // 网络拦截器
    .cache(OkHttpCacheHelper.createCache(context))
    .build()
```

**功能：**
- ✅ 网络不可用时自动使用缓存
- ✅ 可配置离线缓存时间
- ✅ 提升离线体验

### 3. 网络状态拦截器 (NetworkStatusInterceptor)

结合 NetworkMonitor，在网络不可用时直接返回错误，避免无效请求。

```kotlin
import com.jun.core.network.interceptor.NetworkStatusInterceptor
import com.jun.core.common.network.NetworkMonitor

// 创建网络状态拦截器
val networkStatusInterceptor = NetworkStatusInterceptor(
    networkMonitor = networkMonitor // 注入 NetworkMonitor
)

// 添加到 OkHttpClient（应该在最前面）
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(networkStatusInterceptor)
    .build()
```

**功能：**
- ✅ 提前检测网络状态
- ✅ 避免无效的网络请求
- ✅ 节省流量和电量

### 4. 请求去重拦截器 (DeduplicationInterceptor)

防止短时间内重复发送相同的请求。

```kotlin
import com.jun.core.network.interceptor.DeduplicationInterceptor

// 创建去重拦截器
val deduplicationInterceptor = DeduplicationInterceptor(
    deduplicationWindowMillis = 1000 // 去重时间窗口（毫秒）
)

// 添加到 OkHttpClient
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(deduplicationInterceptor)
    .build()
```

**功能：**
- ✅ 防止重复请求
- ✅ 自动合并相同请求
- ✅ 可配置去重时间窗口

**使用场景：**
- 快速连续点击导致的重复请求
- 列表刷新和加载更多同时触发
- 多个组件同时请求相同数据

### 5. 内存缓存 (MemoryNetworkCache)

提供内存缓存功能，用于缓存网络请求结果。

```kotlin
import com.jun.core.network.cache.MemoryNetworkCache

// 创建内存缓存
val cache = MemoryNetworkCache<String, User>(
    maxSize = 50,              // 最大缓存数量
    ttlMillis = 5 * 60 * 1000  // 缓存过期时间（毫秒，默认5分钟）
)

// 使用缓存
val cached = cache.get("user_123")
cache.put("user_123", user)
```

**功能：**
- ✅ 内存缓存，访问速度快
- ✅ 自动过期清理
- ✅ 可配置缓存大小和过期时间

### 6. 缓存策略 (CachePolicy)

提供多种缓存策略，满足不同场景需求。

```kotlin
import com.jun.core.network.cache.CachePolicy
import com.jun.core.network.extension.cachedApiCall

// 策略1：不使用缓存
val result = cachedApiCall(
    cache = cache,
    cacheKey = "user_123",
    cachePolicy = CachePolicy.NO_CACHE,
    apiCall = { userApi.getUser("123") }
)

// 策略2：只使用缓存
val result = cachedApiCall(
    cache = cache,
    cacheKey = "user_123",
    cachePolicy = CachePolicy.CACHE_ONLY,
    apiCall = { userApi.getUser("123") }
)

// 策略3：优先使用缓存（推荐用于列表数据）
val result = cachedApiCall(
    cache = cache,
    cacheKey = "users",
    cachePolicy = CachePolicy.CACHE_FIRST,
    apiCall = { userApi.getUsers() }
)

// 策略4：优先请求网络（推荐用于实时数据）
val result = cachedApiCall(
    cache = cache,
    cacheKey = "user_123",
    cachePolicy = CachePolicy.NETWORK_FIRST,
    apiCall = { userApi.getUser("123") }
)

// 策略5：同时使用缓存和网络（推荐用于详情页）
val result = cachedApiCall(
    cache = cache,
    cacheKey = "user_123",
    cachePolicy = CachePolicy.CACHE_AND_NETWORK,
    apiCall = { userApi.getUser("123") }
)
```

**缓存策略说明：**

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| `NO_CACHE` | 不使用缓存，直接请求网络 | 需要实时数据的场景 |
| `CACHE_ONLY` | 只使用缓存，不发起网络请求 | 离线模式 |
| `CACHE_FIRST` | 优先使用缓存，缓存不存在时请求网络 | 列表数据、配置数据 |
| `NETWORK_FIRST` | 优先请求网络，失败时使用缓存 | 实时数据、用户信息 |
| `CACHE_AND_NETWORK` | 先返回缓存，后台更新网络数据 | 详情页、需要快速响应的场景 |

### 7. 带缓存的网络请求扩展 (CacheExtensions)

提供便捷的缓存请求方法。

```kotlin
import com.jun.core.network.extension.cachedApiCall
import com.jun.core.network.extension.cachedApiCallFlow

// 同步版本
suspend fun getUser(id: String): AppResult<User> {
    return cachedApiCall(
        cache = userCache,
        cacheKey = "user_$id",
        cachePolicy = CachePolicy.NETWORK_FIRST,
        apiCall = { userApi.getUser(id) }
    )
}

// Flow 版本
fun getUserFlow(id: String): Flow<AppResult<User>> {
    return cachedApiCallFlow(
        cache = userCache,
        cacheKey = "user_$id",
        cachePolicy = CachePolicy.CACHE_AND_NETWORK,
        apiCall = { userApi.getUser(id) }
    )
}
```

---

## 🔧 配置示例

### 完整的 NetworkModule 配置

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        networkConfig: NetworkConfig,
        networkMonitor: NetworkMonitor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(networkConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(networkConfig.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(networkConfig.writeTimeoutSeconds, TimeUnit.SECONDS)
        
        // 1. 网络状态检查（最前面）
        if (networkConfig.enableNetworkStatusCheck) {
            builder.addInterceptor(NetworkStatusInterceptor(networkMonitor))
        }
        
        // 2. 请求去重
        if (networkConfig.enableRequestDeduplication) {
            builder.addInterceptor(
                DeduplicationInterceptor(
                    deduplicationWindowMillis = networkConfig.deduplicationWindowMillis
                )
            )
        }
        
        // 3. 日志拦截器
        if (networkConfig.enableLogging) {
            builder.addInterceptor(LoggingInterceptor())
        }
        
        // 4. 缓存配置
        if (networkConfig.enableCache) {
            // 添加缓存
            builder.cache(OkHttpCacheHelper.createCache(context))
            
            // 添加缓存拦截器（网络拦截器）
            builder.addNetworkInterceptor(
                CacheInterceptor(
                    maxAge = networkConfig.cacheMaxAgeSeconds.toInt(),
                    maxStale = networkConfig.cacheMaxStaleSeconds.toInt()
                )
            )
            
            // 添加离线缓存拦截器（应用拦截器）
            builder.addInterceptor(
                OfflineCacheInterceptor(
                    maxStale = networkConfig.cacheMaxStaleSeconds.toInt()
                )
            )
        }
        
        return builder.build()
    }
}
```

---

## 📊 性能优化效果

### 1. 缓存优化
- **减少网络请求**：相同请求直接使用缓存
- **提升响应速度**：缓存数据访问速度快
- **节省流量**：减少重复数据下载
- **离线支持**：网络不可用时仍可使用缓存数据

### 2. 请求去重
- **防止重复请求**：短时间内相同请求只执行一次
- **减少服务器压力**：避免无效的重复请求
- **提升用户体验**：快速点击不会导致多次请求

### 3. 网络状态检查
- **提前失败**：网络不可用时立即返回错误
- **节省电量**：避免无效的网络连接
- **提升响应速度**：不需要等待超时

---

## 🎯 使用建议

### 1. 缓存策略选择

- **列表数据**：使用 `CACHE_FIRST`，优先显示缓存，后台更新
- **详情数据**：使用 `CACHE_AND_NETWORK`，快速显示缓存，后台刷新
- **实时数据**：使用 `NETWORK_FIRST`，优先获取最新数据
- **配置数据**：使用 `CACHE_FIRST`，减少不必要的请求

### 2. 拦截器顺序

拦截器的执行顺序很重要：

```
1. NetworkStatusInterceptor（最前面，提前检查网络）
2. DeduplicationInterceptor（去重，避免重复请求）
3. LoggingInterceptor（日志记录）
4. CacheInterceptor（网络拦截器，缓存响应）
5. OfflineCacheInterceptor（应用拦截器，离线缓存）
```

### 3. 缓存大小配置

- **HTTP 缓存**：建议 10-50MB，根据应用数据量调整
- **内存缓存**：建议 50-100 个条目，根据内存情况调整
- **缓存过期时间**：根据数据更新频率调整

---

## ✨ 总结

框架现在提供了完整的网络优化功能：

- ✅ **HTTP 缓存**：减少网络请求，提升响应速度
- ✅ **离线缓存**：网络不可用时使用缓存数据
- ✅ **请求去重**：防止重复请求，节省资源
- ✅ **网络状态检查**：提前检测，避免无效请求
- ✅ **内存缓存**：快速访问，提升用户体验
- ✅ **多种缓存策略**：满足不同场景需求

**建议：根据实际需求选择合适的缓存策略和拦截器配置！**


