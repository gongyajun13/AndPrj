package com.jun.andprj.di

import com.jun.andprj.data.remote.api.UserApi
import com.jun.andprj.data.remote.api.WanAndroidApi
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
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * App 模块的网络 API 绑定
 * 注意：网络配置已在 core-network 模块中提供
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class WanAndroidRetrofit
    
    /**
     * 为WanAndroid API创建独立的Retrofit实例
     */
    @Provides
    @Singleton
    @WanAndroidRetrofit
    fun provideWanAndroidRetrofit(
        moshi: Moshi
    ): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                LoggingInterceptor(
                    enabled = true,
                    logLevel = LoggingInterceptor.LogLevel.BODY,
                    formatJson = true,
                    maxBodyLength = 2000
                )
            )
            .build()
        
        return Retrofit.Builder()
            .baseUrl("https://www.wanandroid.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideWanAndroidApi(
        @WanAndroidRetrofit retrofit: Retrofit
    ): WanAndroidApi {
        return retrofit.create(WanAndroidApi::class.java)
    }
}



