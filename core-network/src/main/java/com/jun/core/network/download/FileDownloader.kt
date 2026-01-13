package com.jun.core.network.download

import com.jun.core.network.client.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.coroutineContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 文件下载状态
 */
sealed class DownloadState {
    /**
     * 准备下载
     */
    object Preparing : DownloadState()
    
    /**
     * 下载中
     * @param progress 下载进度（0-100）
     * @param downloadedBytes 已下载字节数
     * @param totalBytes 总字节数（如果未知则为 -1）
     * @param speed 下载速度（字节/秒）
     */
    data class Downloading(
        val progress: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speed: Long
    ) : DownloadState()
    
    /**
     * 下载已暂停
     * @param downloadedBytes 已下载字节数
     * @param totalBytes 总字节数
     */
    data class Paused(
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadState()
    
    /**
     * 下载完成
     * @param file 下载的文件
     */
    data class Completed(val file: File) : DownloadState()
    
    /**
     * 下载失败
     * @param error 错误信息
     */
    data class Failed(val error: String) : DownloadState()
    
    /**
     * 下载已取消
     */
    object Cancelled : DownloadState()
}

/**
 * 文件下载器
 * 基于 core-network 框架实现文件下载功能
 * 
 * 设计说明：
 * 1. 使用 NetworkClient 的 OkHttpClient（支持所有拦截器：日志、认证、重试等）
 * 2. 使用 NetworkClient 的 RequestConfig 来配置请求（请求头、查询参数等）
 * 3. 使用 Flow 发送下载进度和状态
 * 4. 支持取消下载
 * 5. 统一的错误处理
 * 6. 自动计算下载速度
 * 
 * 为什么不直接使用 NetworkClient？
 * - NetworkClient 主要用于 JSON API 请求，会调用 response.string() 一次性读取响应体
 * - 文件下载需要流式处理，不能一次性读取到内存（大文件会 OOM）
 * - 文件下载需要进度回调，NetworkClient 没有此功能
 * - 文件下载不需要 JSON 解析
 * 
 * 使用示例：
 * ```kotlin
 * @Inject
 * lateinit var networkClient: NetworkClient
 * 
 * val downloader = FileDownloader(networkClient)
 * 
 * // 基础下载
 * lifecycleScope.launch {
 *     downloader.download(url, file)
 *         .collect { state ->
 *             when (state) {
 *                 is DownloadState.Downloading -> {
 *                     progressBar.progress = state.progress
 *                     speedText.text = "${state.speed.formatFileSize()}/s"
 *                 }
 *                 is DownloadState.Completed -> {
 *                     // 下载完成
 *                 }
 *                 is DownloadState.Failed -> {
 *                     // 下载失败
 *                 }
 *                 else -> {}
 *             }
 *         }
 * }
 * 
 * // 使用 RequestConfig 配置请求
 * val config = requestConfig {
 *     header("Authorization", "Bearer token")
 *     queryParam("version", "1.0.0")
 * }
 * downloader.download(url, file, config)
 * ```
 */
class FileDownloader(
    private val networkClient: NetworkClient
) {
    
    /**
     * 获取 NetworkClient 的 OkHttpClient 实例
     */
    private val okHttpClient: OkHttpClient
        get() = networkClient.okHttpClient
    
    /**
     * 下载文件（支持断点续传）
     * 
     * @param url 下载地址
     * @param file 保存的文件
     * @param requestConfig 请求配置（可选，用于设置请求头、查询参数等）
     * @param downloadConfig 下载配置（可选，用于自定义下载行为）
     * @return Flow<DownloadState> 下载状态流
     */
    fun download(
        url: String,
        file: File,
        requestConfig: NetworkClient.RequestConfig? = null,
        downloadConfig: DownloadConfig = DownloadConfig.DEFAULT
    ): Flow<DownloadState> = flow {
        try {
            // 确保目录存在
            file.parentFile?.mkdirs()
            
            // 发送准备状态
            emit(DownloadState.Preparing)
            
            // 生成请求ID（如果没有提供）
            val requestId = requestConfig?.requestId ?: UUID.randomUUID().toString()
            
            // 构建请求URL（处理路径参数）
            val requestUrl = requestConfig?.let { buildUrl(url, it.pathParams) } ?: url
            
            // 检查是否支持断点续传
            val existingFileSize = if (file.exists() && downloadConfig.resumeFromExisting) {
                file.length()
            } else {
                0L
            }
            
            // 如果文件已存在且需要重新下载，删除旧文件
            if (file.exists() && !downloadConfig.resumeFromExisting) {
                file.delete()
            }
            
            // 构建请求头（添加请求ID和配置的请求头）
            val headers = requestConfig?.headers?.toMutableMap() ?: mutableMapOf()
            headers["X-Request-ID"] = requestId
            
            // 如果支持断点续传且文件已存在，添加 Range 请求头
            if (existingFileSize > 0) {
                headers["Range"] = "bytes=$existingFileSize-"
                Timber.d("[FileDownloader] 断点续传: 从 $existingFileSize 字节处继续下载")
            }
            
            // 构建请求
            val requestBuilder = Request.Builder()
                .url(requestUrl)
            
            // 添加请求头
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            
            // 添加查询参数（如果有）
            if (!requestConfig?.queryParams.isNullOrEmpty()) {
                val urlBuilder = requestUrl.toHttpUrlOrNull()?.newBuilder()
                if (urlBuilder != null) {
                    requestConfig.queryParams.forEach { (key, value) ->
                        urlBuilder.addQueryParameter(key, value)
                    }
                    requestBuilder.url(urlBuilder.build())
                }
            }
            
            // 如果配置了超时，创建新的 OkHttpClient
            val clientToUse = if (downloadConfig.timeoutMillis > 0) {
                okHttpClient.newBuilder()
                    .callTimeout(downloadConfig.timeoutMillis, TimeUnit.MILLISECONDS)
                    .readTimeout(downloadConfig.timeoutMillis, TimeUnit.MILLISECONDS)
                    .writeTimeout(downloadConfig.timeoutMillis, TimeUnit.MILLISECONDS)
                    .build()
            } else {
                okHttpClient
            }
            
            val request = requestBuilder.build()
            
            Timber.d("[FileDownloader] 开始下载 [RequestID: $requestId, URL: $requestUrl, Resume: ${downloadConfig.resumeFromExisting}, ExistingSize: $existingFileSize, BufferSize: ${downloadConfig.bufferSize}, UpdateInterval: ${downloadConfig.progressUpdateInterval}ms]")
            
            val response = clientToUse.newCall(request).execute()
            
            // 检查响应状态码
            // 206 Partial Content 表示支持断点续传
            // 200 OK 表示从头开始下载
            // 416 Requested Range Not Satisfiable 表示 Range 请求无效
            val isPartialContent = response.code == 206
            val isSuccess = response.isSuccessful || isPartialContent
            
            // 处理 416 错误：Range 请求无效
            if (response.code == 416) {
                Timber.w("[FileDownloader] HTTP 416: Range 请求无效 (ExistingSize: $existingFileSize)")
                
                // 检查文件是否已下载完成
                val contentLengthHeader = response.header("Content-Range")?.let {
                    // Content-Range 格式: "bytes */total" 或 "bytes start-end/total"
                    val totalMatch = Regex("bytes \\*/?(\\d+)").find(it)
                    totalMatch?.groupValues?.get(1)?.toLongOrNull()
                } ?: response.header("Content-Length")?.toLongOrNull()
                
                if (contentLengthHeader != null && existingFileSize >= contentLengthHeader) {
                    // 文件已下载完成
                    Timber.d("[FileDownloader] 文件已下载完成 (ExistingSize: $existingFileSize, TotalSize: $contentLengthHeader)")
                    emit(DownloadState.Completed(file))
                    return@flow
                }
                
                // Range 请求无效，删除已下载的文件，从头开始下载
                Timber.d("[FileDownloader] Range 请求无效，删除已下载文件，从头开始下载")
                if (file.exists()) {
                    file.delete()
                }
                
                // 重新构建请求（不包含 Range 头）
                val retryRequestBuilder = Request.Builder()
                    .url(requestUrl)
                
                // 添加请求头（移除 Range 头）
                val retryHeaders = headers.toMutableMap()
                retryHeaders.remove("Range")
                retryHeaders.forEach { (key, value) ->
                    retryRequestBuilder.header(key, value)
                }
                
                // 添加查询参数（如果有）
                if (!requestConfig?.queryParams.isNullOrEmpty()) {
                    val urlBuilder = requestUrl.toHttpUrlOrNull()?.newBuilder()
                    if (urlBuilder != null) {
                        requestConfig!!.queryParams.forEach { (key, value) ->
                            urlBuilder.addQueryParameter(key, value)
                        }
                        retryRequestBuilder.url(urlBuilder.build())
                    }
                }
                
                val retryRequest = retryRequestBuilder.build()
                val retryResponse = clientToUse.newCall(retryRequest).execute()
                
                if (!retryResponse.isSuccessful) {
                    throw IOException("下载失败: HTTP ${retryResponse.code}")
                }
                
                // 使用重试的响应继续下载
                val retryBody = retryResponse.body ?: throw IOException("响应体为空")
                val retryContentLength = retryBody.contentLength()
                
                // 下载文件（从头开始）
                var totalBytesRead = 0L
                var lastUpdateTime = System.currentTimeMillis()
                var lastBytesRead = 0L
                
                retryBody.byteStream().use { input ->
                    RandomAccessFile(file, "rw").use { raf ->
                        raf.setLength(0) // 清空文件
                        
                        val buffer = ByteArray(downloadConfig.bufferSize)
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            // 检查是否被取消
                            coroutineContext[Job]?.ensureActive()
                            
                            raf.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            // 计算下载速度（根据配置的更新间隔）
                            val currentTime = System.currentTimeMillis()
                            val timeDelta = currentTime - lastUpdateTime
                            
                            if (timeDelta >= downloadConfig.progressUpdateInterval || totalBytesRead == retryContentLength) {
                                val bytesDelta = totalBytesRead - lastBytesRead
                                val speed = if (timeDelta > 0) (bytesDelta * 1000) / timeDelta else 0L
                                
                                val progress = if (retryContentLength > 0) {
                                    ((totalBytesRead * 100) / retryContentLength).toInt()
                                } else {
                                    0
                                }
                                
                                emit(DownloadState.Downloading(
                                    progress = progress,
                                    downloadedBytes = totalBytesRead,
                                    totalBytes = retryContentLength,
                                    speed = speed
                                ))
                                
                                downloadConfig.onProgress?.invoke(progress, totalBytesRead, retryContentLength)
                                
                                lastUpdateTime = currentTime
                                lastBytesRead = totalBytesRead
                            }
                        }
                    }
                }
                
                emit(DownloadState.Completed(file))
                return@flow
            }
            
            if (!isSuccess) {
                throw IOException("下载失败: HTTP ${response.code}")
            }
            
            val body = response.body ?: throw IOException("响应体为空")
            
            // 获取文件总大小
            val contentLength = if (isPartialContent) {
                // 206 响应：Content-Range 格式为 "bytes start-end/total"
                val contentRange = response.header("Content-Range")
                if (contentRange != null) {
                    val totalMatch = Regex("bytes \\d+-\\d+/(\\d+)").find(contentRange)
                    totalMatch?.groupValues?.get(1)?.toLongOrNull() ?: body.contentLength()
                } else {
                    body.contentLength() + existingFileSize
                }
            } else {
                body.contentLength()
            }
            
            // 下载文件（使用 RandomAccessFile 支持断点续传）
            var totalBytesRead = existingFileSize
            var lastUpdateTime = System.currentTimeMillis()
            var lastBytesRead = existingFileSize
            
            body.byteStream().use { input ->
                RandomAccessFile(file, "rw").use { raf ->
                    // 如果是从断点续传，移动到文件末尾
                    if (existingFileSize > 0) {
                        raf.seek(existingFileSize)
                    }
                    
                    val buffer = ByteArray(downloadConfig.bufferSize)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // 检查是否被取消
                        coroutineContext[Job]?.ensureActive()
                        
                        raf.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        // 计算下载速度（根据配置的更新间隔）
                        val currentTime = System.currentTimeMillis()
                        val timeDelta = currentTime - lastUpdateTime
                        
                        if (timeDelta >= downloadConfig.progressUpdateInterval || totalBytesRead == contentLength) {
                            val bytesDelta = totalBytesRead - lastBytesRead
                            val speed = if (timeDelta > 0) {
                                (bytesDelta * 1000 / timeDelta)
                            } else {
                                0L
                            }
                            
                            // 计算进度
                            val progress = if (contentLength > 0) {
                                ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }
                            
                            // 发送下载中状态
                            val downloadingState = DownloadState.Downloading(
                                progress = progress,
                                downloadedBytes = totalBytesRead,
                                totalBytes = contentLength,
                                speed = speed
                            )
                            emit(downloadingState)
                            
                            // 调用进度回调（兼容旧代码）
                            downloadConfig.onProgress?.invoke(progress, totalBytesRead, contentLength)
                            
                            lastUpdateTime = currentTime
                            lastBytesRead = totalBytesRead
                        }
                    }
                }
            }
            
            // 验证文件大小（如果启用）
            if (downloadConfig.verifyFileSize && contentLength > 0) {
                val actualFileSize = file.length()
                if (actualFileSize != contentLength) {
                    val errorMsg = "文件大小验证失败: 期望 ${contentLength} 字节，实际 ${actualFileSize} 字节"
                    Timber.e("[FileDownloader] $errorMsg")
                    emit(DownloadState.Failed(errorMsg))
                    return@flow
                }
            }
            
            // 下载完成
            emit(DownloadState.Completed(file))
            Timber.d("[FileDownloader] 下载完成 [RequestID: $requestId]: ${file.absolutePath}, 大小: $totalBytesRead 字节")
        } catch (e: kotlinx.coroutines.CancellationException) {
            // 下载被取消
            Timber.d("[FileDownloader] 下载已取消: $url")
            emit(DownloadState.Cancelled)
            throw e
        } catch (e: SocketTimeoutException) {
            // 超时错误
            val errorMessage = "下载超时: ${e.message ?: "连接超时"}"
            Timber.e(e, "[FileDownloader] $errorMessage: $url")
            emit(DownloadState.Failed(errorMessage))
        } catch (e: UnknownHostException) {
            // 网络不可达
            val errorMessage = "网络不可达: ${e.message ?: "无法连接到服务器"}"
            Timber.e(e, "[FileDownloader] $errorMessage: $url")
            emit(DownloadState.Failed(errorMessage))
        } catch (e: IOException) {
            // IO 错误
            val errorMessage = when {
                e.message?.contains("timeout", ignoreCase = true) == true -> "下载超时: ${e.message}"
                e.message?.contains("network", ignoreCase = true) == true -> "网络错误: ${e.message}"
                e.message?.contains("connection", ignoreCase = true) == true -> "连接错误: ${e.message}"
                else -> "下载失败: ${e.message ?: "IO错误"}"
            }
            Timber.e(e, "[FileDownloader] $errorMessage: $url")
            emit(DownloadState.Failed(errorMessage))
        } catch (e: Exception) {
            // 其他错误
            val errorMessage = "下载失败: ${e.message ?: e.javaClass.simpleName}"
            Timber.e(e, "[FileDownloader] $errorMessage: $url")
            emit(DownloadState.Failed(errorMessage))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 下载文件（简化版本，只返回成功或失败）
     * 
     * @param url 下载地址
     * @param file 保存的文件
     * @param requestConfig 请求配置（可选）
     * @param downloadConfig 下载配置（可选）
     * @return Flow<DownloadState> 下载状态流（只包含 Preparing, Completed, Failed, Cancelled）
     */
    fun downloadSimple(
        url: String,
        file: File,
        requestConfig: NetworkClient.RequestConfig? = null,
        downloadConfig: DownloadConfig = DownloadConfig.DEFAULT
    ): Flow<DownloadState> = flow {
        download(url, file, requestConfig, downloadConfig).collect { state ->
            // 只发送关键状态，不发送下载中的详细进度
            when (state) {
                is DownloadState.Preparing,
                is DownloadState.Completed,
                is DownloadState.Failed,
                is DownloadState.Cancelled,
                is DownloadState.Paused -> emit(state)
                is DownloadState.Downloading -> {
                    // 忽略下载中状态
                }
            }
        }
    }
    
    /**
     * 下载文件（兼容旧版本 API）
     * 
     * @param url 下载地址
     * @param file 保存的文件
     * @param config 请求配置（可选，用于设置请求头、查询参数等）
     * @param resumeFromExisting 是否从已存在的文件继续下载（断点续传），默认 true
     * @param onProgress 进度回调（可选，用于兼容旧代码）
     * @return Flow<DownloadState> 下载状态流
     * @deprecated 使用 download(url, file, requestConfig, downloadConfig) 替代
     */
    @Deprecated(
        message = "使用 download(url, file, requestConfig, downloadConfig) 替代",
        replaceWith = ReplaceWith("download(url, file, config, DownloadConfig(resumeFromExisting = resumeFromExisting, onProgress = onProgress))")
    )
    fun download(
        url: String,
        file: File,
        config: NetworkClient.RequestConfig? = null,
        resumeFromExisting: Boolean = true,
        onProgress: ((progress: Int, downloaded: Long, total: Long) -> Unit)? = null
    ): Flow<DownloadState> {
        val downloadConfig = DownloadConfig(
            resumeFromExisting = resumeFromExisting,
            onProgress = onProgress
        )
        return download(url, file, config, downloadConfig)
    }
    
    /**
     * 构建 URL（替换路径参数）
     * 与 NetworkClient.buildUrl 保持一致
     */
    private fun buildUrl(url: String, pathParams: Map<String, String>): String {
        var result = url
        pathParams.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
}

