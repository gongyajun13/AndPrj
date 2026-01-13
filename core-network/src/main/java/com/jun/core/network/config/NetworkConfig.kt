package com.jun.core.network.config

/**
 * 网络配置接口
 * 子项目可以实现此接口来提供项目特定的网络配置
 */
interface NetworkConfig {
    /**
     * API 基础 URL
     */
    val baseUrl: String
    
    /**
     * 连接超时时间（秒）
     */
    val connectTimeoutSeconds: Long
        get() = 30
    
    /**
     * 读取超时时间（秒）
     */
    val readTimeoutSeconds: Long
        get() = 30
    
    /**
     * 写入超时时间（秒）
     */
    val writeTimeoutSeconds: Long
        get() = 30
    
    /**
     * 是否启用日志拦截器
     */
    val enableLogging: Boolean
        get() = true
    
    /**
     * 日志级别
     */
    val logLevel: LogLevel
        get() = LogLevel.BODY
    
    /**
     * 日志级别枚举
     */
    enum class LogLevel {
        NONE,       // 不记录日志
        BASIC,      // 只记录请求方法和URL
        HEADERS,    // 记录请求方法和URL以及请求头
        BODY        // 记录请求方法和URL、请求头以及请求体和响应体
    }
    
    /**
     * 是否格式化JSON响应体
     */
    val formatJsonResponse: Boolean
        get() = true
    
    /**
     * 响应体最大显示长度（字符数），超过此长度将被截断，0表示不限制
     */
    val maxResponseBodyLength: Int
        get() = 2000
    
    /**
     * 是否启用网络缓存
     */
    val enableCache: Boolean
        get() = false
    
    /**
     * 缓存最大存活时间（秒）
     */
    val cacheMaxAgeSeconds: Long
        get() = 60
    
    /**
     * 离线缓存最大存活时间（秒）
     */
    val cacheMaxStaleSeconds: Long
        get() = 7 * 24 * 60 * 60 // 7天
    
    /**
     * 是否启用请求去重
     */
    val enableRequestDeduplication: Boolean
        get() = true
    
    /**
     * 请求去重时间窗口（毫秒）
     */
    val deduplicationWindowMillis: Long
        get() = 1000
    
    /**
     * 是否启用网络状态检查
     */
    val enableNetworkStatusCheck: Boolean
        get() = true
}

