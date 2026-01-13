package com.jun.core.network.api

import com.jun.core.common.error.AppError
import com.jun.core.common.error.toAppError
import com.jun.core.common.error.toAppResultError
import com.jun.core.common.result.AppResult
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * API 响应封装
 * 用于统一处理 Retrofit 的 Response
 */
sealed class ApiResponse<out T> {
    /**
     * 成功响应
     */
    data class Success<T>(val data: T) : ApiResponse<T>()
    
    /**
     * 错误响应
     */
    data class Error(
        val code: Int,
        val message: String,
        val throwable: Throwable? = null
    ) : ApiResponse<Nothing>()
    
    /**
     * 网络错误
     */
    data class NetworkError(val throwable: Throwable) : ApiResponse<Nothing>()
}

/**
 * 将 Retrofit Response 转换为 ApiResponse
 * 注意：此方法会捕获 JSON 解析异常，但建议使用 toApiResponseEnhanced() 获得更好的错误处理
 */
fun <T> Response<T>.toApiResponse(): ApiResponse<T> {
    return try {
        if (isSuccessful) {
            val body = body()
            if (body != null) {
                ApiResponse.Success(body)
            } else {
                ApiResponse.Error(
                    code = code(),
                    message = "响应体为空"
                )
            }
        } else {
            ApiResponse.Error(
                code = code(),
                message = message() ?: "请求失败"
            )
        }
    } catch (e: com.squareup.moshi.JsonDataException) {
        // JSON 数据类型不匹配
        ApiResponse.Error(
            code = code(),
            message = "数据格式错误: 服务端返回的数据类型与预期不符 - ${e.message}",
            throwable = e
        )
    } catch (e: com.squareup.moshi.JsonEncodingException) {
        // JSON 编码错误
        ApiResponse.Error(
            code = code(),
            message = "JSON 编码错误: ${e.message}",
            throwable = e
        )
    } catch (e: Exception) {
        ApiResponse.NetworkError(e)
    }
}

/**
 * 将 ApiResponse 转换为 AppResult
 */
fun <T> ApiResponse<T>.toAppResult(): AppResult<T> {
    return when (this) {
        is ApiResponse.Success -> AppResult.Success(data)
        is ApiResponse.Error -> {
            val appError = AppError.HttpError(
                message = message,
                code = code,
                cause = throwable
            )
            appError.toAppResultError()
        }
        is ApiResponse.NetworkError -> {
            val appError = throwable.toAppError()
            appError.toAppResultError()
        }
    }
}

/**
 * 安全调用 Retrofit API，自动转换为 AppResult
 * 注意：此方法会捕获 JSON 解析异常，但建议使用 safeApiCallEnhanced() 获得更好的错误处理
 */
suspend fun <T> safeApiCall(
    apiCall: suspend () -> Response<T>
): AppResult<T> {
    return try {
        val response = apiCall()
        response.toApiResponse().toAppResult()
    } catch (e: JsonDataException) {
        // JSON 数据类型不匹配
        val appError = AppError.NetworkError(
            message = "数据格式错误: 服务端返回的数据类型与预期不符",
            cause = e
        )
        appError.toAppResultError()
    } catch (e: JsonEncodingException) {
        // JSON 编码错误
        val appError = AppError.NetworkError(
            message = "JSON 编码错误: ${e.message}",
            cause = e
        )
        appError.toAppResultError()
    } catch (e: Exception) {
        val appError = e.toAppError()
        appError.toAppResultError()
    }
}

/**
 * 将 Retrofit Response 转换为 ApiResponse（增强版本）
 * 提供更详细的错误处理，特别是 JSON 解析错误
 */
fun <T> Response<T>.toApiResponseEnhanced(): ApiResponse<T> {
    return try {
        if (isSuccessful) {
            val body = body()
            if (body != null) {
                ApiResponse.Success(body)
            } else {
                ApiResponse.Error(
                    code = code(),
                    message = "响应体为空",
                    throwable = null
                )
            }
        } else {
            // HTTP 错误响应
            val errorBody = errorBody()?.string() ?: message() ?: "请求失败"
            ApiResponse.Error(
                code = code(),
                message = parseErrorMessage(errorBody),
                throwable = HttpException(this)
            )
        }
    } catch (e: JsonDataException) {
        // JSON 数据类型不匹配
        ApiResponse.Error(
            code = code(),
            message = "数据格式错误: 服务端返回的数据类型与预期不符 - ${e.message}",
            throwable = e
        )
    } catch (e: JsonEncodingException) {
        // JSON 编码错误
        ApiResponse.Error(
            code = code(),
            message = "JSON 编码错误: ${e.message}",
            throwable = e
        )
    } catch (e: IOException) {
        // IO 错误（包括网络错误）
        ApiResponse.NetworkError(e)
    } catch (e: Exception) {
        // 其他未知错误
        ApiResponse.NetworkError(e)
    }
}

/**
 * 安全调用 Retrofit API（增强版本）
 * 提供更详细的错误处理，特别是 JSON 解析错误
 * 推荐使用此方法获得最佳的错误处理体验
 */
suspend fun <T> safeApiCallEnhanced(
    apiCall: suspend () -> Response<T>
): AppResult<T> {
    return try {
        val response = apiCall()
        response.toApiResponseEnhanced().toAppResult()
    } catch (e: JsonDataException) {
        // JSON 数据类型不匹配
        val appError = AppError.NetworkError(
            message = "数据格式错误: 服务端返回的数据类型与预期不符",
            cause = e
        )
        appError.toAppResultError()
    } catch (e: JsonEncodingException) {
        // JSON 编码错误
        val appError = AppError.NetworkError(
            message = "JSON 编码错误: ${e.message}",
            cause = e
        )
        appError.toAppResultError()
    } catch (e: SocketTimeoutException) {
        // 超时错误
        val appError = AppError.NetworkError(
            message = "请求超时，请检查网络连接",
            cause = e
        )
        appError.toAppResultError()
    } catch (e: UnknownHostException) {
        // 网络不可达
        val appError = AppError.NetworkError(
            message = "网络不可达，请检查网络连接",
            cause = e
        )
        appError.toAppResultError()
    } catch (e: HttpException) {
        // HTTP 错误
        val errorBody = e.response()?.errorBody()?.string() ?: e.message()
        val appError = AppError.HttpError(
            message = parseErrorMessage(errorBody),
            code = e.code(),
            cause = e
        )
        appError.toAppResultError()
    } catch (e: IOException) {
        // IO 错误
        val appError = e.toAppError()
        appError.toAppResultError()
    } catch (e: Exception) {
        // 其他未知错误
        val appError = e.toAppError()
        appError.toAppResultError()
    }
}

/**
 * 解析错误消息
 * 尝试从错误响应体中提取错误信息
 */
private fun parseErrorMessage(errorBody: String?): String {
    if (errorBody.isNullOrBlank()) {
        return "请求失败"
    }
    
    return try {
        // 尝试解析 JSON 错误响应
        val json = org.json.JSONObject(errorBody)
        json.optString("message", json.optString("error", errorBody))
    } catch (e: Exception) {
        // 如果不是 JSON，返回原始错误信息
        errorBody
    }
}

