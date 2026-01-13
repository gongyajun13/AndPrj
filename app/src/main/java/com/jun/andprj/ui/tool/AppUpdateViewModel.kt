package com.jun.andprj.ui.tool

import android.content.Context
import android.os.Build
import androidx.lifecycle.viewModelScope
import com.jun.core.common.extension.formatFileSize
import com.jun.core.common.network.NetworkMonitor
import com.jun.core.network.client.NetworkClient
import com.jun.core.network.download.DownloadState
import com.jun.core.network.download.FileDownloader
import com.jun.core.ui.viewmodel.BaseViewModel
import kotlin.math.abs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * App 升级 UI 状态
 */
sealed class AppUpdateUiState {
    /**
     * 初始状态
     */
    object Initial : AppUpdateUiState()
    
    /**
     * 检查更新中
     */
    object Checking : AppUpdateUiState()
    
    /**
     * 检查更新失败
     * @param error 错误信息
     */
    data class CheckFailed(val error: String) : AppUpdateUiState()
    
    /**
     * 发现新版本
     * @param latestVersion 最新版本号
     * @param latestVersionCode 最新版本代码
     * @param downloadUrl 下载地址
     * @param updateInfo 更新信息
     * @param isForceUpdate 是否强制更新
     * @param fileSize 文件大小（字节）
     */
    data class UpdateAvailable(
        val latestVersion: String,
        val latestVersionCode: Int,
        val downloadUrl: String,
        val updateInfo: String,
        val isForceUpdate: Boolean = false,
        val fileSize: Long = 0L
    ) : AppUpdateUiState()
    
    /**
     * 已是最新版本
     */
    object AlreadyLatest : AppUpdateUiState()
    
    /**
     * 下载中
     * @param progress 下载进度（0-100）
     * @param downloadedBytes 已下载字节数
     * @param totalBytes 总字节数
     * @param speed 下载速度（字节/秒）
     * @param estimatedTimeRemaining 预计剩余时间（秒，-1 表示未知）
     */
    data class Downloading(
        val progress: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speed: Long,
        val estimatedTimeRemaining: Long = -1L
    ) : AppUpdateUiState()
    
    /**
     * 下载已暂停
     * @param downloadedBytes 已下载字节数
     * @param totalBytes 总字节数
     */
    data class DownloadPaused(
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : AppUpdateUiState()
    
    /**
     * 下载完成
     * @param file 下载的文件
     */
    data class DownloadCompleted(val file: File) : AppUpdateUiState()
    
    /**
     * 下载失败
     * @param error 错误信息
     * @param canRetry 是否可以重试
     */
    data class DownloadFailed(
        val error: String,
        val canRetry: Boolean = true
    ) : AppUpdateUiState()
    
    /**
     * 下载已取消
     */
    object DownloadCancelled : AppUpdateUiState()
    
    /**
     * 检测到未完成的下载
     * @param downloadedBytes 已下载字节数
     * @param totalBytes 总字节数（如果未知则为 -1）
     * @param downloadUrl 下载地址（如果已知）
     */
    data class IncompleteDownloadDetected(
        val downloadedBytes: Long,
        val totalBytes: Long = -1L,
        val downloadUrl: String? = null
    ) : AppUpdateUiState()
    
    /**
     * APK 文件已完整，可以安装
     * @param file 已下载的 APK 文件
     */
    data class ApkFileReady(val file: File) : AppUpdateUiState()
}

/**
 * App 升级 ViewModel
 * 管理 App 升级相关的业务逻辑和状态
 */
/**
 * App 升级 ViewModel
 * 管理 App 升级相关的业务逻辑和状态
 */
@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkClient: NetworkClient,
    private val cacheManager: AppUpdateCacheManager,
    private val networkMonitor: NetworkMonitor
) : BaseViewModel<com.jun.core.ui.state.UiState<Nothing>>() {
    
    /**
     * App 升级 UI 状态
     */
    private val _appUpdateState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Initial)
    val appUpdateState: StateFlow<AppUpdateUiState> = _appUpdateState.asStateFlow()
    
    private val fileDownloader: FileDownloader by lazy {
        FileDownloader(networkClient)
    }
    
    private val apkFile: File by lazy {
        File(context.getExternalFilesDir("apk"), "app_update.apk")
    }
    
    private var downloadJob: Job? = null
    
    /**
     * 当前版本信息
     */
    val currentVersion: String = "v${com.jun.andprj.BuildConfig.VERSION_NAME} (${com.jun.andprj.BuildConfig.VERSION_CODE})"
    
    /**
     * 当前版本代码
     */
    val currentVersionCode: Int = com.jun.andprj.BuildConfig.VERSION_CODE
    
    /**
     * APK 文件路径
     */
    val apkFilePath: File = apkFile
    
    /**
     * 是否正在下载
     */
    val isDownloading: Boolean
        get() = downloadJob?.isActive == true
    
    /**
     * APK 文件是否存在
     */
    val isApkFileExists: Boolean
        get() = apkFile.exists()
    
    /**
     * 下载地址（用于重试和继续）
     */
    private var lastDownloadUrl: String? = null
    
    /**
     * 保存的文件大小（用于判断文件是否完整）
     */
    private var savedFileSize: Long = 0L
    
    /**
     * 当前缓存数据
     */
    private var currentCacheData: AppUpdateCacheData? = null
    
    /**
     * 是否已暂停
     */
    private var isPaused: Boolean = false
    
    /**
     * 暂停时的下载进度（用于继续下载）
     */
    private var pausedProgress: Long = 0L
    
    /**
     * 上次缓存更新时间（用于节流）
     */
    private var lastCacheUpdateTime = 0L
    
    /**
     * 缓存更新间隔（毫秒）
     */
    private val CACHE_UPDATE_INTERVAL = 1000L // 1秒更新一次
    
    /**
     * 上次缓存更新的进度百分比（用于按进度更新）
     */
    private var lastCacheUpdateProgress = -1
    
    override fun createInitialState(): com.jun.core.ui.state.UiState<Nothing> {
        return com.jun.core.ui.state.UiState.Initial
    }
    
    /**
     * 更新 App 升级状态
     */
    private fun updateAppUpdateState(state: AppUpdateUiState) {
        _appUpdateState.value = state
    }
    
    /**
     * 获取文件大小（通过 HEAD 请求）
     * @param url 文件下载地址
     * @return 文件大小（字节），如果获取失败返回 0
     */
    private suspend fun getFileSize(url: String): Long {
        return try {
            val request = okhttp3.Request.Builder()
                .url(url)
                .method("HEAD", null) // HEAD 请求，只获取响应头，不下载文件内容
                .build()
            
            val response = networkClient.okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
                Timber.d("[AppUpdateViewModel] 获取文件大小成功: ${contentLength.formatFileSize()} ($contentLength 字节)")
                contentLength
            } else {
                Timber.w("[AppUpdateViewModel] 获取文件大小失败: HTTP ${response.code}")
                0L
            }
        } catch (e: Exception) {
            Timber.e(e, "[AppUpdateViewModel] 获取文件大小异常")
            0L
        }
    }
    
    /**
     * 检查更新
     */
    fun checkUpdate() {
        // 检查网络状态
        if (!networkMonitor.isNetworkAvailable()) {
            updateAppUpdateState(AppUpdateUiState.CheckFailed("网络不可用，请检查网络连接"))
            setError("网络不可用，请检查网络连接", null)
            return
        }
        
        updateAppUpdateState(AppUpdateUiState.Checking)
        setLoading()
        
        viewModelScope.launch {
            try {
                // 模拟检查更新（实际应该调用服务器API）
                // TODO: 替换为真实的 API 调用
                kotlinx.coroutines.delay(1000)
                
                // 模拟发现新版本
                val latestVersion = "v1.0.1"
                val latestVersionCode = 2
                val downloadUrl = "https://aiera-android.oss-cn-shanghai.aliyuncs.com/aiera/40070/hotdog-4.00.70-40070-20260104062056_40070_jiagu_sign.apk"
                val updateInfo = "📱 版本更新内容：\n\n" +
                        "✨ 新功能：\n" +
                        "• 新增暗黑模式支持\n" +
                        "• 优化界面交互体验\n\n" +
                        "🐛 问题修复：\n" +
                        "• 修复已知崩溃问题\n" +
                        "• 优化内存占用\n\n" +
                        "⚡ 性能优化：\n" +
                        "• 提升应用启动速度\n" +
                        "• 优化网络请求性能"
                val isForceUpdate = false
                
                // 比较版本号
                if (latestVersionCode > currentVersionCode) {
                    // 保存下载地址，用于继续下载
                    lastDownloadUrl = downloadUrl
                    
                    // 获取真实的文件大小
                    val fileSize = getFileSize(downloadUrl)
                    // 保存文件大小，用于后续判断文件是否完整
                    savedFileSize = fileSize
                    
                    Timber.d("[AppUpdateViewModel] 检查更新成功: 版本=$latestVersion, 文件大小=${fileSize.formatFileSize()}")
                    
                    // 保存缓存数据
                    val cacheData = AppUpdateCacheData(
                        downloadUrl = downloadUrl,
                        totalBytes = fileSize,
                        downloadedBytes = if (apkFile.exists()) apkFile.length() else 0L,
                        latestVersion = latestVersion,
                        latestVersionCode = latestVersionCode,
                        updateInfo = updateInfo,
                        isForceUpdate = isForceUpdate
                    )
                    cacheManager.saveCacheData(cacheData)
                    currentCacheData = cacheData
                    
                    val updateState = AppUpdateUiState.UpdateAvailable(
                        latestVersion = latestVersion,
                        latestVersionCode = latestVersionCode,
                        downloadUrl = downloadUrl,
                        updateInfo = updateInfo,
                        isForceUpdate = isForceUpdate,
                        fileSize = fileSize
                    )
                    updateAppUpdateState(updateState)
                    
                    // 检查是否有未完成的下载文件
                    if (fileSize > 0 && isApkFileIncomplete(fileSize)) {
                        val downloadedBytes = apkFile.length()
                        Timber.d("[AppUpdateViewModel] 检测到未完成的下载文件，大小: $downloadedBytes, 预期: $fileSize")
                        // 如果当前状态是 IncompleteDownloadDetected，更新为 UpdateAvailable
                        // 因为现在有了完整的更新信息，UI 会显示继续下载选项
                    }
                    
                    setSuccess(Unit)
                } else {
                    updateAppUpdateState(AppUpdateUiState.AlreadyLatest)
                    setSuccess(Unit)
                }
            } catch (e: Exception) {
                Timber.e(e, "检查更新失败")
                val errorMessage = e.message ?: "网络连接失败，请检查网络设置"
                updateAppUpdateState(AppUpdateUiState.CheckFailed(errorMessage))
                setError("检查更新失败: $errorMessage", e)
            }
        }
    }
    
    /**
     * 开始下载
     * @param downloadUrl 下载地址
     * @param resumeFromExisting 是否从已存在的文件继续下载（断点续传），默认 true
     */
    fun startDownload(downloadUrl: String, resumeFromExisting: Boolean = true) {
        if (downloadJob?.isActive == true) {
            Timber.w("[AppUpdateViewModel] 正在下载中，忽略重复请求")
            return
        }
        
        // 检查网络状态
        if (!networkMonitor.isNetworkAvailable()) {
            updateAppUpdateState(
                AppUpdateUiState.DownloadFailed(
                    error = "网络不可用，请检查网络连接",
                    canRetry = true
                )
            )
            setError("网络不可用，请检查网络连接", null)
            return
        }
        
        // 保存下载地址，用于重试和继续
        lastDownloadUrl = downloadUrl
        isPaused = false
        // 重置缓存更新相关变量
        lastCacheUpdateTime = 0L
        lastCacheUpdateProgress = -1
        
        // 确保目录存在
        apkFile.parentFile?.mkdirs()
        
        downloadJob = viewModelScope.launch {
            fileDownloader.download(
                downloadUrl, 
                apkFile, 
                downloadConfig = com.jun.core.network.download.DownloadConfig(
                    resumeFromExisting = resumeFromExisting
                )
            )
                .catch { e ->
                    // Flow 异常处理
                    Timber.e(e, "[AppUpdateViewModel] 下载 Flow 异常")
                    updateAppUpdateState(
                        AppUpdateUiState.DownloadFailed(
                            error = e.message ?: "下载失败",
                            canRetry = true
                        )
                    )
                    downloadJob = null
                }
                .collect { state ->
                    when (state) {
                        is DownloadState.Preparing -> {
                            // 准备下载，不更新 UI 状态
                        }
                        
                        is DownloadState.Downloading -> {
                            // 计算预计剩余时间
                            val estimatedTime = if (state.speed > 0 && state.totalBytes > 0) {
                                val remainingBytes = state.totalBytes - state.downloadedBytes
                                remainingBytes / state.speed
                            } else {
                                -1L
                            }
                            
                            // 节流更新缓存：每 1 秒或每 5% 进度更新一次
                            val currentTime = System.currentTimeMillis()
                            val progressDiff = abs(state.progress - lastCacheUpdateProgress)
                            val timeDiff = currentTime - lastCacheUpdateTime
                            
                            val shouldUpdateCache = timeDiff >= CACHE_UPDATE_INTERVAL || progressDiff >= 5
                            
                            if (shouldUpdateCache) {
                                // 更新缓存中的下载进度
                                viewModelScope.launch {
                                    cacheManager.updateDownloadProgress(state.downloadedBytes, state.totalBytes)
                                }
                                lastCacheUpdateTime = currentTime
                                lastCacheUpdateProgress = state.progress
                            }
                            
                            // 更新缓存的文件大小（如果总大小变化）
                            if (state.totalBytes > 0 && savedFileSize != state.totalBytes) {
                                savedFileSize = state.totalBytes
                                viewModelScope.launch {
                                    cacheManager.updateFileSize(state.totalBytes)
                                }
                            }
                            
                            updateAppUpdateState(
                                AppUpdateUiState.Downloading(
                                    progress = state.progress,
                                    downloadedBytes = state.downloadedBytes,
                                    totalBytes = state.totalBytes,
                                    speed = state.speed,
                                    estimatedTimeRemaining = estimatedTime
                                )
                            )
                        }
                        
                        is DownloadState.Paused -> {
                            // 下载已暂停（由 FileDownloader 发出，但通常由 ViewModel 控制）
                            pausedProgress = state.downloadedBytes
                            updateAppUpdateState(
                                AppUpdateUiState.DownloadPaused(
                                    downloadedBytes = state.downloadedBytes,
                                    totalBytes = state.totalBytes
                                )
                            )
                        }
                        
                        is DownloadState.Completed -> {
                            // 下载完成时，更新缓存数据
                            val completedFileSize = state.file.length()
                            if (completedFileSize > 0) {
                                savedFileSize = completedFileSize
                                // 更新缓存中的下载进度（标记为完成）
                                cacheManager.updateDownloadProgress(completedFileSize, savedFileSize)
                                Timber.d("[AppUpdateViewModel] 下载完成，保存文件大小: ${completedFileSize.formatFileSize()}")
                            }
                            
                            updateAppUpdateState(
                                AppUpdateUiState.DownloadCompleted(state.file)
                            )
                            downloadJob = null
                            lastDownloadUrl = null
                            isPaused = false
                            pausedProgress = 0L
                        }
                        
                        is DownloadState.Failed -> {
                            updateAppUpdateState(
                                AppUpdateUiState.DownloadFailed(
                                    error = state.error,
                                    canRetry = true
                                )
                            )
                            downloadJob = null
                        }
                        
                        is DownloadState.Cancelled -> {
                            updateAppUpdateState(
                                AppUpdateUiState.DownloadCancelled
                            )
                            downloadJob = null
                        }
                    }
                }
        }
    }
    
    /**
     * 暂停下载
     */
    fun pauseDownload() {
        if (downloadJob?.isActive == true) {
            // 获取当前下载进度
            val currentState = _appUpdateState.value
            if (currentState is AppUpdateUiState.Downloading) {
                pausedProgress = currentState.downloadedBytes
            }
            
            // 取消下载 Job（这会触发 CancellationException，但文件已部分下载）
            downloadJob?.cancel()
            downloadJob = null
            isPaused = true
            
            // 更新状态为暂停
            val currentState2 = _appUpdateState.value
            if (currentState2 is AppUpdateUiState.Downloading) {
                updateAppUpdateState(
                    AppUpdateUiState.DownloadPaused(
                        downloadedBytes = currentState2.downloadedBytes,
                        totalBytes = currentState2.totalBytes
                    )
                )
            }
            
            Timber.d("[AppUpdateViewModel] 下载已暂停，已下载: $pausedProgress 字节")
        }
    }
    
    /**
     * 继续下载（从断点处继续）
     */
    fun resumeDownload() {
        val downloadUrl = lastDownloadUrl
        if (downloadUrl != null && isPaused) {
            Timber.d("[AppUpdateViewModel] 继续下载: $downloadUrl, 从 $pausedProgress 字节处继续")
            // 从断点处继续下载（使用断点续传）
            startDownload(downloadUrl, resumeFromExisting = true)
        } else {
            Timber.w("[AppUpdateViewModel] 无法继续下载：没有可用的下载地址或未暂停")
        }
    }
    
    /**
     * 重试下载
     */
    fun retryDownload() {
        val downloadUrl = lastDownloadUrl
        if (downloadUrl != null) {
            Timber.d("[AppUpdateViewModel] 重试下载: $downloadUrl")
            startDownload(downloadUrl)
        } else {
            // 如果没有保存的下载地址，尝试从状态中获取
            val currentState = _appUpdateState.value
            if (currentState is AppUpdateUiState.UpdateAvailable) {
                Timber.d("[AppUpdateViewModel] 从状态中获取下载地址并重试: ${currentState.downloadUrl}")
                startDownload(currentState.downloadUrl)
            } else {
                Timber.w("[AppUpdateViewModel] 无法重试下载：没有可用的下载地址")
            }
        }
    }
    
    /**
     * 清除下载状态（用于重置 UI）
     */
    fun clearDownloadState() {
        downloadJob?.cancel()
        downloadJob = null
        lastDownloadUrl = null
        updateAppUpdateState(AppUpdateUiState.Initial)
    }
    
    /**
     * 取消下载
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        isPaused = false
        pausedProgress = 0L
        // 取消下载时删除部分下载的文件
        if (apkFile.exists()) {
            apkFile.delete()
            Timber.d("[AppUpdateViewModel] 取消下载并删除部分下载的文件: ${apkFile.absolutePath}")
        }
        // 清除缓存数据
        viewModelScope.launch {
            cacheManager.clearCache()
        }
        updateAppUpdateState(AppUpdateUiState.DownloadCancelled)
    }
    
    /**
     * 删除已下载的 APK 文件
     */
    fun deleteApkFile() {
        if (apkFile.exists()) {
            val deleted = apkFile.delete()
            if (deleted) {
                Timber.d("[AppUpdateViewModel] 已删除 APK 文件: ${apkFile.absolutePath}")
                // 清除缓存数据
                viewModelScope.launch {
                    cacheManager.clearCache()
                }
            } else {
                Timber.w("[AppUpdateViewModel] 删除 APK 文件失败: ${apkFile.absolutePath}")
            }
        }
    }
    
    /**
     * 获取 APK 文件大小
     */
    fun getApkFileSize(): Long {
        return if (apkFile.exists()) {
            apkFile.length()
        } else {
            0L
        }
    }
    
    /**
     * 验证 APK 文件是否完整（简单检查：文件大小是否大于 0）
     */
    fun isApkFileValid(): Boolean {
        return apkFile.exists() && apkFile.length() > 0
    }
    
    /**
     * 检查文件是否可能是未完成的下载
     * 通过比较文件大小和预期大小来判断
     * @param expectedSize 预期的文件大小（字节），如果为 0 则无法判断
     * @return true 如果文件存在但可能未完成
     */
    fun isApkFileIncomplete(expectedSize: Long = 0L): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return false
        }
        
        // 如果提供了预期大小，比较文件大小
        if (expectedSize > 0) {
            val currentSize = apkFile.length()
            // 如果文件大小小于预期的 95%，认为可能是未完成的
            // 同时，如果文件大小小于 1MB，也认为可能是未完成的（APK 文件通常都比较大）
            return currentSize < expectedSize * 0.95 || currentSize < 1024 * 1024
        }
        
        // 如果没有预期大小，通过文件大小粗略判断
        // APK 文件通常至少几 MB，如果小于 1MB 可能是未完成的
        val currentSize = apkFile.length()
        return currentSize < 1024 * 1024
    }
    
    /**
     * 检查是否有未完成的下载文件
     * 在检查更新后调用，如果发现文件存在但可能未完成，返回 true
     */
    fun hasIncompleteDownload(): Boolean {
        val currentState = _appUpdateState.value
        return if (currentState is AppUpdateUiState.UpdateAvailable) {
            // 如果已检查更新，使用预期的文件大小来判断
            isApkFileIncomplete(currentState.fileSize)
        } else {
            // 如果没有更新信息，使用粗略判断
            isApkFileIncomplete()
        }
    }
    
    /**
     * 初始化并检查本地下载进度
     * 在进入界面时调用，读取缓存数据并判断文件状态
     */
    fun checkLocalDownloadProgress() {
        viewModelScope.launch {
            try {
                // 1. 读取缓存数据
                currentCacheData = cacheManager.getCurrentCacheData()
                val cacheData = currentCacheData
                
                Timber.d("[AppUpdateViewModel] 读取缓存数据: ${cacheData?.hasCache()}")
                
                // 2. 如果没有缓存数据，显示检查更新按钮
                if (cacheData == null || !cacheData.hasCache()) {
                    Timber.d("[AppUpdateViewModel] 无缓存数据，显示检查更新")
                    updateAppUpdateState(AppUpdateUiState.Initial)
                    return@launch
                }
                
                // 3. 恢复缓存的数据
                lastDownloadUrl = cacheData.downloadUrl
                savedFileSize = cacheData.totalBytes
                
                // 4. 检查文件是否存在
                if (!apkFile.exists() || apkFile.length() == 0L) {
                    // 文件不存在，但可能有缓存数据（可能是下载失败或文件被删除）
                    Timber.d("[AppUpdateViewModel] 文件不存在，但存在缓存数据，清除缓存")
                    cacheManager.clearCache()
                    updateAppUpdateState(AppUpdateUiState.Initial)
                    return@launch
                }
                
                // 5. 获取实际文件大小
                val actualFileSize = apkFile.length()
                
                // 6. 更新缓存中的已下载字节数（使用实际文件大小）
                if (actualFileSize != cacheData.downloadedBytes) {
                    cacheManager.updateDownloadProgress(actualFileSize, cacheData.totalBytes)
                }
                
                // 7. 判断文件是否完整（根据缓存的真实文件大小和已缓存文件大小对比）
                val isComplete = if (cacheData.totalBytes > 0) {
                    // 使用缓存的文件大小判断
                    cacheData.copy(downloadedBytes = actualFileSize).isFileComplete()
                } else {
                    // 如果没有缓存的文件大小，使用粗略判断
                    actualFileSize >= 1024 * 1024 // 至少1MB
                }
                
                if (isComplete) {
                    // 文件完整，显示安装APK
                    Timber.d("[AppUpdateViewModel] 文件完整，大小: ${actualFileSize.formatFileSize()}，可以安装")
                    updateAppUpdateState(AppUpdateUiState.ApkFileReady(apkFile))
                } else {
                    // 文件不完整，显示下载进度、继续下载、重新下载
                    Timber.d("[AppUpdateViewModel] 文件不完整，已下载: ${actualFileSize.formatFileSize()}, 总大小: ${cacheData.totalBytes.formatFileSize()}")
                    updateAppUpdateState(
                        AppUpdateUiState.IncompleteDownloadDetected(
                            downloadedBytes = actualFileSize,
                            totalBytes = cacheData.totalBytes,
                            downloadUrl = cacheData.downloadUrl
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "[AppUpdateViewModel] 检查本地下载进度失败")
                updateAppUpdateState(AppUpdateUiState.Initial)
            }
        }
    }
    
    /**
     * 检查 APK 文件是否完整（更严格的检查）
     * 通过尝试解析 APK 文件来判断
     * @param expectedSize 预期的文件大小（字节），如果为 0 则不检查大小
     * @return true 如果文件完整
     */
    fun isApkFileComplete(expectedSize: Long = 0L): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return false
        }
        
        val currentSize = apkFile.length()
        
        // 如果提供了预期大小，比较文件大小
        if (expectedSize > 0) {
            // 文件大小应该在预期的 95%-105% 范围内（允许一些误差）
            val sizeMatch = currentSize >= expectedSize * 0.95 && currentSize <= expectedSize * 1.05
            if (!sizeMatch) {
                return false
            }
        }
        
        // APK 文件通常至少几 MB，如果小于 1MB 很可能是未完成的
        if (currentSize < 1024 * 1024) {
            return false
        }
        
        // 简单验证：检查文件是否可以读取
        // 更严格的验证可以在安装时进行
        return try {
            apkFile.canRead()
        } catch (e: Exception) {
            Timber.e(e, "检查 APK 文件完整性失败")
            false
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        downloadJob?.cancel()
    }
}

