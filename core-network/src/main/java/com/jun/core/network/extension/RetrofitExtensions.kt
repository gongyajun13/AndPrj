package com.jun.core.network.extension

import com.jun.core.common.result.AppResult
import com.jun.core.network.api.ApiResponse
import com.jun.core.network.api.safeApiCall
import com.jun.core.network.api.toApiResponse
import retrofit2.Call
import retrofit2.Response

/**
 * Retrofit 扩展函数集合
 */

/**
 * 执行 Retrofit Call，返回 AppResult
 */
suspend fun <T> Call<T>.executeAsResult(): AppResult<T> {
    return safeApiCall {
        execute()
    }
}

/**
 * 执行 Retrofit Call，返回 ApiResponse
 */
suspend fun <T> Call<T>.executeAsApiResponse(): ApiResponse<T> {
    return try {
        val response = execute()
        response.toApiResponse()
    } catch (e: Exception) {
        ApiResponse.NetworkError(e)
    }
}

/**
 * 检查 Response 是否成功
 */
fun <T> Response<T>.isSuccess(): Boolean {
    return isSuccessful && body() != null
}

/**
 * 获取响应体（安全）
 */
fun <T> Response<T>.getBodyOrNull(): T? {
    return if (isSuccess()) body() else null
}

/**
 * 获取错误消息
 */
fun <T> Response<T>.getErrorMessage(): String {
    return message() ?: "请求失败 (${code()})"
}


