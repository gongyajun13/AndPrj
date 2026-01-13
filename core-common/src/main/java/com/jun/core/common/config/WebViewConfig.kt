package com.jun.core.common.config

/**
 * WebView 配置接口
 * 用于配置 WebView 的行为和特性
 * 
 * 安全说明：
 * - 默认配置已启用安全设置，防止内容泄露
 * - 如需访问本地文件，请使用 permissive() 配置（不推荐）
 */
interface WebViewConfig {
    /**
     * 是否启用 JavaScript
     */
    val enableJavaScript: Boolean
    
    /**
     * 是否启用 DOM 存储
     */
    val enableDomStorage: Boolean
    
    /**
     * 是否启用数据库存储
     */
    val enableDatabase: Boolean
    
    /**
     * 是否启用文件访问
     */
    val enableFileAccess: Boolean
    
    /**
     * 是否启用内容访问
     */
    val enableContentAccess: Boolean
    
    /**
     * 是否启用缩放
     */
    val enableZoom: Boolean
    
    /**
     * 是否启用内置缩放控件
     */
    val enableBuiltInZoomControls: Boolean
    
    /**
     * 是否显示缩放控件
     */
    val enableDisplayZoomControls: Boolean
    
    /**
     * 是否启用混合内容（HTTP 和 HTTPS）
     */
    val enableMixedContent: Boolean
    
    /**
     * 缓存模式
     * LOAD_DEFAULT: 默认缓存模式
     * LOAD_CACHE_ELSE_NETWORK: 优先使用缓存
     * LOAD_NO_CACHE: 不使用缓存
     * LOAD_CACHE_ONLY: 只使用缓存
     */
    val cacheMode: Int
    
    /**
     * 是否启用硬件加速
     */
    val enableHardwareAcceleration: Boolean
    
    /**
     * 用户代理字符串（可选）
     */
    val userAgentString: String?
    
    /**
     * 是否允许 file:// 协议（安全风险：可能导致本地文件泄露）
     * 默认 false，除非明确需要访问本地文件
     */
    val allowFileScheme: Boolean
    
    /**
     * 是否允许通用文件访问（allowUniversalAccessFromFileURLs）
     * 安全风险：允许 file:// 页面访问其他来源的内容
     * 默认 false
     */
    val allowUniversalAccessFromFile: Boolean
    
    /**
     * 是否允许 JavaScript 访问文件（allowFileAccessFromFileURLs）
     * 安全风险：允许 JavaScript 访问本地文件
     * 默认 false
     */
    val allowFileAccessFromFileURLs: Boolean
    
    /**
     * 是否启用安全浏览（Safe Browsing）
     * Android 8.0+ 支持
     * 默认 true
     */
    val enableSafeBrowsing: Boolean
    
    /**
     * 默认配置（安全配置）
     * 已启用所有安全设置，防止内容泄露
     */
    companion object {
        /**
         * 创建默认配置（安全配置）
         * 注意：默认配置已启用安全设置，防止内容泄露
         */
        fun default(): WebViewConfig = object : WebViewConfig {
            override val enableJavaScript: Boolean = true
            override val enableDomStorage: Boolean = true
            override val enableDatabase: Boolean = true
            override val enableFileAccess: Boolean = false // 默认禁用文件访问
            override val enableContentAccess: Boolean = false // 默认禁用内容访问
            override val enableZoom: Boolean = false
            override val enableBuiltInZoomControls: Boolean = false
            override val enableDisplayZoomControls: Boolean = false
            override val enableMixedContent: Boolean = false // 默认禁用混合内容
            override val cacheMode: Int = 0 // LOAD_DEFAULT
            override val enableHardwareAcceleration: Boolean = true
            override val userAgentString: String? = null
            override val allowFileScheme: Boolean = false // 默认禁用 file:// 协议
            override val allowUniversalAccessFromFile: Boolean = false // 默认禁用
            override val allowFileAccessFromFileURLs: Boolean = false // 默认禁用
            override val enableSafeBrowsing: Boolean = true // 默认启用安全浏览
        }
        
        /**
         * 创建宽松配置（不推荐，仅用于特殊场景）
         * 警告：此配置存在安全风险，可能导致内容泄露
         */
        fun permissive(): WebViewConfig = object : WebViewConfig {
            override val enableJavaScript: Boolean = true
            override val enableDomStorage: Boolean = true
            override val enableDatabase: Boolean = true
            override val enableFileAccess: Boolean = true
            override val enableContentAccess: Boolean = true
            override val enableZoom: Boolean = true
            override val enableBuiltInZoomControls: Boolean = true
            override val enableDisplayZoomControls: Boolean = false
            override val enableMixedContent: Boolean = true
            override val cacheMode: Int = 0
            override val enableHardwareAcceleration: Boolean = true
            override val userAgentString: String? = null
            override val allowFileScheme: Boolean = true
            override val allowUniversalAccessFromFile: Boolean = false // 即使宽松配置也建议禁用
            override val allowFileAccessFromFileURLs: Boolean = false // 即使宽松配置也建议禁用
            override val enableSafeBrowsing: Boolean = true
        }
    }
}
