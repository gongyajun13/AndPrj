package com.jun.core.network.client

import com.jun.core.common.result.AppResult
import com.jun.core.network.api.safeApiCallEnhanced
import com.jun.core.network.cache.CachePolicy
import com.jun.core.network.cache.NetworkCache
import com.jun.core.network.extension.cachedApiCall
import com.jun.core.network.extension.cachedApiCallFlow
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import timber.log.Timber
import java.io.IOException
import java.util.UUID

/**
 * 网络请求客户端
 * 提供通用的 HTTP 请求方法（GET、POST、PUT、DELETE、PATCH等）
 * 支持缓存策略和统一的错误处理
 * 
 * 设计说明：
 * 1. 与项目架构保持一致，使用 Retrofit 的类型系统和自动 JSON 转换
 * 2. 返回 Response<T> 以保持与现有 API 接口的一致性
 * 3. 提供 safeExecute 方法，使用 safeApiCallEnhanced 处理错误
 * 4. 支持与 BaseRepository.executeNetworkCall 模式兼容
 */
class NetworkClient(
    val retrofit: Retrofit,
    val moshi: Moshi,
    val okHttpClient: okhttp3.OkHttpClient
) {
    
    /**
     * 请求配置
     */
    data class RequestConfig(
        /**
         * 请求头
         */
        val headers: Map<String, String> = emptyMap(),
        
        /**
         * 查询参数（用于 GET 请求）
         */
        val queryParams: Map<String, String> = emptyMap(),
        
        /**
         * 路径参数（用于 URL 路径中的参数）
         */
        val pathParams: Map<String, String> = emptyMap(),
        
        /**
         * 缓存键（用于缓存）
         */
        val cacheKey: String? = null,
        
        /**
         * 缓存实例
         */
        val cache: NetworkCache<String, Any>? = null,
        
        /**
         * 缓存策略
         */
        val cachePolicy: CachePolicy = CachePolicy.NETWORK_FIRST,
        
        /**
         * 请求ID（用于追踪和调试）
         */
        val requestId: String? = null,
        
        /**
         * 是否显示 Loading 对话框
         * 默认 false，不显示。如果需要显示 loading 对话框，请在 requestConfig 中调用 showLoading()
         * 注意：如果框架中有 StateLayout 等组件处理加载状态，通常不需要使用此功能
         */
        val showLoading: Boolean = false,
        
        /**
         * Loading 提示消息
         */
        val loadingMessage: String = "加载中...",
        
        /**
         * Loading 标签（用于区分不同的 loading，可选）
         */
        val loadingTag: String? = null,
        
        /**
         * Loading 超时时间（毫秒），0 表示不超时，默认 0
         * 如果超过此时间 loading 仍未关闭，将自动关闭
         */
        val loadingTimeoutMillis: Long = 0,
        
        /**
         * Loading 样式配置
         * null 表示使用默认配置
         * 默认：null
         */
        val loadingConfig: com.jun.core.common.ui.LoadingDialogConfig? = null
    )
    
    /**
     * GET 请求（返回 Response，与 Retrofit API 接口保持一致）
     * @param url 请求 URL（相对于 baseUrl）
     * @param config 请求配置
     * @return Response<T> Retrofit 响应
     */
    suspend inline fun <reified T> getResponse(
        url: String,
        config: RequestConfig = RequestConfig()
    ): Response<T> {
        return executeRequest<T, Unit>(
            url = url,
            method = HttpMethod.GET,
            body = null,
            config = config
        )
    }
    
    /**
     * GET 请求（安全执行，自动转换为 AppResult）
     * @param url 请求 URL（相对于 baseUrl）
     * @param config 请求配置
     * @return AppResult<T> 请求结果
     */
    suspend inline fun <reified T> get(
        url: String,
        config: RequestConfig = RequestConfig()
    ): AppResult<T> {
        return safeExecute(config) {
            getResponse<T>(url, config)
        }
    }
    
    /**
     * GET 请求（Flow 版本，支持响应式 UI 刷新）
     * 特别适用于 CACHE_AND_NETWORK 策略，可以 emit 缓存和网络结果
     * @param url 请求 URL（相对于 baseUrl）
     * @param config 请求配置
     * @return Flow<AppResult<T>> 请求结果流
     */
    suspend inline fun <reified T> getFlow(
        url: String,
        config: RequestConfig = RequestConfig()
    ): Flow<AppResult<T>> {
        return safeExecuteFlow(config) {
            getResponse<T>(url, config)
        }
    }
    
    /**
     * POST 请求（返回 Response）
     */
    suspend inline fun <reified T, reified B> postResponse(
        url: String,
        body: B? = null,
        config: RequestConfig = RequestConfig()
    ): Response<T> {
        return executeRequest(
            url = url,
            method = HttpMethod.POST,
            body = body,
            config = config
        )
    }
    
    /**
     * POST 请求（安全执行，自动转换为 AppResult）
     */
    suspend inline fun <reified T, reified B> post(
        url: String,
        body: B? = null,
        config: RequestConfig = RequestConfig()
    ): AppResult<T> {
        return safeExecute(config) {
            postResponse<T, B>(url, body, config)
        }
    }
    
    /**
     * POST 请求（Flow 版本，支持响应式 UI 刷新）
     */
    suspend inline fun <reified T, reified B> postFlow(
        url: String,
        body: B? = null,
        config: RequestConfig = RequestConfig()
    ): Flow<AppResult<T>> {
        return safeExecuteFlow(config) {
            postResponse<T, B>(url, body, config)
        }
    }
    
    /**
     * PUT 请求（返回 Response）
     */
    suspend inline fun <reified T, reified B> putResponse(
        url: String,
        body: B? = null,
        config: RequestConfig = RequestConfig()
    ): Response<T> {
        return executeRequest(
            url = url,
            method = HttpMethod.PUT,
            body = body,
            config = config
        )
    }
    
    /**
     * PUT 请求（安全执行，自动转换为 AppResult）
     */
    suspend inline fun <reified T, reified B> put(
        url: String,
        body: B? = null,
        config: RequestConfig = RequestConfig()
    ): AppResult<T> {
        return safeExecute(config) {
            putResponse<T, B>(url, body, config)
        }
    }
    
    /**
     * PUT 请求（Flow 版本，支持响应式 UI 刷新）
     */
    suspend inline fun <reified T, reified B> putFlow(
        url: String,
        body: B? = null,
        config: RequestConfig = RequestConfig()
    ): Flow<AppResult<T>> {
        return safeExecuteFlow(config) {
            putResponse<T, B>(url, body, config)
        }
    }
    
    /**
     * DELETE 请求（返回 Response）
     */
    suspend inline fun <reified T> deleteResponse(
        url: String,
        config: RequestConfig = RequestConfig()
    ): Response<T> {
        return executeRequest<T, Unit>(
            url = url,
            method = HttpMethod.DELETE,
            body = null,
            config = config
        )
    }
    
    /**
     * DELETE 请求（安全执行，自动转换为 AppResult）
     */
    suspend inline fun <reified T> delete(
        url: String,
        config: RequestConfig = RequestConfig()
    ): AppResult<T> {
        return safeExecute(config) {
            deleteResponse<T>(url, config)
        }
    }
    
    /**
     * DELETE 请求（Flow 版本，支持响应式 UI 刷新）
     */
    suspend inline fun <reified T> deleteFlow(
        url: String,
        config: RequestConfig = RequestConfig()
    ): Flow<AppResult<T>> {
        return safeExecuteFlow(config) {
            deleteResponse<T>(url, config)
        }
    }
    
    /**
     * PATCH 请求（返回 Response）
     */
    suspend inline fun <reified T, reified B> patchResponse(
        url: String,
        body: B? = null,
        config: RequestConfig = RequestConfig()
    ): Response<T> {
        return executeRequest(
            url = url,
            method = HttpMethod.PATCH,
            body = body,
            config = config
        )
    }
    
    /**
     * PATCH 请求（安全执行，自动转换为 AppResult）
     */
    suspend inline fun <reified T, reified B> patch(
        url: String,
        body: B? = null,
        config: RequestConfig = RequestConfig()
    ): AppResult<T> {
        return safeExecute(config) {
            patchResponse<T, B>(url, body, config)
        }
    }
    
    /**
     * PATCH 请求（Flow 版本，支持响应式 UI 刷新）
     */
    suspend inline fun <reified T, reified B> patchFlow(
        url: String,
        body: B? = null,
        config: RequestConfig = RequestConfig()
    ): Flow<AppResult<T>> {
        return safeExecuteFlow(config) {
            patchResponse<T, B>(url, body, config)
        }
    }
    
    /**
     * 安全执行请求（使用 safeApiCallEnhanced 处理错误）
     * 与 BaseRepository.executeNetworkCall 模式兼容
     * 
     * @param config 请求配置（用于缓存和 loading）
     * @param apiCall 要执行的 API 调用
     */
    suspend inline fun <reified T> safeExecute(
        config: RequestConfig? = null,
        noinline apiCall: suspend () -> Response<T>
    ): AppResult<T> {
        // 检查协程是否已取消
        coroutineContext[Job]?.ensureActive()
        
        // 显示 Loading（如果配置了）
        val showLoading = config?.showLoading == true
        val loadingTag = config?.loadingTag
        var loadingClosed = false
        
        if (showLoading) {
            com.jun.core.network.loading.LoadingManager.showLoading(
                message = config.loadingMessage,
                tag = loadingTag,
                timeoutMillis = config.loadingTimeoutMillis,
                config = config.loadingConfig
            )
        }
        
        return try {
            // 如果配置了缓存，使用缓存策略
            val result = if (config?.cache != null && config.cacheKey != null) {
                @Suppress("UNCHECKED_CAST")
                val typedCache = config.cache as? NetworkCache<String, T>
                if (typedCache != null) {
                    cachedApiCall(
                        cache = typedCache,
                        cacheKey = config.cacheKey,
                        cachePolicy = config.cachePolicy,
                        apiCall = apiCall
                    )
                } else {
                    safeApiCallEnhanced(apiCall)
                }
            } else {
                safeApiCallEnhanced(apiCall)
            }
            result
        } catch (e: CancellationException) {
            // 协程被取消时，也要关闭 loading
            // 注意：CancellationException 需要重新抛出，否则协程取消机制会失效
            if (showLoading && !loadingClosed) {
                com.jun.core.network.loading.LoadingManager.hideLoading(tag = loadingTag)
                loadingClosed = true
            }
            throw e
        } finally {
            // 确保在所有情况下都关闭 loading
            // 注意：safeApiCallEnhanced 不会抛出异常（除了 CancellationException），
            // 它总是返回 AppResult<T>，所以 finally 块可以安全地关闭 loading
            if (showLoading && !loadingClosed) {
                com.jun.core.network.loading.LoadingManager.hideLoading(tag = loadingTag)
                loadingClosed = true
            }
        }
    }
    
    /**
     * 安全执行请求（Flow 版本，支持响应式 UI 刷新）
     * 特别适用于 CACHE_AND_NETWORK 策略，可以 emit 缓存和网络结果
     * 
     * @param config 请求配置（用于缓存）
     * @param apiCall 要执行的 API 调用
     * @return Flow<AppResult<T>> 请求结果流
     */
    suspend inline fun <reified T> safeExecuteFlow(
        config: RequestConfig? = null,
        noinline apiCall: suspend () -> Response<T>
    ): Flow<AppResult<T>> {
        // 检查协程是否已取消
        coroutineContext[Job]?.ensureActive()
        
        // 显示 Loading（如果配置了）
        val showLoading = config?.showLoading == true
        val loadingTag = config?.loadingTag
        if (showLoading) {
            com.jun.core.network.loading.LoadingManager.showLoading(
                message = config.loadingMessage,
                tag = loadingTag,
                timeoutMillis = config.loadingTimeoutMillis,
                config = config.loadingConfig
            )
        }
        
        // 使用 AtomicBoolean 来跟踪 loading 是否已关闭（用于 Flow 的 onCompletion）
        val loadingClosed = java.util.concurrent.atomic.AtomicBoolean(false)
        
        return flow {
            try {
                // 如果配置了缓存，使用缓存策略的 Flow 版本
                val resultFlow = if (config?.cache != null && config.cacheKey != null) {
                    @Suppress("UNCHECKED_CAST")
                    val typedCache = config.cache as? NetworkCache<String, T>
                    if (typedCache != null) {
                        // 对于 CACHE_AND_NETWORK 策略，Flow 版本可以 emit 缓存和网络结果
                        if (config.cachePolicy == CachePolicy.CACHE_AND_NETWORK) {
                            flow {
                                // 先 emit 缓存（如果存在）
                                val cached = typedCache.get(config.cacheKey)
                                if (cached != null) {
                                    emit(AppResult.Success(cached))
                                }
                                // 然后请求网络并 emit 结果
                                val networkResult = safeApiCallEnhanced(apiCall)
                                if (networkResult is AppResult.Success) {
                                    typedCache.put(config.cacheKey, networkResult.data)
                                }
                                emit(networkResult)
                            }
                        } else {
                            // 其他策略使用 cachedApiCallFlow
                            cachedApiCallFlow(
                                cache = typedCache,
                                cacheKey = config.cacheKey,
                                cachePolicy = config.cachePolicy,
                                apiCall = apiCall
                            )
                        }
                    } else {
                        flow { emit(safeApiCallEnhanced(apiCall)) }
                    }
                } else {
                    flow { emit(safeApiCallEnhanced(apiCall)) }
                }
                
                resultFlow.collect { result ->
                    emit(result)
                }
            } catch (e: CancellationException) {
                // 协程被取消时，也要关闭 loading
                // 注意：CancellationException 需要重新抛出，否则协程取消机制会失效
                if (showLoading && loadingClosed.compareAndSet(false, true)) {
                    com.jun.core.network.loading.LoadingManager.hideLoading(tag = loadingTag)
                }
                throw e
            } finally {
                // 确保在所有情况下都关闭 loading
                // 注意：safeApiCallEnhanced 不会抛出异常（除了 CancellationException），
                // 它总是返回 AppResult<T>，所以 finally 块可以安全地关闭 loading
                if (showLoading && loadingClosed.compareAndSet(false, true)) {
                    com.jun.core.network.loading.LoadingManager.hideLoading(tag = loadingTag)
                }
            }
        }.onCompletion { cause ->
            // 使用 onCompletion 确保在 Flow 完成、取消或异常时都能关闭 loading
            // 这是额外的保护，因为 Flow 可能被取消而不会执行 finally
            // cause 为 null 表示正常完成，非 null 表示异常或取消
            // 使用 AtomicBoolean 确保只关闭一次
            if (showLoading && loadingClosed.compareAndSet(false, true)) {
                com.jun.core.network.loading.LoadingManager.hideLoading(tag = loadingTag)
            }
        }
    }
    
    /**
     * 执行请求（通用方法，返回 Response）
     * 使用 Retrofit 的类型系统和自动 JSON 转换
     */
    suspend inline fun <reified T, reified B : Any> executeRequest(
        url: String,
        method: HttpMethod,
        body: B?,
        config: RequestConfig
    ): Response<T> {
        // 检查协程是否已取消
        coroutineContext[Job]?.ensureActive()
        
        // 生成请求ID（如果没有提供）
        val requestId = config.requestId ?: UUID.randomUUID().toString()
        
        // 构建请求头（添加请求ID）
        val headersWithId = config.headers.toMutableMap().apply {
            put("X-Request-ID", requestId)
        }
        
        // 构建动态 API 接口
        val api = retrofit.create(DynamicApi::class.java)
        
        // 构建请求体
        val requestBody = body?.let {
            try {
                coroutineContext[Job]?.ensureActive() // 检查取消状态
                val json = moshi.adapter(B::class.java).toJson(it)
                json.toRequestBody("application/json; charset=utf-8".toMediaType())
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                throw IOException("请求体序列化失败 [RequestID: $requestId]: ${e.message}", e)
            }
        }
        
        // 构建请求 URL
        val requestUrl = buildUrl(url, config.pathParams)
        
        // 执行请求并获取响应
        val response = try {
            coroutineContext[Job]?.ensureActive() // 检查取消状态
            when (method) {
                HttpMethod.GET -> {
                    api.get(
                        url = requestUrl,
                        headers = headersWithId,
                        queryParams = config.queryParams
                    )
                }
                HttpMethod.POST -> {
                    api.post(
                        url = requestUrl,
                        headers = headersWithId,
                        body = requestBody
                    )
                }
                HttpMethod.PUT -> {
                    api.put(
                        url = requestUrl,
                        headers = headersWithId,
                        body = requestBody
                    )
                }
                HttpMethod.DELETE -> {
                    api.delete(
                        url = requestUrl,
                        headers = headersWithId,
                        queryParams = config.queryParams
                    )
                }
                HttpMethod.PATCH -> {
                    api.patch(
                        url = requestUrl,
                        headers = headersWithId,
                        body = requestBody
                    )
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw IOException("网络请求失败 [RequestID: $requestId]: ${e.message}", e)
        }
        
        // 解析响应体（使用 Retrofit + Moshi 自动转换）
        // 注意：由于 DynamicApi 返回 okhttp3.ResponseBody，无法直接获取 HTTP 状态码
        // 但我们可以假设如果 Retrofit 没有抛出异常，HTTP 请求应该是成功的（2xx）
        // 如果 JSON 解析失败，我们返回 200 状态码（而不是 500），因为 HTTP 请求本身是成功的
        return try {
            coroutineContext[Job]?.ensureActive() // 检查取消状态
            val responseString = response.string()
            
            // 记录原始响应内容（用于调试）
            val responsePreview = if (responseString.length > 500) {
                responseString.take(500) + "..."
            } else {
                responseString
            }
            
            // 对于泛型类型，Moshi 需要完整的类型信息
            // 由于 T 是 reified，我们可以使用 Kotlin 的 typeOf<T>() 来获取完整的类型信息
            // 这是处理 reified 泛型类型的最佳方式
            val type: java.lang.reflect.Type = try {
                // 使用 Kotlin 的 typeOf<T>() 获取完整的 KType 信息（包括泛型参数）
                val kType: KType = typeOf<T>()
                // 将 KType 转换为 Java 的 Type
                kType.javaType
            } catch (e: Exception) {
                // 如果获取失败，回退到使用 Class
                Timber.w(e, "[NetworkClient] 无法获取泛型类型信息，使用 Class [RequestID: $requestId]")
                T::class.java
            }
            
            // 使用获取到的类型创建 adapter
            @Suppress("UNCHECKED_CAST")
            val adapter = moshi.adapter<Any>(type) as com.squareup.moshi.JsonAdapter<T>
            val data = adapter.fromJson(responseString)
            
            if (data != null) {
                // 解析成功，创建成功的 Response（使用 200 状态码）
                Timber.d("[NetworkClient] JSON 解析成功 [RequestID: $requestId]")
                Response.success(data)
            } else {
                // 解析失败（data 为 null），但 HTTP 请求成功
                // 使用 200 状态码（而不是 500），因为 HTTP 请求本身是成功的
                Timber.e("[NetworkClient] JSON 解析失败: data 为 null [RequestID: $requestId]")
                Timber.e("[NetworkClient] 预期类型: ${T::class.java.name}")
                Timber.e("[NetworkClient] 完整响应内容:\n$responseString")
                
                val errorMessage = buildString {
                    appendLine("JSON解析失败: 响应体为空或无法解析为预期类型")
                    appendLine("预期类型: ${T::class.java.simpleName}")
                    appendLine("响应内容预览:")
                    appendLine(responsePreview)
                }
                val errorBody = errorMessage.toResponseBody("text/plain; charset=utf-8".toMediaType())
                retrofit2.Response.error<T>(200, errorBody)
            }
        } catch (e: com.squareup.moshi.JsonDataException) {
            // Moshi JSON 数据类型不匹配异常
            if (e is CancellationException) throw e
            
            Timber.e(e, "[NetworkClient] JSON 解析失败: JsonDataException [RequestID: $requestId]")
            Timber.e("[NetworkClient] 预期类型: ${T::class.java.name}")
            
            val errorMessage = try {
                val responseString = response.string()
                val responsePreview = if (responseString.length > 500) {
                    responseString.take(500) + "..."
                } else {
                    responseString
                }
                
                buildString {
                    appendLine("JSON解析失败: 数据类型不匹配")
                    appendLine("异常类型: ${e.javaClass.simpleName}")
                    appendLine("异常消息: ${e.message}")
                    appendLine("预期类型: ${T::class.java.simpleName}")
                    appendLine("响应内容预览:")
                    appendLine(responsePreview)
                }
            } catch (ex: Exception) {
                Timber.e(ex, "[NetworkClient] 无法读取响应内容 [RequestID: $requestId]")
                "JSON解析失败: ${e.message}\n异常类型: ${e.javaClass.simpleName}\n预期类型: ${T::class.java.simpleName}"
            }
            val errorBody = errorMessage.toResponseBody("text/plain; charset=utf-8".toMediaType())
            retrofit2.Response.error<T>(200, errorBody)
        } catch (e: com.squareup.moshi.JsonEncodingException) {
            // Moshi JSON 编码异常
            if (e is CancellationException) throw e
            
            Timber.e(e, "[NetworkClient] JSON 解析失败: JsonEncodingException [RequestID: $requestId]")
            
            val errorMessage = try {
                val responseString = response.string()
                val responsePreview = if (responseString.length > 500) {
                    responseString.take(500) + "..."
                } else {
                    responseString
                }
                
                buildString {
                    appendLine("JSON解析失败: JSON 编码错误")
                    appendLine("异常类型: ${e.javaClass.simpleName}")
                    appendLine("异常消息: ${e.message}")
                    appendLine("响应内容预览:")
                    appendLine(responsePreview)
                }
            } catch (ex: Exception) {
                Timber.e(ex, "[NetworkClient] 无法读取响应内容 [RequestID: $requestId]")
                "JSON解析失败: ${e.message}\n异常类型: ${e.javaClass.simpleName}"
            }
            val errorBody = errorMessage.toResponseBody("text/plain; charset=utf-8".toMediaType())
            retrofit2.Response.error<T>(200, errorBody)
        } catch (e: Exception) {
            // 其他 JSON 解析异常
            if (e is CancellationException) throw e
            
            Timber.e(e, "[NetworkClient] JSON 解析失败: 未知异常 [RequestID: $requestId]")
            Timber.e("[NetworkClient] 预期类型: ${T::class.java.name}")
            
            val errorMessage = try {
                val responseString = response.string()
                val responsePreview = if (responseString.length > 500) {
                    responseString.take(500) + "..."
                } else {
                    responseString
                }
                
                buildString {
                    appendLine("JSON解析失败: ${e.javaClass.simpleName}")
                    appendLine("异常消息: ${e.message}")
                    appendLine("预期类型: ${T::class.java.simpleName}")
                    appendLine("响应内容预览:")
                    appendLine(responsePreview)
                }
            } catch (ex: Exception) {
                Timber.e(ex, "[NetworkClient] 无法读取响应内容 [RequestID: $requestId]")
                "JSON解析失败: ${e.message}\n异常类型: ${e.javaClass.simpleName}\n预期类型: ${T::class.java.simpleName}"
            }
            val errorBody = errorMessage.toResponseBody("text/plain; charset=utf-8".toMediaType())
            retrofit2.Response.error<T>(200, errorBody)
        }
    }
    
    /**
     * TypeReference 辅助类
     * 用于在运行时通过反射获取泛型类型信息
     * 这是处理泛型类型解析的标准模式
     * 
     * 使用示例：
     * ```kotlin
     * val type = object : TypeReference<WanAndroidResponse<List<Banner>>>() {}.type
     * val adapter = moshi.adapter<Any>(type)
     * ```
     */
    abstract class TypeReference<T> {
        val type: java.lang.reflect.Type
            get() = (javaClass.genericSuperclass as java.lang.reflect.ParameterizedType)
                .actualTypeArguments[0]
    }
    
    /**
     * 构建 URL（替换路径参数）
     */
    fun buildUrl(url: String, pathParams: Map<String, String>): String {
        var result = url
        pathParams.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
    
    /**
     * HTTP 方法枚举
     */
    enum class HttpMethod {
        GET, POST, PUT, DELETE, PATCH
    }
    
    /**
     * 动态 API 接口
     * 用于支持任意 URL 的请求
     * 使用 okhttp3.ResponseBody 来避免类型擦除问题
     * 注意：由于返回 ResponseBody，无法直接获取 HTTP 状态码
     * 如果 HTTP 请求失败，Retrofit 会抛出异常
     * 如果 HTTP 请求成功但 JSON 解析失败，我们返回 200 状态码（而不是 500）
     */
    interface DynamicApi {
        @GET
        suspend fun get(
            @Url url: String,
            @HeaderMap headers: Map<String, String>,
            @QueryMap queryParams: Map<String, String>
        ): okhttp3.ResponseBody
        
        @POST
        suspend fun post(
            @Url url: String,
            @HeaderMap headers: Map<String, String>,
            @Body body: okhttp3.RequestBody?
        ): okhttp3.ResponseBody
        
        @PUT
        suspend fun put(
            @Url url: String,
            @HeaderMap headers: Map<String, String>,
            @Body body: okhttp3.RequestBody?
        ): okhttp3.ResponseBody
        
        @DELETE
        suspend fun delete(
            @Url url: String,
            @HeaderMap headers: Map<String, String>,
            @QueryMap queryParams: Map<String, String>
        ): okhttp3.ResponseBody
        
        @PATCH
        suspend fun patch(
            @Url url: String,
            @HeaderMap headers: Map<String, String>,
            @Body body: okhttp3.RequestBody?
        ): okhttp3.ResponseBody
    }
}

/**
 * 请求配置构建器
 * 提供链式调用的方式构建请求配置
 */
class RequestConfigBuilder {
    private var headers: MutableMap<String, String> = mutableMapOf()
    private var queryParams: MutableMap<String, String> = mutableMapOf()
    private var pathParams: MutableMap<String, String> = mutableMapOf()
    private var cacheKey: String? = null
    private var cache: NetworkCache<String, Any>? = null
    private var cachePolicy: CachePolicy = CachePolicy.NETWORK_FIRST
    
    private var requestId: String? = null
    
    private var showLoading: Boolean = false
    private var loadingMessage: String = "加载中..."
    private var loadingTag: String? = null
    private var loadingTimeoutMillis: Long = 0
    private var loadingConfig: com.jun.core.common.ui.LoadingDialogConfig? = null
    
    /**
     * 添加请求头
     */
    fun header(key: String, value: String): RequestConfigBuilder {
        headers[key] = value
        return this
    }
    
    /**
     * 添加多个请求头
     */
    fun headers(headers: Map<String, String>): RequestConfigBuilder {
        this.headers.putAll(headers)
        return this
    }
    
    /**
     * 设置 Authorization 请求头（便捷方法）
     */
    fun authorization(token: String, prefix: String = "Bearer "): RequestConfigBuilder {
        headers["Authorization"] = "$prefix$token"
        return this
    }
    
    /**
     * 设置 Content-Type 请求头（便捷方法）
     */
    fun contentType(type: String = "application/json"): RequestConfigBuilder {
        headers["Content-Type"] = type
        return this
    }
    
    /**
     * 添加查询参数
     */
    fun queryParam(key: String, value: String): RequestConfigBuilder {
        queryParams[key] = value
        return this
    }
    
    /**
     * 添加多个查询参数
     */
    fun queryParams(params: Map<String, String>): RequestConfigBuilder {
        this.queryParams.putAll(params)
        return this
    }
    
    /**
     * 添加路径参数
     */
    fun pathParam(key: String, value: String): RequestConfigBuilder {
        pathParams[key] = value
        return this
    }
    
    /**
     * 添加多个路径参数
     */
    fun pathParams(params: Map<String, String>): RequestConfigBuilder {
        this.pathParams.putAll(params)
        return this
    }
    
    /**
     * 设置缓存键
     */
    fun cacheKey(key: String): RequestConfigBuilder {
        this.cacheKey = key
        return this
    }
    
    /**
     * 设置缓存实例
     */
    fun cache(cache: NetworkCache<String, Any>): RequestConfigBuilder {
        this.cache = cache
        return this
    }
    
    /**
     * 设置缓存策略
     */
    fun cachePolicy(policy: CachePolicy): RequestConfigBuilder {
        this.cachePolicy = policy
        return this
    }
    
    /**
     * 设置请求ID（用于追踪和调试）
     */
    fun requestId(id: String): RequestConfigBuilder {
        this.requestId = id
        return this
    }
    
    /**
     * 显示 Loading 对话框
     * 
     * 注意：默认情况下不显示 loading，因为框架中有 StateLayout 等组件可以处理加载状态。
     * 只有在需要全局 loading 对话框的场景下才使用此方法（例如：不需要 StateLayout 的简单场景）。
     * 
     * @param message Loading 提示消息，默认为 "加载中..."
     * @param tag Loading 标签（可选，用于区分不同的 loading）
     * @param timeoutMillis Loading 超时时间（毫秒），0 表示不超时，默认 0
     * @param config Loading 样式配置，null 表示使用默认配置
     */
    fun showLoading(
        message: String = "加载中...",
        tag: String? = null,
        timeoutMillis: Long = 0,
        config: com.jun.core.common.ui.LoadingDialogConfig? = null
    ): RequestConfigBuilder {
        this.showLoading = true
        this.loadingMessage = message
        this.loadingTag = tag
        this.loadingTimeoutMillis = timeoutMillis
        this.loadingConfig = config
        return this
    }
    
    /**
     * 隐藏 Loading 对话框（默认不显示）
     */
    fun hideLoading(): RequestConfigBuilder {
        this.showLoading = false
        return this
    }
    
    /**
     * 构建请求配置
     */
    fun build(): NetworkClient.RequestConfig {
        return NetworkClient.RequestConfig(
            headers = headers,
            queryParams = queryParams,
            pathParams = pathParams,
            cacheKey = cacheKey,
            cache = cache,
            cachePolicy = cachePolicy,
            requestId = requestId,
            showLoading = showLoading,
            loadingMessage = loadingMessage,
            loadingTag = loadingTag,
            loadingTimeoutMillis = loadingTimeoutMillis,
            loadingConfig = loadingConfig
        )
    }
}

/**
 * 创建请求配置构建器
 */
fun requestConfig(block: RequestConfigBuilder.() -> Unit): NetworkClient.RequestConfig {
    return RequestConfigBuilder().apply(block).build()
}
