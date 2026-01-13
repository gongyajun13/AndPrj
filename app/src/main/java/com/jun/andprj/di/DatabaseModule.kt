package com.jun.andprj.di

import android.content.Context
import com.jun.andprj.config.DatabaseConfigImpl
import com.jun.andprj.data.local.database.AppDatabase
import com.jun.andprj.data.local.database.dao.UserDao
import com.jun.core.database.di.RoomDatabaseBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App 模块的数据库绑定
 * 注意：数据库配置已在 core-database 模块中提供
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        databaseConfig: DatabaseConfigImpl,
        roomDatabaseBuilder: RoomDatabaseBuilder
    ): AppDatabase {
        return roomDatabaseBuilder.build(AppDatabase::class.java)
    }
    
    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
}

