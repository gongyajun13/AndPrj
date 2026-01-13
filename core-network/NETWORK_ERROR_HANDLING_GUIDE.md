# 网络请求数据类型不匹配异常处理指南

## 🎯 问题描述

当服务端返回的数据类型与预期不符时（比如期望返回 `User` 对象，但服务端返回了错误信息或其他格式），可能会导致 JSON 解析失败，从而引发应用崩溃。

## ✅ 解决方案

框架已增强了对这类异常的处理，提供了多种保护机制：

### 1. 增强的错误处理（推荐）

#### 增强的错误处理函数

在 `ApiResponse.kt` 中提供了专门处理 JSON 解析错误的增强函数：

```kotlin
import com.jun.core.network.api.safeApiCallEnhanced
import com.jun.core.network.api.toApiResponseEnhanced

// 方式1：使用增强的安全调用（推荐）
suspend fun getUser(id: String): AppResult<User> {
    return safeApiCallEnhanced {
        userApi.getUser(id)
    }
}

// 方式2：使用增强的响应转换
suspend fun getUser(id: String): AppResult<User> {
    return try {
        val response = userApi.getUser(id)
        response.toApiResponseEnhanced().toAppResult()
    } catch (e: Exception) {
        // 错误已被处理
        AppResult.Error(exception = e)
    }
}
```

**增强功能：**
- ✅ 专门捕获 `JsonDataException`（数据类型不匹配）
- ✅ 专门捕获 `JsonEncodingException`（JSON 编码错误）
- ✅ 提供详细的错误信息
- ✅ 区分不同类型的网络错误（超时、网络不可达等）

### 2. 原有的错误处理（已增强）

原有的 `safeApiCall()` 和 `toApiResponse()` 也已增强，可以捕获 JSON 解析错误：

```kotlin
import com.jun.core.network.api.safeApiCall

// 使用原有的安全调用（已增强）
suspend fun getUser(id: String): AppResult<User> {
    return safeApiCall {
        userApi.getUser(id)
    }
}
```

**增强内容：**
- ✅ 捕获 `JsonDataException` 并转换为明确的错误信息
- ✅ 捕获 `JsonEncodingException` 并转换为明确的错误信息

### 3. 响应验证拦截器（可选）

提供了 `ResponseValidationInterceptor` 来在解析前验证响应格式：

```kotlin
import com.jun.core.network.interceptor.ResponseValidationInterceptor

// 在 NetworkModule 中添加拦截器
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(ResponseValidationInterceptor(enabled = true))
    .build()
```

**功能：**
- ✅ 验证响应体是否为有效的 JSON
- ✅ 在解析前发现格式错误
- ✅ 返回明确的错误响应

## 📋 错误类型说明

### JsonDataException
当服务端返回的数据类型与预期不符时，Moshi 会抛出此异常。

**示例场景：**
```kotlin
// 期望返回 User 对象
data class User(val id: String, val name: String)

// 但服务端返回了错误信息
{
  "error": true,
  "message": "用户不存在"
}

// 此时会抛出 JsonDataException
```

**处理方式：**
```kotlin
// 使用增强的错误处理
val result = safeApiCallEnhanced {
    userApi.getUser(id)
}

when (result) {
    is AppResult.Success -> // 处理成功
    is AppResult.Error -> {
        // 错误信息：数据格式错误: 服务端返回的数据类型与预期不符
        val errorMessage = result.errorMessage
    }
    is AppResult.Loading -> // 处理加载中
}
```

### JsonEncodingException
当 JSON 格式本身有问题时，Moshi 会抛出此异常。

**示例场景：**
```kotlin
// 服务端返回了无效的 JSON
"{ invalid json }"

// 此时会抛出 JsonEncodingException
```

**处理方式：**
```kotlin
// 使用增强的错误处理
val result = safeApiCallEnhanced {
    userApi.getUser(id)
}

when (result) {
    is AppResult.Error -> {
        // 错误信息：JSON 编码错误: ...
    }
}
```

## 🔧 使用建议

### 1. 推荐使用增强的错误处理

```kotlin
class UserRepository : BaseRepository {
    suspend fun getUser(id: String): AppResult<User> {
        // 使用增强的安全调用
        return safeApiCallEnhanced {
            userApi.getUser(id)
        }
    }
}
```

### 2. 在 Repository 中统一处理

```kotlin
class UserRepository : BaseRepository {
    suspend fun getUser(id: String): AppResult<User> {
        return safeApiCallEnhanced {
            userApi.getUser(id)
        }.onError { error ->
            // 统一处理错误
            Timber.e(error.exception, "获取用户失败: ${error.errorMessage}")
        }
    }
}
```

### 3. 在 ViewModel 中处理错误

```kotlin
class UserViewModel : BaseViewModel<UiState<User>>() {
    fun loadUser(id: String) {
        executeAsync(
            block = { repository.getUser(id) },
            onError = { error ->
                // 处理错误，显示友好的错误信息
                showError("获取用户信息失败，请稍后重试")
            }
        )
    }
}
```

## 🛡️ 多层保护

框架提供了多层保护机制：

1. **拦截器层**：`ResponseValidationInterceptor` 在解析前验证响应格式
2. **解析层**：`toApiResponseEnhanced()` 专门处理 JSON 解析错误
3. **调用层**：`safeApiCallEnhanced()` 统一捕获所有异常
4. **结果层**：`AppResult` 统一封装结果和错误

## 📊 错误信息示例

### 数据类型不匹配
```
数据格式错误: 服务端返回的数据类型与预期不符 - Expected BEGIN_OBJECT but was STRING at path $
```

### JSON 编码错误
```
JSON 编码错误: Use JsonReader.setLenient(true) to accept malformed JSON at line 1 column 1 path $
```

### 网络超时
```
请求超时，请检查网络连接
```

### 网络不可达
```
网络不可达，请检查网络连接
```

## ✨ 总结

框架现在可以很好地处理数据类型不匹配的异常：

- ✅ **增强的错误处理**：专门捕获 JSON 解析错误
- ✅ **详细的错误信息**：提供明确的错误原因
- ✅ **多层保护机制**：拦截器、解析器、调用层全方位保护
- ✅ **统一的错误封装**：所有错误都封装在 `AppResult.Error` 中

**建议：使用 `safeApiCallEnhanced()` 来获得最佳的错误处理体验！**

