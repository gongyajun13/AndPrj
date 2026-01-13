# NetworkClient 使用指南

## 📦 概述

`NetworkClient` 是一个通用的网络请求工具类，提供了完整的 HTTP 请求方法（GET、POST、PUT、DELETE、PATCH）和丰富的拦截器支持。

**重要**：`NetworkClient` 已通过 Hilt 提供单例实例，**无需每次创建新对象**，直接通过依赖注入使用即可。

## 🚀 快速开始

### 1. 通过依赖注入使用 NetworkClient（推荐）

`NetworkClient` 已在 `core-network` 模块的 `NetworkModule` 中配置为单例，直接注入使用：

```kotlin
// 在 ViewModel 中使用
@HiltViewModel
class UserViewModel @Inject constructor(
    private val networkClient: NetworkClient  // ✅ 单例，全局共享
) : BaseViewModel<UiState<List<User>>>() {
    
    fun loadUsers() {
        executeAsync(
            block = { 
                networkClient.get<List<User>>("/api/users")
            }
        )
    }
    
    override fun createInitialState(): UiState<List<User>> {
        return UiState.Initial
    }
}

// 在 Activity/Fragment 中使用
@AndroidEntryPoint
class UserListActivity : BaseActivity<ActivityUserListBinding>() {
    
    @Inject
    lateinit var networkClient: NetworkClient  // ✅ 单例，全局共享
    
    override fun setupViews() {
        // 使用 networkClient...
    }
}
```

### 2. 使用 NetworkClientBuilder 创建自定义 NetworkClient（高级用法）

如果需要创建具有特殊配置的 `NetworkClient` 实例（例如不同的 baseUrl），可以使用 `NetworkClientBuilder`：

```kotlin
import com.jun.core.network.client.networkClient
import com.jun.core.common.network.NetworkMonitor
import com.jun.core.network.interceptor.AuthTokenProvider

// 创建 NetworkClient（完整配置）
val networkClient = networkClient {
    baseUrl("https://api.example.com/")
    
    // 超时配置
    timeouts(
        connectSeconds = 30,
        readSeconds = 30,
        writeSeconds = 30
    )
    
    // 认证拦截器
    auth(
        tokenProvider = object : AuthTokenProvider {
            override fun getToken(): String? = "your_token_here"
        },
        headerName = "Authorization",
        tokenPrefix = "Bearer "
    )
    
    // 网络状态拦截器
    networkStatus(
        networkMonitor = networkMonitor,
        enabled = true
    )
    
    // 请求去重拦截器
    deduplication(
        enabled = true,
        windowMillis = 1000
    )
    
    // 日志拦截器
    logging(
        enabled = true,
        level = LoggingInterceptor.LogLevel.BODY,
        formatJson = true,
        maxBodyLength = 2000
    )
    
    // 缓存拦截器
    cache(
        context = context,
        enabled = true,
        maxAgeSeconds = 60,
        maxStaleSeconds = 7 * 24 * 60 * 60
    )
    
    // 重试拦截器
    retry(
        enabled = true,
        maxRetries = 3,
        delayMillis = 1000
    )
    
    // 响应验证拦截器
    responseValidation(enabled = true)
}
```

### 2. 基本使用

#### GET 请求

```kotlin
// 简单 GET 请求
val result = networkClient.get<User>(
    url = "/api/user/123"
)

// 带查询参数和请求头
val result = networkClient.get<ArticleList>(
    url = "/api/articles",
    config = requestConfig {
        header("Authorization", "Bearer token")
        queryParam("page", "1")
        queryParam("size", "20")
    }
)
```

#### POST 请求

```kotlin
// POST 请求（带请求体）
val result = networkClient.post<Article, CreateArticleRequest>(
    url = "/api/articles",
    body = CreateArticleRequest(
        title = "标题",
        content = "内容"
    ),
    config = requestConfig {
        header("Content-Type", "application/json")
    }
)
```

#### PUT 请求

```kotlin
val result = networkClient.put<User, UpdateUserRequest>(
    url = "/api/user/{id}",
    body = UpdateUserRequest(name = "新名称"),
    config = requestConfig {
        pathParam("id", "123")
    }
)
```

#### DELETE 请求

```kotlin
val result = networkClient.delete<Unit>(
    url = "/api/article/{id}",
    config = requestConfig {
        pathParam("id", "123")
    }
)
```

#### PATCH 请求

```kotlin
val result = networkClient.patch<User, PatchUserRequest>(
    url = "/api/user/{id}",
    body = PatchUserRequest(name = "新名称"),
    config = requestConfig {
        pathParam("id", "123")
    }
)
```

### 3. 带缓存的请求

```kotlin
// 创建内存缓存
val cache = MemoryNetworkCache<String, ArticleListResponse>(
    maxSize = 50,
    ttlMillis = 5 * 60 * 1000 // 5分钟
)

// 使用缓存策略
val result = networkClient.get<ArticleListResponse>(
    url = "/api/articles",
    config = requestConfig {
        cache(cache)
        cacheKey("article_list_1")
        cachePolicy(CachePolicy.NETWORK_FIRST)
    }
)
```

## 🔧 拦截器配置详解

### 1. 网络状态拦截器 (NetworkStatusInterceptor)

在网络不可用时提前返回错误，避免无效请求。

```kotlin
networkStatus(
    networkMonitor = networkMonitor,
    enabled = true
)
```

**功能：**
- ✅ 提前检测网络状态
- ✅ 避免无效的网络请求
- ✅ 节省流量和电量

### 2. BaseUrl 拦截器 (BaseUrlInterceptor)

动态切换 API 的 BaseUrl。

```kotlin
val baseUrlInterceptor = BaseUrlInterceptor("https://api.example.com/")
networkClient {
    baseUrlInterceptor(baseUrlInterceptor)
    // 后续可以动态切换
    // baseUrlInterceptor.setBaseUrl("https://api2.example.com/")
}
```

**功能：**
- ✅ 动态切换 BaseUrl
- ✅ 支持多环境切换
- ✅ 线程安全

### 3. 认证拦截器 (AuthInterceptor)

自动在请求头中添加认证 token。

```kotlin
auth(
    tokenProvider = object : AuthTokenProvider {
        override fun getToken(): String? = "your_token_here"
        override suspend fun refreshToken(): String? = "refreshed_token"
    },
    headerName = "Authorization",
    tokenPrefix = "Bearer "
)
```

**功能：**
- ✅ 自动添加认证 token
- ✅ 支持 token 刷新
- ✅ 可配置 header 名称和前缀

### 4. 请求去重拦截器 (DeduplicationInterceptor)

防止短时间内重复发送相同的请求。

```kotlin
deduplication(
    enabled = true,
    windowMillis = 1000 // 1秒内的重复请求会被去重
)
```

**功能：**
- ✅ 防止重复请求
- ✅ 自动合并相同请求
- ✅ 可配置去重时间窗口

**使用场景：**
- 快速连续点击导致的重复请求
- 列表刷新和加载更多同时触发
- 多个组件同时请求相同数据

### 5. 日志拦截器 (LoggingInterceptor)

记录请求和响应的详细信息。

```kotlin
logging(
    enabled = true,
    level = LoggingInterceptor.LogLevel.BODY,
    formatJson = true,
    maxBodyLength = 2000
)
```

**功能：**
- ✅ 详细的请求日志
- ✅ JSON 格式化
- ✅ curl 命令打印
- ✅ 长日志自动换行

### 6. 缓存拦截器 (CacheInterceptor & OfflineCacheInterceptor)

提供 HTTP 缓存功能，减少网络请求。

```kotlin
cache(
    context = context,
    enabled = true,
    maxAgeSeconds = 60,        // 缓存最大存活时间
    maxStaleSeconds = 7 * 24 * 60 * 60 // 离线缓存时间（7天）
)
```

**功能：**
- ✅ HTTP 缓存支持
- ✅ 离线缓存支持
- ✅ 自动缓存管理
- ✅ 可配置缓存时间

### 7. 重试拦截器 (RetryInterceptor)

在网络请求失败时自动重试。

```kotlin
retry(
    enabled = true,
    maxRetries = 3,        // 最大重试次数
    delayMillis = 1000    // 重试延迟（毫秒）
)
```

**功能：**
- ✅ 自动重试失败请求
- ✅ 可配置重试次数和延迟
- ✅ 支持指数退避

### 8. 响应验证拦截器 (ResponseValidationInterceptor)

验证响应体格式，防止数据类型不匹配。

```kotlin
responseValidation(enabled = true)
```

**功能：**
- ✅ 验证响应体是否为有效的 JSON
- ✅ 在解析前发现格式错误
- ✅ 返回明确的错误响应

## 📋 拦截器执行顺序

拦截器按照以下顺序执行（从前往后）：

1. **NetworkStatusInterceptor** - 网络状态检查（最前面）
2. **BaseUrlInterceptor** - BaseUrl 动态切换
3. **AuthInterceptor** - 认证 token 添加
4. **DeduplicationInterceptor** - 请求去重
5. **RetryInterceptor** - 请求重试
6. **LoggingInterceptor** - 日志记录
7. **ResponseValidationInterceptor** - 响应验证
8. **CacheInterceptor** - HTTP 缓存（网络拦截器）
9. **OfflineCacheInterceptor** - 离线缓存（应用拦截器）

## 🎯 完整示例

```kotlin
// 1. 创建 NetworkMonitor
val networkMonitor = NetworkMonitorImpl(context)

// 2. 创建 NetworkClient
val networkClient = networkClient {
    baseUrl("https://api.example.com/")
    
    // 网络状态检查
    networkStatus(networkMonitor, enabled = true)
    
    // 认证
    auth(
        tokenProvider = object : AuthTokenProvider {
            override fun getToken(): String? = getStoredToken()
        }
    )
    
    // 请求去重
    deduplication(enabled = true, windowMillis = 1000)
    
    // 日志
    logging(
        enabled = BuildConfig.DEBUG,
        level = LoggingInterceptor.LogLevel.BODY,
        formatJson = true
    )
    
    // 缓存
    cache(
        context = context,
        enabled = true,
        maxAgeSeconds = 60,
        maxStaleSeconds = 7 * 24 * 60 * 60
    )
    
    // 重试
    retry(enabled = true, maxRetries = 3)
    
    // 响应验证
    responseValidation(enabled = true)
}

// 3. 使用 NetworkClient
lifecycleScope.launch {
    val result = networkClient.get<ArticleList>(
        url = "/api/articles",
        config = requestConfig {
            queryParam("page", "1")
            cache(cache)
            cacheKey("articles_page_1")
            cachePolicy(CachePolicy.NETWORK_FIRST)
        }
    )
    
    result.onSuccess { articles ->
        // 处理成功
    }.onError { error ->
        // 处理错误
    }
}
```

## ✨ 特性总结

- ✅ **完整的 HTTP 方法支持**：GET、POST、PUT、DELETE、PATCH
- ✅ **丰富的拦截器支持**：8 种拦截器，覆盖所有常见场景
- ✅ **缓存策略支持**：5 种缓存策略，满足不同需求
- ✅ **类型安全**：使用 `reified` 泛型，编译时类型检查
- ✅ **链式配置**：DSL 风格的配置构建器
- ✅ **统一错误处理**：增强的错误处理机制
- ✅ **灵活配置**：支持请求头、查询参数、路径参数等

## 📝 注意事项

1. **拦截器顺序很重要**：按照推荐的顺序配置拦截器，以获得最佳效果
2. **缓存配置**：启用缓存时需要提供 `Context` 或 `Cache` 实例
3. **网络状态检查**：需要提供 `NetworkMonitor` 实例
4. **认证 token**：需要实现 `AuthTokenProvider` 接口
5. **BaseUrl 拦截器**：如果需要动态切换 BaseUrl，需要单独配置

