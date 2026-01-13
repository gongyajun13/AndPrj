# Android 框架使用指南

## 📋 项目结构

本项目已重构为一个功能齐全、使用方便的框架类项目，可以作为其他矩阵项目的开发地基。

### 模块架构

```
AndPrj/
├── core-common/      # 核心通用模块
│   ├── result/       # AppResult 统一结果封装
│   ├── error/        # 错误处理
│   ├── extension/    # 扩展函数
│   └── config/       # 配置接口
│
├── core-network/     # 网络层模块
│   ├── api/          # API 响应封装
│   ├── config/       # 网络配置接口
│   └── di/           # 网络层依赖注入
│
├── core-database/    # 数据库层模块
│   ├── dao/          # BaseDao 基类
│   ├── config/       # 数据库配置接口
│   └── di/           # 数据库层依赖注入
│
├── core-domain/      # 领域层模块
│   ├── repository/   # BaseRepository 基类
│   └── usecase/      # BaseUseCase 基类
│
├── core-ui/          # UI 层模块
│   ├── state/        # UiState 状态管理
│   ├── viewmodel/    # BaseViewModel 基类
│   └── event/        # SingleLiveEvent 单次事件
│
└── app/              # 应用模块（业务代码）
    ├── config/       # 配置实现
    ├── data/         # 数据层
    ├── domain/       # 领域层
    ├── ui/           # UI 层
    └── di/           # 依赖注入
```

## 🚀 核心功能

### 1. AppResult - 统一结果封装

`AppResult<T>` 提供了统一的结果封装，替代标准库的 `Result<T>`，提供更丰富的功能：

```kotlin
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(...) : AppResult<Nothing>()
    object Loading : AppResult<Nothing>()
}
```

**使用示例：**

```kotlin
val result: AppResult<List<User>> = repository.getUsers()

result
    .onSuccess { users -> 
        // 处理成功
    }
    .onError { error -> 
        // 处理错误
    }
    .map { users -> users.size } // 映射数据
```

### 2. BaseRepository - Repository 基类

`BaseRepository` 提供了通用的错误处理和线程调度：

```kotlin
class UserRepositoryImpl : UserRepository, BaseRepository {
    
    override suspend fun getUsers(): AppResult<List<User>> {
        return executeNetworkCall {
            // 网络请求逻辑
        }
    }
    
    override suspend fun saveUsers(users: List<User>): AppResult<Unit> {
        return executeDatabaseCall {
            // 数据库操作逻辑
        }
    }
}
```

**提供的方法：**
- `executeNetworkCall()` - 执行网络请求，自动处理错误和线程切换
- `executeDatabaseCall()` - 执行数据库操作，自动处理错误和线程切换
- `executeCall()` - 执行通用操作，自动处理错误

### 3. BaseUseCase - UseCase 基类

`BaseUseCase` 提供了统一的 UseCase 执行逻辑：

```kotlin
// 无参数 UseCase
class GetUsersUseCase @Inject constructor(
    private val repository: UserRepository
) : BaseUseCaseNoParamsImpl<List<User>>() {
    
    override suspend fun execute(): List<User> {
        return repository.getUsers().getOrThrow()
    }
}

// 有参数 UseCase
class GetUserByIdUseCase @Inject constructor(
    private val repository: UserRepository
) : BaseUseCaseImpl<String, User>() {
    
    override suspend fun execute(params: String): User {
        return repository.getUserById(params).getOrThrow()
    }
}
```

### 4. BaseViewModel - ViewModel 基类

`BaseViewModel` 提供了统一的状态管理和错误处理：

```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUsersUseCase: GetUsersUseCase
) : BaseViewModel<UiState<List<User>>>() {
    
    init {
        loadUsers()
    }
    
    fun loadUsers() {
        executeAsync(
            block = { getUsersUseCase() },
            onSuccess = { users ->
                // 成功后的处理
            }
        )
    }
    
    override fun createInitialState(): UiState<List<User>> {
        return UiState.Initial
    }
}
```

**提供的功能：**
- 自动状态管理（Loading、Success、Error、Empty）
- 统一的错误处理
- 简化的异步操作执行

### 5. UiState - UI 状态管理

`UiState` 提供了统一的 UI 状态封装：

```kotlin
sealed class UiState<out T> {
    object Initial : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}
```

**在 Activity/Fragment 中使用：**

```kotlin
viewModel.uiState.collect { state ->
    when (state) {
        is UiState.Loading -> showLoading()
        is UiState.Success -> showData(state.data)
        is UiState.Error -> showError(state.message)
        is UiState.Empty -> showEmpty()
        is UiState.Initial -> {}
    }
}
```

## 🔧 配置

### 网络配置

在 `app` 模块中实现 `NetworkConfig` 接口：

```kotlin
@Singleton
class NetworkConfigImpl @Inject constructor() : NetworkConfig {
    override val baseUrl: String
        get() = "https://api.example.com/"
    
    override val enableLogging: Boolean
        get() = BuildConfig.DEBUG
}
```

### 数据库配置

在 `app` 模块中实现 `DatabaseConfig` 接口：

```kotlin
@Singleton
class DatabaseConfigImpl @Inject constructor() : DatabaseConfig {
    override val databaseName: String
        get() = "app_database"
    
    override val databaseVersion: Int
        get() = 1
}
```

### 应用配置

在 `app` 模块中实现 `AppConfig` 接口：

```kotlin
class AppConfigImpl : AppConfig {
    override val isDebug: Boolean
        get() = BuildConfig.DEBUG
}
```

## 📦 依赖注入

所有配置都需要通过 Hilt 绑定：

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    
    @Binds
    @Singleton
    abstract fun bindNetworkConfig(networkConfigImpl: NetworkConfigImpl): NetworkConfig
    
    @Binds
    @Singleton
    abstract fun bindDatabaseConfig(databaseConfigImpl: DatabaseConfigImpl): DatabaseConfig
}
```

## 🎯 使用流程

### 1. 创建 Repository

```kotlin
interface UserRepository {
    suspend fun getUsers(): AppResult<List<User>>
}

class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao
) : UserRepository, BaseRepository {
    
    override suspend fun getUsers(): AppResult<List<User>> {
        return executeNetworkCall {
            val response = userApi.getUsers()
            // 处理响应...
        }
    }
}
```

### 2. 创建 UseCase

```kotlin
class GetUsersUseCase @Inject constructor(
    private val repository: UserRepository
) : BaseUseCaseNoParamsImpl<List<User>>() {
    
    override suspend fun execute(): List<User> {
        return repository.getUsers().getOrThrow()
    }
}
```

### 3. 创建 ViewModel

```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUsersUseCase: GetUsersUseCase
) : BaseViewModel<UiState<List<User>>>() {
    
    fun loadUsers() {
        executeAsync(
            block = { getUsersUseCase() }
        )
    }
    
    override fun createInitialState(): UiState<List<User>> {
        return UiState.Initial
    }
}
```

### 4. 在 UI 中使用

```kotlin
lifecycleScope.launch {
    viewModel.uiState.collect { state ->
        when (state) {
            is UiState.Loading -> progressBar.show()
            is UiState.Success -> {
                progressBar.hide()
                adapter.submitList(state.data)
            }
            is UiState.Error -> {
                progressBar.hide()
                showError(state.message)
            }
            else -> {}
        }
    }
}
```

## 🔄 迁移到新项目

1. **复制 core 模块**：将 `core-*` 模块复制到新项目
2. **实现配置接口**：在新项目中实现 `NetworkConfig`、`DatabaseConfig`、`AppConfig`
3. **配置依赖注入**：在 DI 模块中绑定配置实现
4. **开始开发**：按照上述流程创建 Repository、UseCase、ViewModel

## 📝 注意事项

1. **Gradle 版本**：确保使用 Gradle 8.13 或更高版本
2. **依赖管理**：所有依赖版本统一在 `gradle/libs.versions.toml` 中管理
3. **错误处理**：框架已提供统一的错误处理，无需在每个地方重复处理
4. **线程切换**：框架已自动处理线程切换，Repository 和 UseCase 中无需手动切换

## 🎨 扩展功能

框架设计为可扩展的，你可以：

1. **扩展 BaseRepository**：添加更多通用方法
2. **扩展 BaseViewModel**：添加更多通用 UI 逻辑
3. **自定义错误处理**：实现自定义的错误处理策略
4. **添加新模块**：根据需要添加新的 core 模块

## 📚 相关文档

- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Room](https://developer.android.com/training/data-storage/room)
- [Retrofit](https://square.github.io/retrofit/)

