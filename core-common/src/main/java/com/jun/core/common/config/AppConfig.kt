package com.jun.core.common.config

/**
 * 应用程序配置接口
 * 子项目可以实现此接口来提供项目特定的配置
 */
interface AppConfig {
    /**
     * 是否为调试模式
     */
    val isDebug: Boolean
    
    /**
     * 应用程序名称
     */
    val appName: String
    
    /**
     * 应用程序版本名称
     */
    val versionName: String
    
    /**
     * 应用程序版本代码
     */
    val versionCode: Int
}

