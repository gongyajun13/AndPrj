# 三个模块扩展功能使用指南

## 📦 扩展概览

已为 core-network、core-database、core-domain 三个模块添加了扩展功能。

---

## 🌐 Core-Network 模块扩展

### 新增文件（3 个）

#### 1. LoggingInterceptor.kt - 日志拦截器

用于记录网络请求和响应的详细信息。

```kotlin
// 创建日志拦截器
val loggingInterceptor = LoggingInterceptor(
    enabled = true,
    logLevel = LoggingInterceptor.LogLevel.BODY // NONE, BASIC, HEADERS, BODY
)

// 添加到 OkHttpClient
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .build()
```

**日志级别：**
- `NONE` - 不记录日志
- `BASIC` - 只记录请求方法和URL
- `HEADERS` - 记录请求方法和URL以及请求头
- `BODY` - 记录请求方法和URL、请求头以及请求体和响应体

#### 2. RetryInterceptor.kt - 重试拦截器

在网络请求失败时自动重试。

```kotlin
// 创建重试拦截器
val retryInterceptor = RetryInterceptor(
    maxRetries = 3,                    // 最大重试次数
    retryDelayMillis = 1000,           // 重试延迟（毫秒）
    retryableExceptions = listOf(      // 可重试的异常类型
        IOException::class.java,
        SocketTimeoutException::class.java
    )
)

// 添加到 OkHttpClient
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(retryInterceptor)
    .build()
```

#### 3. RetrofitExtensions.kt - Retrofit 扩展函数

提供 Retrofit 相关的扩展函数。

```kotlin
// 执行 Call，返回 AppResult
val result: AppResult<User> = userApi.getUser(id).executeAsResult()

// 执行 Call，返回 ApiResponse
val response: ApiResponse<User> = userApi.getUser(id).executeAsApiResponse()

// 检查 Response 是否成功
val isSuccess = response.isSuccess()

// 安全获取响应体
val body = response.getBodyOrNull()

// 获取错误消息
val errorMessage = response.getErrorMessage()
```

### 原有文件（5 个）
1. ✅ `api/ApiResponse.kt` - API 响应封装
2. ✅ `config/NetworkConfig.kt` - 网络配置接口
3. ✅ `di/NetworkModule.kt` - 网络层依赖注入
4. ✅ `interceptor/AuthInterceptor.kt` - 认证拦截器
5. ✅ `interceptor/BaseUrlInterceptor.kt` - BaseUrl 拦截器

### 总计
- **8 个文件**（从 5 个增加到 8 个）

---

## 💾 Core-Database 模块扩展

### 新增文件（2 个）

#### 1. DaoExtensions.kt - DAO 扩展函数

提供 BaseDao 的安全操作方法，返回 AppResult。

```kotlin
// 安全插入实体
val result: AppResult<Long> = userDao.insertSafe(user)

// 安全批量插入
val result: AppResult<List<Long>> = userDao.insertAllSafe(users)

// 安全更新实体
val result: AppResult<Int> = userDao.updateSafe(user)

// 安全批量更新
val result: AppResult<Int> = userDao.updateAllSafe(users)

// 安全删除实体
val result: AppResult<Int> = userDao.deleteSafe(user)

// 安全批量删除
val result: AppResult<Int> = userDao.deleteAllSafe(users)

// 插入或更新（如果存在则更新，不存在则插入）
val result: AppResult<Long> = userDao.insertOrUpdate(user)
```

#### 2. DatabaseUtils.kt - 数据库工具类

提供数据库相关的工具方法。

```kotlin
// 创建简单的数据库迁移
val migration = DatabaseUtils.createSimpleMigration(
    startVersion = 1,
    endVersion = 2,
    "ALTER TABLE users ADD COLUMN age INTEGER NOT NULL DEFAULT 0"
)

// 清空所有表（谨慎使用）
DatabaseUtils.clearAllTables(database)

// 检查数据库是否已打开
val isOpen = DatabaseUtils.isOpen(database)

// 获取数据库版本
val version = DatabaseUtils.getVersion(database)
```

### 原有文件（3 个）
1. ✅ `config/DatabaseConfig.kt` - 数据库配置接口
2. ✅ `dao/BaseDao.kt` - BaseDao 接口
3. ✅ `di/DatabaseModule.kt` - 数据库层依赖注入

### 总计
- **5 个文件**（从 3 个增加到 5 个）

---

## 🏗️ Core-Domain 模块扩展

### 新增文件（3 个）

#### 1. FlowUseCase.kt - Flow 类型的 UseCase

用于返回 Flow 数据流的 UseCase。

```kotlin
// 有参数的 Flow UseCase
class GetUserFlowUseCase : FlowUseCaseImpl<String, User>() {
    override suspend fun execute(params: String): User {
        return repository.getUser(params)
    }
}

// 使用
val flow: Flow<AppResult<User>> = getUserFlowUseCase("user_id")
flow.collect { result ->
    when (result) {
        is AppResult.Success -> // 处理成功
        is AppResult.Error -> // 处理错误
        is AppResult.Loading -> // 处理加载中
    }
}

// 无参数的 Flow UseCase
class GetUsersFlowUseCase : FlowUseCaseNoParamsImpl<List<User>>() {
    override suspend fun execute(): List<User> {
        return repository.getUsers()
    }
}

// 使用
val flow: Flow<AppResult<List<User>>> = getUsersFlowUseCase()
```

#### 2. RepositoryExtensions.kt - Repository 扩展函数

提供 Repository 的 Flow 操作方法。

```kotlin
class UserRepository : BaseRepository {
    // 执行网络请求并返回 Flow
    suspend fun getUserAsFlow(id: String): Flow<AppResult<User>> {
        return executeNetworkCallAsFlow {
            api.getUser(id)
        }
    }
    
    // 执行数据库操作并返回 Flow
    suspend fun getUsersAsFlow(): Flow<AppResult<List<User>>> {
        return executeDatabaseCallAsFlow {
            dao.getAllUsers()
        }
    }
    
    // 执行通用操作并返回 Flow
    suspend fun processDataAsFlow(): Flow<AppResult<Data>> {
        return executeCallAsFlow {
            // 处理逻辑
        }
    }
}
```

#### 3. DomainUtils.kt - 领域层工具类

提供领域层相关的工具方法。

```kotlin
// 合并多个 AppResult
val result1: AppResult<User> = getUser()
val result2: AppResult<Profile> = getProfile()
val combined: AppResult<Pair<User, Profile>> = DomainUtils.combineResults(result1, result2)

// 合并多个 AppResult（列表版本）
val results = listOf(result1, result2, result3)
val combined: AppResult<List<Any>> = DomainUtils.combineResults(*results.toTypedArray())

// 检查 AppResult 状态
val isSuccess = result.isSuccess()
val isError = result.isError()
val isLoading = result.isLoading()

// 获取数据或错误消息
val data = result.getDataOrNull()
val errorMessage = result.getErrorMessageOrNull()
```

### 原有文件（2 个）
1. ✅ `repository/BaseRepository.kt` - BaseRepository 接口
2. ✅ `usecase/BaseUseCase.kt` - BaseUseCase 基类

### 总计
- **5 个文件**（从 2 个增加到 5 个）

---

## 📊 扩展统计

### Core-Network
- **新增**：3 个文件
- **总计**：8 个文件
- **功能**：日志拦截器、重试拦截器、Retrofit 扩展

### Core-Database
- **新增**：2 个文件
- **总计**：5 个文件
- **功能**：DAO 安全操作扩展、数据库工具类

### Core-Domain
- **新增**：3 个文件
- **总计**：5 个文件
- **功能**：Flow UseCase、Repository Flow 扩展、领域工具类

### 总计
- **新增文件**：8 个
- **总文件数**：18 个（从 10 个增加到 18 个）

---

## 🎯 使用建议

### Core-Network
1. **日志拦截器**：在开发环境使用 `LogLevel.BODY`，生产环境使用 `LogLevel.BASIC` 或 `NONE`
2. **重试拦截器**：根据网络环境调整重试次数和延迟时间
3. **Retrofit 扩展**：使用 `executeAsResult()` 简化错误处理

### Core-Database
1. **DAO 扩展**：使用 `*Safe()` 方法获得统一的错误处理
2. **数据库工具**：使用 `createSimpleMigration()` 简化数据库迁移

### Core-Domain
1. **Flow UseCase**：用于需要实时数据更新的场景
2. **Repository Flow**：用于将数据操作转换为 Flow
3. **领域工具**：使用 `combineResults()` 合并多个操作结果

---

## ✨ 总结

三个模块已成功扩展：

- ✅ **Core-Network**：添加了日志和重试拦截器，以及 Retrofit 扩展函数
- ✅ **Core-Database**：添加了 DAO 安全操作扩展和数据库工具类
- ✅ **Core-Domain**：添加了 Flow UseCase、Repository Flow 扩展和领域工具类

**所有模块扩展完成！** 🎉


