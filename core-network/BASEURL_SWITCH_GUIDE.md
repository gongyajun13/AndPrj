# BaseUrl 一键切换指南

## 📋 概述

通过 `BaseUrlInterceptor` 和 `NetworkInterceptorManager`，可以实现**一键切换全局 baseUrl**，无需重启应用或重新创建 `NetworkClient` 实例。

## 🎯 实现原理

1. **BaseUrlInterceptor**：拦截器在运行时动态替换请求的 baseUrl
2. **NetworkInterceptorManager**：统一管理拦截器，提供便捷的切换接口
3. **依赖注入**：通过 Hilt 自动注册和管理拦截器

## ✅ 已完成的配置

### 1. NetworkModule 自动配置

`BaseUrlInterceptor` 已在 `NetworkModule` 中自动配置：

```kotlin
@Provides
@Singleton
fun provideBaseUrlInterceptor(
    networkConfig: NetworkConfig
): BaseUrlInterceptor {
    return BaseUrlInterceptor(networkConfig.baseUrl)
}

@Provides
@Singleton
fun provideOkHttpClient(
    networkConfig: NetworkConfig,
    baseUrlInterceptor: BaseUrlInterceptor  // ✅ 自动注入
): OkHttpClient {
    val builder = OkHttpClient.Builder()
    // ...
    // ✅ 自动添加 BaseUrlInterceptor
    builder.addInterceptor(baseUrlInterceptor)
    // ...
}

@Provides
@Singleton
fun provideNetworkInterceptorManager(
    baseUrlInterceptor: BaseUrlInterceptor  // ✅ 自动注册
): NetworkInterceptorManager {
    return NetworkInterceptorManager().apply {
        setBaseUrlInterceptor(baseUrlInterceptor)
    }
}
```

### 2. NetworkInterceptorManager 自动注册

`NetworkInterceptorManager` 会自动注册 `BaseUrlInterceptor`，无需手动配置。

## 🚀 使用方法

### 方法 1：在 Activity/Fragment 中使用（推荐）

```kotlin
@AndroidEntryPoint
class NetworkDemoActivity : BaseActivity<ActivityNetworkDemoBinding>() {
    
    @Inject
    lateinit var interceptorManager: NetworkInterceptorManager
    
    private fun switchToProduction() {
        // ✅ 一键切换 baseUrl
        interceptorManager.switchBaseUrl("https://www.wanandroid.com/")
        showSuccess("已切换到生产环境")
    }
    
    private fun switchToTest() {
        interceptorManager.switchBaseUrl("https://test.wanandroid.com/")
        showSuccess("已切换到测试环境")
    }
    
    private fun showCurrentBaseUrl() {
        val currentBaseUrl = interceptorManager.getCurrentBaseUrl()
        showMessage("当前 BaseUrl: $currentBaseUrl")
    }
}
```

### 方法 2：在 ViewModel 中使用

```kotlin
@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val interceptorManager: NetworkInterceptorManager
) : BaseViewModel<UiState<List<Article>>>() {
    
    fun switchEnvironment(env: String) {
        val baseUrl = when (env) {
            "production" -> "https://www.wanandroid.com/"
            "staging" -> "https://staging.wanandroid.com/"
            "dev" -> "https://dev.wanandroid.com/"
            else -> "https://www.wanandroid.com/"
        }
        interceptorManager.switchBaseUrl(baseUrl)
    }
}
```

### 方法 3：在任意地方使用（通过 Application）

```kotlin
class MyApplication : Application() {
    
    @Inject
    lateinit var interceptorManager: NetworkInterceptorManager
    
    override fun onCreate() {
        super.onCreate()
        
        // 根据配置切换 baseUrl
        if (BuildConfig.DEBUG) {
            interceptorManager.switchBaseUrl("https://dev.wanandroid.com/")
        } else {
            interceptorManager.switchBaseUrl("https://www.wanandroid.com/")
        }
    }
}
```

## 🎨 UI 示例（NetworkDemoActivity）

在 `NetworkDemoActivity` 中已添加了完整的 BaseUrl 切换功能：

### 布局文件

```xml
<!-- BaseUrl 切换 -->
<TextView
    android:text="BaseUrl 切换"
    android:textSize="@dimen/text_size_medium"
    android:textStyle="bold" />

<MaterialButton
    android:id="@+id/btnSwitchBaseUrlProduction"
    android:text="切换到生产环境 (wanandroid.com)" />

<MaterialButton
    android:id="@+id/btnSwitchBaseUrlTest"
    android:text="切换到测试环境 (test.wanandroid.com)" />

<MaterialButton
    android:id="@+id/btnSwitchBaseUrlDev"
    android:text="切换到开发环境 (dev.wanandroid.com)" />

<MaterialButton
    android:id="@+id/btnShowCurrentBaseUrl"
    android:text="查看当前 BaseUrl" />
```

### Activity 代码

```kotlin
@AndroidEntryPoint
class NetworkDemoActivity : BaseActivity<ActivityNetworkDemoBinding>() {
    
    @Inject
    lateinit var interceptorManager: NetworkInterceptorManager
    
    private fun setupButtonListeners() {
        // BaseUrl 切换
        binding.btnSwitchBaseUrlProduction.setOnClickListener {
            switchBaseUrl("https://www.wanandroid.com/", "生产环境")
        }
        
        binding.btnSwitchBaseUrlTest.setOnClickListener {
            switchBaseUrl("https://test.wanandroid.com/", "测试环境")
        }
        
        binding.btnSwitchBaseUrlDev.setOnClickListener {
            switchBaseUrl("https://dev.wanandroid.com/", "开发环境")
        }
        
        binding.btnShowCurrentBaseUrl.setOnClickListener {
            showCurrentBaseUrl()
        }
    }
    
    private fun switchBaseUrl(newBaseUrl: String, envName: String) {
        interceptorManager.switchBaseUrl(newBaseUrl)
        val currentBaseUrl = interceptorManager.getCurrentBaseUrl() ?: "未知"
        showSuccess("已切换到 $envName\n当前 BaseUrl: $currentBaseUrl")
    }
    
    private fun showCurrentBaseUrl() {
        val currentBaseUrl = interceptorManager.getCurrentBaseUrl() ?: "未配置"
        showMessage("当前 BaseUrl: $currentBaseUrl")
    }
}
```

## 🔧 API 说明

### NetworkInterceptorManager 方法

| 方法 | 说明 | 示例 |
|------|------|------|
| `switchBaseUrl(newBaseUrl: String)` | 切换 baseUrl | `switchBaseUrl("https://api.example.com/")` |
| `getCurrentBaseUrl(): String?` | 获取当前 baseUrl | `val url = getCurrentBaseUrl()` |
| `hasBaseUrlInterceptor(): Boolean` | 检查是否已配置拦截器 | `if (hasBaseUrlInterceptor()) { ... }` |
| `getConfigSummary(): String` | 获取配置摘要（调试用） | `Timber.d(getConfigSummary())` |

## ⚠️ 注意事项

### 1. BaseUrl 格式

确保 baseUrl 以 `/` 结尾：

```kotlin
// ✅ 正确
interceptorManager.switchBaseUrl("https://www.wanandroid.com/")

// ❌ 错误（缺少尾部斜杠）
interceptorManager.switchBaseUrl("https://www.wanandroid.com")
```

### 2. 线程安全

`BaseUrlInterceptor.setBaseUrl()` 是线程安全的（使用 `synchronized`），可以在任意线程调用。

### 3. 立即生效

切换 baseUrl 后，**后续的所有网络请求**都会使用新的 baseUrl，无需重启应用。

### 4. 不影响已发起的请求

切换 baseUrl 不会影响**正在进行的请求**，只影响**新的请求**。

## 📊 工作流程

```
1. 应用启动
   ↓
2. NetworkModule 创建 BaseUrlInterceptor（使用 NetworkConfig.baseUrl）
   ↓
3. BaseUrlInterceptor 添加到 OkHttpClient
   ↓
4. NetworkInterceptorManager 注册 BaseUrlInterceptor
   ↓
5. 用户调用 interceptorManager.switchBaseUrl("新URL")
   ↓
6. BaseUrlInterceptor 内部更新 baseUrl（线程安全）
   ↓
7. 后续所有请求使用新的 baseUrl
```

## 🎯 使用场景

1. **环境切换**：开发、测试、生产环境切换
2. **A/B 测试**：切换不同的 API 服务器
3. **故障转移**：主服务器故障时切换到备用服务器
4. **调试测试**：临时切换到测试服务器进行调试

## 📝 完整示例

```kotlin
@AndroidEntryPoint
class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {
    
    @Inject
    lateinit var interceptorManager: NetworkInterceptorManager
    
    override fun setupViews() {
        // 显示当前环境
        val currentBaseUrl = interceptorManager.getCurrentBaseUrl()
        binding.tvCurrentEnvironment.text = "当前环境: $currentBaseUrl"
        
        // 环境切换按钮
        binding.btnSwitchProduction.setOnClickListener {
            switchEnvironment("production", "https://www.wanandroid.com/")
        }
        
        binding.btnSwitchStaging.setOnClickListener {
            switchEnvironment("staging", "https://staging.wanandroid.com/")
        }
        
        binding.btnSwitchDev.setOnClickListener {
            switchEnvironment("dev", "https://dev.wanandroid.com/")
        }
    }
    
    private fun switchEnvironment(envName: String, baseUrl: String) {
        interceptorManager.switchBaseUrl(baseUrl)
        showSuccess("已切换到 $envName 环境")
        
        // 更新 UI
        binding.tvCurrentEnvironment.text = "当前环境: $baseUrl"
        
        // 可选：保存到 SharedPreferences
        saveEnvironmentToPrefs(envName, baseUrl)
    }
}
```

## ✨ 总结

通过 `BaseUrlInterceptor` 和 `NetworkInterceptorManager`，实现了一键切换全局 baseUrl 的功能：

- ✅ **自动配置**：通过 Hilt 依赖注入自动配置
- ✅ **线程安全**：使用 `synchronized` 保证线程安全
- ✅ **立即生效**：切换后立即影响后续请求
- ✅ **易于使用**：简单的 API，一行代码完成切换
- ✅ **统一管理**：通过 `NetworkInterceptorManager` 统一管理

现在可以在应用的任意位置通过 `interceptorManager.switchBaseUrl()` 一键切换 baseUrl！







