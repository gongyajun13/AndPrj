package com.jun.core.network.config

import com.jun.core.network.interceptor.AuthTokenProvider
import com.jun.core.network.interceptor.BaseUrlInterceptor
import com.jun.core.network.interceptor.LoggingInterceptor

/**
 * 网络拦截器管理器
 * 用于动态调整和管理网络请求拦截器的配置
 * 
 * 设计说明：
 * 1. 虽然 OkHttpClient 是不可变的，但某些拦截器内部支持动态配置
 * 2. 通过此管理器可以访问和修改这些可动态调整的拦截器
 * 3. 适用于需要运行时调整拦截器配置的场景（如切换环境、更新 token 等）
 * 4. 支持自动注册拦截器，简化使用
 */
class NetworkInterceptorManager {
    
    /**
     * BaseUrl 拦截器（支持动态切换 BaseUrl）
     */
    var baseUrlInterceptor: BaseUrlInterceptor? = null
        private set
    
    /**
     * 认证 Token Provider（支持动态更新 token）
     */
    var authTokenProvider: AuthTokenProvider? = null
        private set
    
    /**
     * 日志拦截器（注意：LoggingInterceptor 本身不支持动态调整，但可以通过替换实例来实现）
     */
    private var _loggingInterceptor: LoggingInterceptor? = null
    
    /**
     * 设置 BaseUrl 拦截器（自动注册）
     */
    fun setBaseUrlInterceptor(interceptor: BaseUrlInterceptor) {
        this.baseUrlInterceptor = interceptor
    }
    
    /**
     * 注册 BaseUrl 拦截器（便捷方法，返回拦截器本身以支持链式调用）
     */
    fun registerBaseUrlInterceptor(interceptor: BaseUrlInterceptor): BaseUrlInterceptor {
        this.baseUrlInterceptor = interceptor
        return interceptor
    }
    
    /**
     * 动态切换 BaseUrl
     */
    fun switchBaseUrl(newBaseUrl: String) {
        require(baseUrlInterceptor != null) {
            "BaseUrlInterceptor 未注册，请先调用 setBaseUrlInterceptor() 或 registerBaseUrlInterceptor()"
        }
        baseUrlInterceptor?.setBaseUrl(newBaseUrl)
    }
    
    /**
     * 获取当前 BaseUrl
     */
    fun getCurrentBaseUrl(): String? {
        return baseUrlInterceptor?.getBaseUrl()
    }
    
    /**
     * 设置认证 Token Provider（自动注册）
     */
    fun setAuthTokenProvider(provider: AuthTokenProvider) {
        this.authTokenProvider = provider
    }
    
    /**
     * 注册认证 Token Provider（便捷方法，返回 provider 本身以支持链式调用）
     */
    fun registerAuthTokenProvider(provider: AuthTokenProvider): AuthTokenProvider {
        this.authTokenProvider = provider
        return provider
    }
    
    /**
     * 检查是否已配置 BaseUrl 拦截器
     */
    fun hasBaseUrlInterceptor(): Boolean {
        return baseUrlInterceptor != null
    }
    
    /**
     * 检查是否已配置认证拦截器
     */
    fun hasAuthInterceptor(): Boolean {
        return authTokenProvider != null
    }
    
    /**
     * 获取拦截器配置摘要（用于调试）
     */
    fun getConfigSummary(): String {
        return buildString {
            appendLine("NetworkInterceptorManager 配置:")
            appendLine("  - BaseUrlInterceptor: ${if (hasBaseUrlInterceptor()) "已配置 (${getCurrentBaseUrl()})" else "未配置"}")
            appendLine("  - AuthInterceptor: ${if (hasAuthInterceptor()) "已配置" else "未配置"}")
        }
    }
}

