# 框架扩展功能使用指南

本文档介绍框架中新增的可扩展功能及其使用方法。

## 📦 新增功能列表

### 1. 分页支持 (Paging)

#### PagingState - 分页状态管理

```kotlin
sealed class PagingState<out T> {
    object Initial
    object Loading
    data class Success<T>(val items: List<T>, val hasMore: Boolean, val currentPage: Int)
    data class Error(val message: String, val throwable: Throwable?)
    data class LoadingMore<T>(val items: List<T>, val currentPage: Int)
    data class NoMoreData<T>(val items: List<T>)
}
```

#### PagingViewModel - 分页 ViewModel 基类

```kotlin
class UserListViewModel @Inject constructor(
    private val getUserListUseCase: GetUserListUseCase
) : PagingViewModel<User>() {
    
    init {
        loadFirstPage()
    }
    
    override suspend fun loadPage(params: PagingParams): AppResult<PagingData<User>> {
        return getUserListUseCase(params)
    }
}

// 在 Activity/Fragment 中使用
viewModel.items.collect { items ->
    adapter.submitList(items)
}

viewModel.pagingState.collect { state ->
    when (state) {
        is PagingState.Loading -> showLoading()
        is PagingState.Success -> {
            hideLoading()
            if (state.hasMore) {
                // 可以加载更多
            }
        }
        is PagingState.LoadingMore -> {
            // 显示加载更多指示器
        }
        is PagingState.NoMoreData -> {
            // 显示没有更多数据
        }
        is PagingState.Error -> showError(state.message)
        else -> {}
    }
}

// 加载更多
viewModel.loadNextPage()
```

### 2. 网络状态监听 (NetworkMonitor)

```kotlin
// 在 Application 或 DI 模块中提供
@Provides
@Singleton
fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
    return NetworkMonitorImpl(context)
}

// 在 ViewModel 或 Repository 中使用
class MyViewModel @Inject constructor(
    private val networkMonitor: NetworkMonitor
) : BaseViewModel<UiState<Data>>() {
    
    init {
        observeNetworkStatus()
    }
    
    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.networkStatus.collect { status ->
                when (status) {
                    is NetworkStatus.Available -> {
                        // 网络可用，可以执行网络请求
                    }
                    is NetworkStatus.Unavailable -> {
                        // 网络不可用
                    }
                    is NetworkStatus.Lost -> {
                        // 网络连接丢失
                    }
                }
            }
        }
    }
    
    fun checkNetwork(): Boolean {
        return networkMonitor.isNetworkAvailable()
    }
}
```

### 3. 日期时间工具 (DateUtils)

```kotlin
// 格式化日期
val dateStr = DateUtils.format(Date(), DateUtils.Format.DATE_TIME)
// 输出: "2024-01-01 12:00:00"

// 格式化时间戳
val timestamp = System.currentTimeMillis()
val dateStr = DateUtils.format(timestamp, DateUtils.Format.DATE_CN)
// 输出: "2024年01月01日"

// 解析日期字符串
val date = DateUtils.parse("2024-01-01", DateUtils.Format.DATE)

// 获取相对时间
val relativeTime = DateUtils.getRelativeTime(timestamp)
// 输出: "刚刚"、"5分钟前"、"昨天 12:00"、"2024-01-01"

// 判断是否为今天
val isToday = DateUtils.isToday(timestamp)

// 获取今天开始/结束时间戳
val todayStart = DateUtils.todayStartTimestamp()
val todayEnd = DateUtils.todayEndTimestamp()
```

### 4. 数据验证工具 (Validator)

```kotlin
// 验证邮箱
val emailResult = Validator.validateEmail("user@example.com")
when (emailResult) {
    is Validator.ValidationResult.Valid -> {
        // 邮箱格式正确
    }
    is Validator.ValidationResult.Invalid -> {
        // 显示错误信息: emailResult.message
    }
}

// 验证手机号
val phoneResult = Validator.validatePhone("13800138000")

// 验证密码强度
val passwordResult = Validator.validateStrongPassword("MyP@ssw0rd")

// 验证 URL
val urlResult = Validator.validateUrl("https://example.com")

// 验证长度范围
val lengthResult = Validator.validateLength(
    value = "hello",
    minLength = 3,
    maxLength = 10,
    fieldName = "用户名"
)

// 批量验证
val allValid = Validator.validateAll(
    Validator.validateEmail(email),
    Validator.validatePhone(phone),
    Validator.validatePassword(password)
)
```

### 5. 资源管理工具 (ResourceProvider)

```kotlin
// 在 DI 模块中提供
@Provides
@Singleton
fun provideResourceProvider(@ApplicationContext context: Context): ResourceProvider {
    return ContextResourceProvider(context)
}

// 在非 Context 环境中使用（如 Repository、UseCase）
class MyRepository @Inject constructor(
    private val resourceProvider: ResourceProvider
) {
    fun getErrorMessage(): String {
        return resourceProvider.getString(R.string.error_message)
    }
    
    fun getFormattedMessage(count: Int): String {
        return resourceProvider.getString(R.string.item_count, count)
    }
    
    fun getColor(): Int {
        return resourceProvider.getColor(R.color.primary)
    }
}
```

### 6. 缓存管理工具 (CacheManager)

```kotlin
// 在 DI 模块中提供
@Provides
@Singleton
fun provideCacheManager(): CacheManager {
    return MemoryCacheManager()
}

// 使用缓存
class MyRepository @Inject constructor(
    private val cacheManager: CacheManager
) {
    suspend fun getData(key: String): Data? {
        // 先尝试从缓存获取
        val cached = cacheManager.get<Data>(key)
        if (cached != null) {
            return cached
        }
        
        // 从网络获取
        val data = fetchFromNetwork()
        
        // 存入缓存（TTL: 5分钟）
        data?.let {
            cacheManager.put(key, it, ttl = 5 * 60 * 1000)
        }
        
        return data
    }
    
    suspend fun clearCache() {
        cacheManager.clear()
    }
}
```

### 7. 图片加载扩展 (ImageExtensions)

```kotlin
// 加载网络图片
imageView.loadUrl(
    url = "https://example.com/image.jpg",
    placeholder = R.drawable.placeholder,
    error = R.drawable.error,
    crossfade = true
)

// 加载圆形图片
imageView.loadCircle(
    url = "https://example.com/avatar.jpg",
    placeholder = R.drawable.avatar_placeholder
)

// 加载圆角图片
imageView.loadRounded(
    url = "https://example.com/image.jpg",
    radius = 16f,
    placeholder = R.drawable.placeholder
)

// 加载本地资源
imageView.loadResource(R.drawable.local_image)

// 清除图片
imageView.clear()
```

### 8. 协程扩展 (CoroutineExtensions)

```kotlin
// 防抖 - 在指定时间内只执行最后一次操作
flowOf("A", "B", "C")
    .debounce(300)
    .collect { value ->
        // 只处理最后一次值
    }

// 节流 - 在指定时间内只执行第一次操作
flowOf("A", "B", "C")
    .throttle(300)
    .collect { value ->
        // 只处理第一次值
    }

// 添加加载状态
dataFlow
    .withLoading(
        onStart = { showLoading() },
        onComplete = { hideLoading() },
        onError = { error -> showError(error.message) }
    )
    .collect { data ->
        // 处理数据
    }

// 安全启动协程
viewModelScope.safeLaunch(
    onError = { error -> 
        Timber.e(error, "操作失败")
    }
) {
    // 执行可能抛出异常的操作
    performRiskyOperation()
}

// 重试机制
val result = retry(
    times = 3,
    initialDelay = 100,
    maxDelay = 1000,
    factor = 2.0
) {
    networkCall()
}
```

### 9. 网络拦截器

#### 认证拦截器 (AuthInterceptor)

```kotlin
// 实现 AuthTokenProvider
class MyAuthTokenProvider @Inject constructor(
    private val tokenRepository: TokenRepository
) : AuthTokenProvider {
    
    override fun getToken(): String? {
        return tokenRepository.getAccessToken()
    }
    
    override suspend fun refreshToken(): String? {
        return tokenRepository.refreshToken()
    }
}

// 在 NetworkModule 中使用
@Provides
@Singleton
fun provideOkHttpClient(
    authTokenProvider: AuthTokenProvider
): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(authTokenProvider))
        .build()
}
```

#### BaseUrl 拦截器 (BaseUrlInterceptor)

```kotlin
// 在 NetworkModule 中使用
@Provides
@Singleton
fun provideBaseUrlInterceptor(networkConfig: NetworkConfig): BaseUrlInterceptor {
    return BaseUrlInterceptor(networkConfig.baseUrl)
}

@Provides
@Singleton
fun provideOkHttpClient(
    baseUrlInterceptor: BaseUrlInterceptor
): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(baseUrlInterceptor)
        .build()
}

// 动态切换 BaseUrl
baseUrlInterceptor.setBaseUrl("https://new-api.example.com/")
```

## 🎯 使用建议

1. **分页功能**：适用于列表数据加载场景
2. **网络状态监听**：在需要根据网络状态调整行为时使用
3. **日期时间工具**：统一日期格式化，避免重复代码
4. **数据验证**：在表单提交前进行数据验证
5. **资源管理**：在非 Context 环境中访问资源
6. **缓存管理**：减少网络请求，提升用户体验
7. **图片加载扩展**：简化图片加载代码
8. **协程扩展**：提供常用的协程操作模式
9. **网络拦截器**：统一处理认证、BaseUrl 等网络配置

## 📝 注意事项

- 所有功能都设计为可扩展的，可以根据项目需求进行定制
- 缓存管理使用内存缓存，应用重启后数据会丢失
- 网络状态监听需要相应的权限
- 图片加载扩展基于 Coil，确保已添加 Coil 依赖

## 🔄 扩展功能

你可以基于这些基础功能进一步扩展：

- 实现磁盘缓存管理器
- 添加更多数据验证规则
- 实现自定义网络拦截器
- 添加更多图片加载选项
- 扩展日期时间工具功能

