package com.jun.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException

/**
 * 响应验证拦截器
 * 用于验证响应体格式，防止数据类型不匹配导致的解析错误
 */
class ResponseValidationInterceptor(
    private val enabled: Boolean = true
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        
        if (!enabled) {
            return response
        }
        
        if (!response.isSuccessful) {
            return response
        }
        
        val responseBody = response.body ?: return response
        
        // 读取响应体内容（但不消费它）
        val source = responseBody.source()
        source.request(Long.MAX_VALUE)
        val buffer = source.buffer.clone()
        val responseBodyString = buffer.readUtf8()
        
        // 检查响应体是否为有效的 JSON
        if (isJsonResponse(response)) {
            try {
                // 尝试解析 JSON，检查格式是否正确
                JSONObject(responseBodyString)
            } catch (e: Exception) {
                // JSON 格式错误，返回错误响应
                Timber.e(e, "响应体 JSON 格式错误")
                return createErrorResponse(
                    response,
                    "响应数据格式错误: ${e.message}"
                )
            }
        }
        
        // 重新创建响应体（因为已经被读取了）
        val mediaType = responseBody.contentType()
        val newResponseBody = responseBodyString.toResponseBody(mediaType)
        
        return response.newBuilder()
            .body(newResponseBody)
            .build()
    }
    
    /**
     * 检查响应是否为 JSON 格式
     */
    private fun isJsonResponse(response: Response): Boolean {
        val contentType = response.header("Content-Type") ?: return false
        return contentType.contains("application/json", ignoreCase = true) ||
               contentType.contains("text/json", ignoreCase = true)
    }
    
    /**
     * 创建错误响应
     */
    private fun createErrorResponse(
        originalResponse: Response,
        errorMessage: String
    ): Response {
        val errorJson = JSONObject().apply {
            put("error", true)
            put("message", errorMessage)
            put("code", 500)
        }
        
        val mediaType = "application/json".toMediaTypeOrNull()
        val errorBody = errorJson.toString().toResponseBody(mediaType)
        
        return originalResponse.newBuilder()
            .code(500)
            .message("数据解析错误")
            .body(errorBody)
            .build()
    }
}

