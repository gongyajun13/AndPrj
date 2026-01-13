package com.jun.core.network.client

import android.content.Context
import com.jun.core.common.network.NetworkMonitor
import com.jun.core.network.interceptor.*
import com.jun.core.network.util.OkHttpCacheHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * NetworkClient 构建器
 * 支持配置各种拦截器，自动构建 OkHttpClient 和 Retrofit
 */
class NetworkClientBuilder {
    private var baseUrl: String = "https://api.example.com/"
    private var connectTimeoutSeconds: Long = 30
    private var readTimeoutSeconds: Long = 30
    private var writeTimeoutSeconds: Long = 30
    
    // 拦截器配置
    private var authTokenProvider: AuthTokenProvider? = null
    private var authHeaderName: String = "Authorization"
    private var authTokenPrefix: String = "Bearer "
    
    private var baseUrlInterceptor: BaseUrlInterceptor? = null
    
    private var networkMonitor: NetworkMonitor? = null
    private var enableNetworkStatusCheck: Boolean = false
    
    private var enableRequestDeduplication: Boolean = true
    private var deduplicationWindowMillis: Long = 1000
    
    private var enableLogging: Boolean = true
    private var logLevel: LoggingInterceptor.LogLevel = LoggingInterceptor.LogLevel.BODY
    private var formatJson: Boolean = true
    private var maxResponseBodyLength: Int = 2000
    
    private var enableCache: Boolean = false
    private var cache: Cache? = null
    private var cacheMaxAgeSeconds: Int = 60
    private var cacheMaxStaleSeconds: Int = 7 * 24 * 60 * 60 // 7天
    
    private var enableRetry: Boolean = false
    private var maxRetries: Int = 3
    private var retryDelayMillis: Long = 1000
    
    private var enableResponseValidation: Boolean = false
    
    private var moshi: Moshi? = null
    
    // 自定义拦截器列表
    private val customInterceptors: MutableList<okhttp3.Interceptor> = mutableListOf()
    private val customNetworkInterceptors: MutableList<okhttp3.Interceptor> = mutableListOf()
    
    /**
     * 设置基础 URL
     */
    fun baseUrl(url: String): NetworkClientBuilder {
        this.baseUrl = url
        return this
    }
    
    /**
     * 设置超时时间
     */
    fun timeouts(
        connectSeconds: Long = 30,
        readSeconds: Long = 30,
        writeSeconds: Long = 30
    ): NetworkClientBuilder {
        this.connectTimeoutSeconds = connectSeconds
        this.readTimeoutSeconds = readSeconds
        this.writeTimeoutSeconds = writeSeconds
        return this
    }
    
    /**
     * 配置认证拦截器
     */
    fun auth(
        tokenProvider: AuthTokenProvider,
        headerName: String = "Authorization",
        tokenPrefix: String = "Bearer "
    ): NetworkClientBuilder {
        this.authTokenProvider = tokenProvider
        this.authHeaderName = headerName
        this.authTokenPrefix = tokenPrefix
        return this
    }
    
    /**
     * 配置 BaseUrl 拦截器（用于动态切换 BaseUrl）
     */
    fun baseUrlInterceptor(interceptor: BaseUrlInterceptor): NetworkClientBuilder {
        this.baseUrlInterceptor = interceptor
        return this
    }
    
    /**
     * 配置网络状态拦截器
     */
    fun networkStatus(
        networkMonitor: NetworkMonitor,
        enabled: Boolean = true
    ): NetworkClientBuilder {
        this.networkMonitor = networkMonitor
        this.enableNetworkStatusCheck = enabled
        return this
    }
    
    /**
     * 配置请求去重拦截器
     */
    fun deduplication(
        enabled: Boolean = true,
        windowMillis: Long = 1000
    ): NetworkClientBuilder {
        this.enableRequestDeduplication = enabled
        this.deduplicationWindowMillis = windowMillis
        return this
    }
    
    /**
     * 配置日志拦截器
     */
    fun logging(
        enabled: Boolean = true,
        level: LoggingInterceptor.LogLevel = LoggingInterceptor.LogLevel.BODY,
        formatJson: Boolean = true,
        maxBodyLength: Int = 2000
    ): NetworkClientBuilder {
        this.enableLogging = enabled
        this.logLevel = level
        this.formatJson = formatJson
        this.maxResponseBodyLength = maxBodyLength
        return this
    }
    
    /**
     * 配置缓存拦截器
     */
    fun cache(
        context: Context? = null,
        cache: Cache? = null,
        enabled: Boolean = true,
        maxAgeSeconds: Int = 60,
        maxStaleSeconds: Int = 7 * 24 * 60 * 60
    ): NetworkClientBuilder {
        this.enableCache = enabled
        this.cacheMaxAgeSeconds = maxAgeSeconds
        this.cacheMaxStaleSeconds = maxStaleSeconds
        
        if (enabled) {
            this.cache = cache ?: context?.let { OkHttpCacheHelper.createCache(it) }
        }
        
        return this
    }
    
    /**
     * 配置重试拦截器
     */
    fun retry(
        enabled: Boolean = true,
        maxRetries: Int = 3,
        delayMillis: Long = 1000
    ): NetworkClientBuilder {
        this.enableRetry = enabled
        this.maxRetries = maxRetries
        this.retryDelayMillis = delayMillis
        return this
    }
    
    /**
     * 配置响应验证拦截器
     */
    fun responseValidation(enabled: Boolean = true): NetworkClientBuilder {
        this.enableResponseValidation = enabled
        return this
    }
    
    /**
     * 设置自定义 Moshi 实例
     */
    fun moshi(moshi: Moshi): NetworkClientBuilder {
        this.moshi = moshi
        return this
    }
    
    /**
     * 添加自定义拦截器（应用拦截器）
     * 拦截器会按照添加顺序执行
     */
    fun addInterceptor(interceptor: okhttp3.Interceptor): NetworkClientBuilder {
        customInterceptors.add(interceptor)
        return this
    }
    
    /**
     * 添加自定义网络拦截器（网络拦截器）
     * 网络拦截器会在应用拦截器之后执行
     */
    fun addNetworkInterceptor(interceptor: okhttp3.Interceptor): NetworkClientBuilder {
        customNetworkInterceptors.add(interceptor)
        return this
    }
    
    /**
     * 构建 NetworkClient
     */
    fun build(): NetworkClient {
        val moshiInstance = moshi ?: Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        
        val okHttpClient = buildOkHttpClient()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshiInstance))
            .build()
        
        return NetworkClient(retrofit, moshiInstance, okHttpClient)
    }
    
    /**
     * 构建 OkHttpClient（按照推荐的拦截器顺序）
     */
    private fun buildOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
        
        // 1. 网络状态检查（最前面，提前检查网络）
        if (enableNetworkStatusCheck && networkMonitor != null) {
            builder.addInterceptor(NetworkStatusInterceptor(networkMonitor!!))
        }
        
        // 2. BaseUrl 拦截器（动态切换 BaseUrl）
        baseUrlInterceptor?.let {
            builder.addInterceptor(it)
        }
        
        // 3. 认证拦截器（添加认证 token）
        authTokenProvider?.let {
            builder.addInterceptor(
                AuthInterceptor(it, authHeaderName, authTokenPrefix)
            )
        }
        
        // 4. 请求去重拦截器（防止重复请求）
        if (enableRequestDeduplication) {
            builder.addInterceptor(
                DeduplicationInterceptor(deduplicationWindowMillis)
            )
        }
        
        // 5. 重试拦截器（自动重试失败请求）
        if (enableRetry) {
            builder.addInterceptor(
                RetryInterceptor(maxRetries, retryDelayMillis)
            )
        }
        
        // 6. 日志拦截器（记录请求和响应）
        if (enableLogging) {
            builder.addInterceptor(
                LoggingInterceptor(
                    enabled = true,
                    logLevel = logLevel,
                    formatJson = formatJson,
                    maxBodyLength = maxResponseBodyLength
                )
            )
        }
        
        // 7. 响应验证拦截器（验证响应格式）
        if (enableResponseValidation) {
            builder.addInterceptor(
                ResponseValidationInterceptor(enabled = true)
            )
        }
        
        // 8. 自定义应用拦截器（在响应验证之后）
        customInterceptors.forEach { interceptor ->
            builder.addInterceptor(interceptor)
        }
        
        // 9. 缓存配置
        if (enableCache && cache != null) {
            builder.cache(cache)
            
            // 缓存拦截器（网络拦截器，用于缓存响应）
            builder.addNetworkInterceptor(
                CacheInterceptor(cacheMaxAgeSeconds, cacheMaxStaleSeconds)
            )
            
            // 离线缓存拦截器（应用拦截器，用于离线时使用缓存）
            builder.addInterceptor(
                OfflineCacheInterceptor(
                    maxStale = cacheMaxStaleSeconds,
                    networkMonitor = networkMonitor
                )
            )
        }
        
        // 10. 自定义网络拦截器（在缓存拦截器之后）
        customNetworkInterceptors.forEach { interceptor ->
            builder.addNetworkInterceptor(interceptor)
        }
        
        return builder.build()
    }
}

/**
 * 创建 NetworkClient 构建器
 */
fun networkClient(block: NetworkClientBuilder.() -> Unit): NetworkClient {
    return NetworkClientBuilder().apply(block).build()
}

