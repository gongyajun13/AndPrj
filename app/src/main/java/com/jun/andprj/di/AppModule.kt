package com.jun.andprj.di

import android.content.Context
import coil.ImageLoader
import coil.util.DebugLogger
import com.jun.andprj.BuildConfig
import com.jun.andprj.config.AppConfigImpl
import com.jun.andprj.config.DatabaseConfigImpl
import com.jun.andprj.config.NetworkConfigImpl
import com.jun.core.common.config.AppConfig
import com.jun.core.common.network.NetworkMonitor
import com.jun.core.common.network.NetworkMonitorImpl
import com.jun.core.database.config.DatabaseConfig
import com.jun.core.network.config.NetworkConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    
    companion object {
        @Provides
        @Singleton
        fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
            return ImageLoader.Builder(context)
                .apply {
                    if (BuildConfig.DEBUG) {
                        logger(DebugLogger())
                    }
                }
                .build()
        }
        
        @Provides
        @Singleton
        fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
            return NetworkMonitorImpl(context)
        }
    }
    
    @Binds
    @Singleton
    abstract fun bindAppConfig(appConfigImpl: AppConfigImpl): AppConfig
    
    @Binds
    @Singleton
    abstract fun bindNetworkConfig(networkConfigImpl: NetworkConfigImpl): NetworkConfig
    
    @Binds
    @Singleton
    abstract fun bindDatabaseConfig(databaseConfigImpl: DatabaseConfigImpl): DatabaseConfig
}



