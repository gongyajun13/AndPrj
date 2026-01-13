package com.jun.core.database.di

import android.content.Context
import androidx.room.Room
import com.jun.core.database.config.DatabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库模块的 Hilt 依赖注入配置
 * 注意：子项目需要提供 DatabaseConfig 的实现和具体的 Database 类
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {
    
    companion object {
        /**
         * 提供 Room Database Builder
         * 子项目需要在自己的 DatabaseModule 中提供具体的 Database 实例
         */
        @Provides
        @Singleton
        fun provideRoomDatabaseBuilder(
            @ApplicationContext context: Context,
            databaseConfig: DatabaseConfig
        ): RoomDatabaseBuilder {
            return RoomDatabaseBuilder(
                context = context,
                databaseName = databaseConfig.databaseName,
                databaseVersion = databaseConfig.databaseVersion,
                fallbackToDestructiveMigration = databaseConfig.fallbackToDestructiveMigration,
                exportSchema = databaseConfig.exportSchema
            )
        }
    }
}

/**
 * Room Database Builder 封装
 * 用于统一配置 Room Database
 */
data class RoomDatabaseBuilder(
    val context: Context,
    val databaseName: String,
    val databaseVersion: Int,
    val fallbackToDestructiveMigration: Boolean,
    val exportSchema: Boolean
) {
    fun <T : androidx.room.RoomDatabase> build(clazz: Class<T>): T {
        val builder = Room.databaseBuilder(context, clazz, databaseName)
            .apply {
                if (fallbackToDestructiveMigration) {
                    fallbackToDestructiveMigration()
                }
            }
        return builder.build()
    }
}

