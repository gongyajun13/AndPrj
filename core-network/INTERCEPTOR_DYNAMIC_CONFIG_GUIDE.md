# 拦截器动态配置指南

## 📋 概述

虽然 `NetworkClient` 是单例（通过 Hilt 注入），但某些拦截器支持**动态调整配置**，无需重新创建 `NetworkClient` 实例。

## ✅ 支持动态调整的拦截器

### 1. BaseUrlInterceptor - BaseUrl 动态切换

**支持动态调整**：✅ 是

```kotlin
// 1. 创建 BaseUrlInterceptor 实例（需要在创建 NetworkClient 时传入）
val baseUrlInterceptor = BaseUrlInterceptor("https://api.example.com/")

// 2. 使用 NetworkClientBuilder 创建 NetworkClient（需要手动管理）
val networkClient = networkClient {
    baseUrlInterceptor(baseUrlInterceptor)
    // ... 其他配置
}

// 3. 动态切换 BaseUrl
baseUrlInterceptor.setBaseUrl("https://api2.example.com/")  // ✅ 立即生效
```

### 2. AuthInterceptor - Token 动态更新

**支持动态调整**：✅ 是（通过 AuthTokenProvider）

```kotlin
// 1. 实现 AuthTokenProvider（支持动态 token）
class DynamicAuthTokenProvider : AuthTokenProvider {
    private var token: String? = null
    
    override fun getToken(): String? = token
    
    fun updateToken(newToken: String) {
        this.token = newToken  // ✅ 动态更新 token
    }
}

// 2. 创建 NetworkClient
val tokenProvider = DynamicAuthTokenProvider()
val networkClient = networkClient {
    auth(tokenProvider = tokenProvider)
    // ... 其他配置
}

// 3. 动态更新 token
tokenProvider.updateToken("new_token_here")  // ✅ 下次请求立即生效
```

### 3. LoggingInterceptor - 日志级别

**支持动态调整**：❌ 否（需要在创建时配置）

如果需要动态调整日志级别，需要：
- 方案1：使用 `NetworkInterceptorManager` 管理（但需要重新创建 OkHttpClient）
- 方案2：在创建时根据环境配置（推荐）

## 🔧 使用 NetworkInterceptorManager（推荐方案）

`NetworkInterceptorManager` 提供了统一的管理接口，方便动态调整拦截器配置。

### 1. 通过依赖注入获取管理器

```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val networkClient: NetworkClient,
    private val interceptorManager: NetworkInterceptorManager  // ✅ 注入管理器
) : BaseViewModel<UiState<List<User>>>() {
    
    fun switchToProduction() {
        // 动态切换 BaseUrl
        interceptorManager.switchBaseUrl("https://api.production.com/")
    }
    
    fun switchToStaging() {
        interceptorManager.switchBaseUrl("https://api.staging.com/")
    }
}
```

### 2. 在 Activity/Fragment 中使用

```kotlin
@AndroidEntryPoint
class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {
    
    @Inject
    lateinit var interceptorManager: NetworkInterceptorManager
    
    override fun setupViews() {
        binding.btnSwitchEnvironment.setOnClickListener {
            // 动态切换环境
            val newBaseUrl = if (isProduction) {
                "https://api.production.com/"
            } else {
                "https://api.staging.com/"
            }
            interceptorManager.switchBaseUrl(newBaseUrl)
            showSuccess("已切换到 ${interceptorManager.getCurrentBaseUrl()}")
        }
    }
}
```

## 🎯 完整示例

### 示例 1：动态切换 BaseUrl

```kotlin
// 1. 在 AppModule 中提供 BaseUrlInterceptor
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideBaseUrlInterceptor(): BaseUrlInterceptor {
        return BaseUrlInterceptor("https://api.example.com/")
    }
    
    @Provides
    @Singleton
    fun provideNetworkClient(
        baseUrlInterceptor: BaseUrlInterceptor,
        moshi: Moshi
    ): NetworkClient {
        return networkClient {
            baseUrlInterceptor(baseUrlInterceptor)
            // ... 其他配置
        }
    }
    
    @Provides
    @Singleton
    fun provideNetworkInterceptorManager(
        baseUrlInterceptor: BaseUrlInterceptor
    ): NetworkInterceptorManager {
        return NetworkInterceptorManager().apply {
            setBaseUrlInterceptor(baseUrlInterceptor)
        }
    }
}
```

```kotlin
// 2. 在 ViewModel 中使用
@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val networkClient: NetworkClient,
    private val interceptorManager: NetworkInterceptorManager
) : BaseViewModel<UiState<List<Article>>>() {
    
    fun loadArticles() {
        executeAsync(
            block = { 
                networkClient.get<List<Article>>("/api/articles")
            }
        )
    }
    
    fun switchEnvironment(env: String) {
        val baseUrl = when (env) {
            "production" -> "https://api.production.com/"
            "staging" -> "https://api.staging.com/"
            else -> "https://api.dev.com/"
        }
        interceptorManager.switchBaseUrl(baseUrl)
    }
}
```

### 示例 2：动态更新 Token

```kotlin
// 1. 实现动态 Token Provider
class AppAuthTokenProvider @Inject constructor(
    private val tokenRepository: TokenRepository
) : AuthTokenProvider {
    
    override fun getToken(): String? {
        return tokenRepository.getToken()  // ✅ 每次请求时获取最新 token
    }
    
    override suspend fun refreshToken(): String? {
        return tokenRepository.refreshToken()
    }
}

// 2. 在 AppModule 中提供
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAuthTokenProvider(
        tokenRepository: TokenRepository
    ): AuthTokenProvider {
        return AppAuthTokenProvider(tokenRepository)
    }
    
    @Provides
    @Singleton
    fun provideNetworkClient(
        authTokenProvider: AuthTokenProvider,
        moshi: Moshi
    ): NetworkClient {
        return networkClient {
            auth(tokenProvider = authTokenProvider)
            // ... 其他配置
        }
    }
    
    @Provides
    @Singleton
    fun provideNetworkInterceptorManager(
        authTokenProvider: AuthTokenProvider
    ): NetworkInterceptorManager {
        return NetworkInterceptorManager().apply {
            setAuthTokenProvider(authTokenProvider)
        }
    }
}
```

```kotlin
// 3. 在 ViewModel 中使用（token 会自动更新，无需手动调用）
@HiltViewModel
class UserViewModel @Inject constructor(
    private val networkClient: NetworkClient,
    private val tokenRepository: TokenRepository
) : BaseViewModel<UiState<User>>() {
    
    fun login(username: String, password: String) {
        executeAsync(
            block = { 
                val result = networkClient.post<LoginResponse, LoginRequest>(
                    url = "/api/login",
                    body = LoginRequest(username, password)
                )
                // 保存 token（会自动更新到 AuthTokenProvider）
                result.onSuccess { response ->
                    tokenRepository.saveToken(response.token)
                }
                result
            }
        )
    }
    
    fun loadUserProfile() {
        // ✅ token 会自动从 tokenRepository 获取最新值
        executeAsync(
            block = { 
                networkClient.get<User>("/api/user/profile")
            }
        )
    }
}
```

## ⚠️ 注意事项

### 1. OkHttpClient 是不可变的

`OkHttpClient` 一旦创建，其拦截器列表就不可修改。但某些拦截器内部支持动态配置：

- ✅ **BaseUrlInterceptor**：通过 `setBaseUrl()` 动态切换
- ✅ **AuthInterceptor**：通过 `AuthTokenProvider.getToken()` 每次获取最新 token
- ❌ **LoggingInterceptor**：配置在创建时确定，不支持动态调整

### 2. 使用 NetworkClientBuilder 的场景

如果需要使用 `BaseUrlInterceptor` 或自定义 `AuthTokenProvider`，建议：

1. **方案 A**：使用 `NetworkClientBuilder` 手动创建（适合特殊配置）
2. **方案 B**：在 `AppModule` 中提供这些拦截器，然后通过 `NetworkInterceptorManager` 管理（推荐）

### 3. 线程安全

- `BaseUrlInterceptor.setBaseUrl()` 是线程安全的（使用 `synchronized`）
- `AuthTokenProvider.getToken()` 应该保证线程安全（建议使用 `@Volatile` 或线程安全的数据结构）

## 📊 总结

| 拦截器 | 支持动态调整 | 调整方式 |
|--------|------------|---------|
| BaseUrlInterceptor | ✅ 是 | `setBaseUrl()` |
| AuthInterceptor | ✅ 是 | 更新 `AuthTokenProvider` 的 token |
| LoggingInterceptor | ❌ 否 | 需要在创建时配置 |
| NetworkStatusInterceptor | ❌ 否 | 需要在创建时配置 |
| DeduplicationInterceptor | ❌ 否 | 需要在创建时配置 |
| RetryInterceptor | ❌ 否 | 需要在创建时配置 |
| ResponseValidationInterceptor | ❌ 否 | 需要在创建时配置 |

## 🎯 最佳实践

1. **BaseUrl 切换**：使用 `NetworkInterceptorManager.switchBaseUrl()`
2. **Token 更新**：实现 `AuthTokenProvider`，每次请求时获取最新 token
3. **日志级别**：根据 `BuildConfig.DEBUG` 在创建时配置
4. **其他配置**：在创建 `NetworkClient` 时一次性配置好







