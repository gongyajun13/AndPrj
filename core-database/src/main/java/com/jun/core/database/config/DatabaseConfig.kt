package com.jun.core.database.config

/**
 * 数据库配置接口
 * 子项目可以实现此接口来提供项目特定的数据库配置
 */
interface DatabaseConfig {
    /**
     * 数据库名称
     */
    val databaseName: String
    
    /**
     * 数据库版本
     */
    val databaseVersion: Int
    
    /**
     * 是否启用回退到破坏性迁移（仅用于开发阶段）
     */
    val fallbackToDestructiveMigration: Boolean
        get() = false
    
    /**
     * 是否导出 Schema
     */
    val exportSchema: Boolean
        get() = false
}

