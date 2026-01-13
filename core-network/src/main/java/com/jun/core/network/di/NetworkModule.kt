package com.jun.core.network.di

import com.jun.core.network.client.NetworkClient
import com.jun.core.network.config.NetworkConfig
import com.jun.core.network.config.NetworkInterceptorManager
import com.jun.core.network.interceptor.BaseUrlInterceptor
import com.jun.core.network.interceptor.LoggingInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 网络模块的 Hilt 依赖注入配置
 * 注意：子项目需要提供 NetworkConfig 的实现
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }
    
    /**
     * 提供 BaseUrlInterceptor 单例
     * 用于动态切换 BaseUrl
     */
    @Provides
    @Singleton
    fun provideBaseUrlInterceptor(
        networkConfig: NetworkConfig
    ): BaseUrlInterceptor {
        return BaseUrlInterceptor(networkConfig.baseUrl)
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        networkConfig: NetworkConfig,
        baseUrlInterceptor: BaseUrlInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(networkConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(networkConfig.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(networkConfig.writeTimeoutSeconds, TimeUnit.SECONDS)
        
        // 添加 BaseUrl 拦截器（用于动态切换 BaseUrl）
        // 注意：拦截器顺序很重要，BaseUrlInterceptor 应该在日志拦截器之前
        builder.addInterceptor(baseUrlInterceptor)
        
        // 添加日志拦截器
        if (networkConfig.enableLogging) {
            val logLevel = when (networkConfig.logLevel) {
                NetworkConfig.LogLevel.NONE -> LoggingInterceptor.LogLevel.NONE
                NetworkConfig.LogLevel.BASIC -> LoggingInterceptor.LogLevel.BASIC
                NetworkConfig.LogLevel.HEADERS -> LoggingInterceptor.LogLevel.HEADERS
                NetworkConfig.LogLevel.BODY -> LoggingInterceptor.LogLevel.BODY
            }
            
            val loggingInterceptor = LoggingInterceptor(
                enabled = true,
                logLevel = logLevel,
                formatJson = networkConfig.formatJsonResponse,
                maxBodyLength = networkConfig.maxResponseBodyLength
            )
            builder.addInterceptor(loggingInterceptor)
        }
        
        return builder.build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        baseUrlInterceptor: BaseUrlInterceptor
    ): Retrofit {
        // 注意：由于使用了 BaseUrlInterceptor，这里使用一个占位符 baseUrl
        // BaseUrlInterceptor 会在运行时动态替换实际的 baseUrl
        // 使用 baseUrlInterceptor 的初始值作为占位符
        return Retrofit.Builder()
            .baseUrl(baseUrlInterceptor.getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    
    /**
     * 提供 NetworkClient 单例实例
     * 使用已配置的 Retrofit 和 Moshi 实例
     * 确保全局只有一个 NetworkClient 实例，避免重复创建
     */
    @Provides
    @Singleton
    fun provideNetworkClient(
        retrofit: Retrofit,
        moshi: Moshi,
        okHttpClient: OkHttpClient
    ): NetworkClient {
        return NetworkClient(retrofit, moshi, okHttpClient)
    }
    
    /**
     * 提供网络拦截器管理器单例
     * 用于动态调整拦截器配置（如 BaseUrl、Token 等）
     * 自动注册 BaseUrlInterceptor，支持一键切换 baseUrl
     */
    @Provides
    @Singleton
    fun provideNetworkInterceptorManager(
        baseUrlInterceptor: BaseUrlInterceptor
    ): NetworkInterceptorManager {
        return NetworkInterceptorManager().apply {
            setBaseUrlInterceptor(baseUrlInterceptor)
        }
    }
}

