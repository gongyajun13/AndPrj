package com.jun.core.network.interceptor

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * BaseUrl 拦截器
 * 用于动态切换 API 的 BaseUrl
 */
class BaseUrlInterceptor(
    private var baseUrl: String
) : Interceptor {
    
    private val lock = Any()
    
    /**
     * 设置新的 BaseUrl
     */
    fun setBaseUrl(newBaseUrl: String) {
        synchronized(lock) {
            baseUrl = newBaseUrl
        }
    }
    
    /**
     * 获取当前 BaseUrl
     */
    fun getBaseUrl(): String {
        synchronized(lock) {
            return baseUrl
        }
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalHttpUrl = originalRequest.url
        
        // 如果 URL 已经是完整的 URL（包含 scheme），则不修改
        // 这样可以支持完整的下载链接，不会被 baseUrl 替换
        if (originalHttpUrl.scheme != null && 
            (originalHttpUrl.scheme == "http" || originalHttpUrl.scheme == "https")) {
            // URL 已经是完整的，直接返回，不进行 baseUrl 替换
            return chain.proceed(originalRequest)
        }
        
        val newBaseUrl = synchronized(lock) { baseUrl }
        
        // 解析新的 BaseUrl
        val newHttpUrl = try {
            // 使用 HttpUrl.Builder 解析新的 BaseUrl
            val baseUrlBuilder = HttpUrl.Builder()
            val baseUrlParsed = baseUrlBuilder
                .scheme(if (newBaseUrl.startsWith("https")) "https" else "http")
                .host(extractHost(newBaseUrl))
                .port(extractPort(newBaseUrl))
                .build()
            
            // 使用新的 BaseUrl 替换原始 URL 的 scheme、host 和 port
            originalHttpUrl.newBuilder()
                .scheme(baseUrlParsed.scheme)
                .host(baseUrlParsed.host)
                .port(baseUrlParsed.port)
                .build()
        } catch (e: Exception) {
            // 如果解析失败，返回原始 URL
            originalHttpUrl
        }
        
        val newRequest = originalRequest.newBuilder()
            .url(newHttpUrl)
            .build()
        
        return chain.proceed(newRequest)
    }
    
    /**
     * 从 URL 字符串中提取主机名
     */
    private fun extractHost(url: String): String {
        return try {
            val urlWithoutProtocol = url.removePrefix("http://").removePrefix("https://")
            val host = urlWithoutProtocol.split("/").first().split(":").first()
            host
        } catch (e: Exception) {
            "localhost"
        }
    }
    
    /**
     * 从 URL 字符串中提取端口号
     */
    private fun extractPort(url: String): Int {
        return try {
            val urlWithoutProtocol = url.removePrefix("http://").removePrefix("https://")
            val hostAndPort = urlWithoutProtocol.split("/").first()
            if (hostAndPort.contains(":")) {
                val port = hostAndPort.split(":").last().toInt()
                port
            } else {
                if (url.startsWith("https")) 443 else 80
            }
        } catch (e: Exception) {
            if (url.startsWith("https")) 443 else 80
        }
    }
}

