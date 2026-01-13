package com.jun.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 认证拦截器接口
 * 子项目可以实现此接口来提供认证 token
 */
interface AuthTokenProvider {
    /**
     * 获取认证 token
     */
    fun getToken(): String?
    
    /**
     * 刷新 token（可选）
     */
    suspend fun refreshToken(): String? = null
}

/**
 * 认证拦截器
 * 自动在请求头中添加认证 token
 */
class AuthInterceptor(
    private val tokenProvider: AuthTokenProvider,
    private val headerName: String = "Authorization",
    private val tokenPrefix: String = "Bearer "
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val token = tokenProvider.getToken()
        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header(headerName, "$tokenPrefix$token")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(newRequest)
    }
}

