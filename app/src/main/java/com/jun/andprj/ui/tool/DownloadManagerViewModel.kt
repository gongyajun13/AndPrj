package com.jun.andprj.ui.tool

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.jun.core.network.client.NetworkClient
import com.jun.core.ui.state.UiState
import com.jun.core.ui.viewmodel.BaseViewModel
import com.jun.core.ui.webview.download.DownloadManager
import com.jun.core.ui.webview.download.DownloadTask
import com.jun.core.ui.webview.download.WebViewDownloadInterceptor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 下载管理 ViewModel
 * 负责下载任务的业务逻辑处理
 */
@HiltViewModel
class DownloadManagerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkClient: NetworkClient
) : BaseViewModel<UiState<List<DownloadTask>>>() {
    
    /**
     * 下载任务列表
     */
    private val _downloadTasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadTasks: StateFlow<List<DownloadTask>> = _downloadTasks.asStateFlow()
    
    /**
     * 是否为空状态
     */
    private val _isEmpty = MutableStateFlow(false)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()
    
    /**
     * 操作结果消息
     */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    /**
     * 需要确认的操作
     */
    private val _confirmAction = MutableStateFlow<ConfirmAction?>(null)
    val confirmAction: StateFlow<ConfirmAction?> = _confirmAction.asStateFlow()
    
    /**
     * 需要打开的文件
     */
    private val _openFile = MutableStateFlow<File?>(null)
    val openFile: StateFlow<File?> = _openFile.asStateFlow()
    
    init {
        // 初始化 DownloadManager
        DownloadManager.init(
            context = context,
            networkClient = networkClient,
            maxConcurrentDownloads = 3,
            autoCleanCompletedTasks = false
        )
        
        // 加载缓存的下载任务
        loadCachedDownloadTasks()
    }
    
    override fun createInitialState(): UiState<List<DownloadTask>> {
        return UiState.Initial
    }
    
    /**
     * 加载缓存的下载任务
     */
    private fun loadCachedDownloadTasks() {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir.resolve("downloads").apply { mkdirs() }
        DownloadManager.loadCachedCompletedTasks(downloadDir, context)
    }
    
    /**
     * 观察下载任务变化
     * 注意：由于 DownloadManager 使用 LiveData，我们需要在 Activity 中观察
     * 这里提供一个方法来更新任务列表
     */
    fun updateDownloadTasks(tasks: List<DownloadTask>) {
        // 创建任务副本，确保 DiffUtil 能检测到变化
        val newList = tasks.map { task ->
            DownloadTask(
                id = task.id,
                url = task.url,
                fileName = task.fileName,
                filePath = task.filePath,
                totalBytes = task.totalBytes,
                downloadedBytes = task.downloadedBytes,
                progress = task.progress,
                speed = task.speed,
                state = task.state,
                error = task.error,
                userAgent = task.userAgent,
                contentDisposition = task.contentDisposition,
                mimeType = task.mimeType,
                contentLength = task.contentLength
            )
        }
        _downloadTasks.value = newList
        _isEmpty.value = newList.isEmpty()
    }
    
    companion object {
        // Intent Extra Keys（与 DownloadManagerActivity 保持一致）
        private const val EXTRA_DOWNLOAD_URL = "download_url"
        private const val EXTRA_DOWNLOAD_USER_AGENT = "download_user_agent"
        private const val EXTRA_DOWNLOAD_CONTENT_DISPOSITION = "download_content_disposition"
        private const val EXTRA_DOWNLOAD_MIME_TYPE = "download_mime_type"
        private const val EXTRA_DOWNLOAD_CONTENT_LENGTH = "download_content_length"
        private const val EXTRA_DOWNLOAD_RESUME_FROM_EXISTING = "download_resume_from_existing"
    }
    
    /**
     * 处理待下载任务
     */
    fun handlePendingDownload(intent: Intent) {
        val url = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return
        
        // 检查是否有完整的下载请求信息
        val userAgent = intent.getStringExtra(EXTRA_DOWNLOAD_USER_AGENT)
        val contentDisposition = intent.getStringExtra(EXTRA_DOWNLOAD_CONTENT_DISPOSITION)
        val mimeType = intent.getStringExtra(EXTRA_DOWNLOAD_MIME_TYPE)
        val contentLength = intent.getLongExtra(EXTRA_DOWNLOAD_CONTENT_LENGTH, -1)
        val resumeFromExisting = intent.getBooleanExtra(EXTRA_DOWNLOAD_RESUME_FROM_EXISTING, true)
        
        // 如果有完整的下载请求信息，使用完整版本
        if (userAgent != null || contentDisposition != null || mimeType != null || contentLength > 0) {
            val request = WebViewDownloadInterceptor.DownloadRequest(
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                contentLength = contentLength
            )
            startDownload(request)
        } else {
            // 否则使用简化版本（仅 URL）
            startDownload(url, resumeFromExisting)
        }
    }
    
    /**
     * 开始下载（使用完整的下载请求）
     */
    fun startDownload(request: WebViewDownloadInterceptor.DownloadRequest) {
        DownloadManager.handleDownload(
            url = request.url,
            userAgent = request.userAgent,
            contentDisposition = request.contentDisposition,
            mimeType = request.mimeType,
            contentLength = request.contentLength,
            resumeFromExisting = true
        )
    }
    
    /**
     * 开始下载（仅使用 URL，简化版本）
     * 文件名将从 URL 中自动提取，如果无法提取则使用默认文件名
     * 
     * @param url 下载链接
     * @param resumeFromExisting 是否支持断点续传，默认为 true
     */
    fun startDownload(url: String, resumeFromExisting: Boolean = true) {
        DownloadManager.handleDownload(
            url = url,
            userAgent = null,
            contentDisposition = null,
            mimeType = null,
            contentLength = -1,
            resumeFromExisting = resumeFromExisting
        )
    }
    
    /**
     * 暂停下载
     */
    fun pauseDownload(task: DownloadTask) {
        DownloadManager.pauseDownloadTask(task.url)
        showMessage("已暂停下载")
    }
    
    /**
     * 继续下载
     */
    fun resumeDownload(task: DownloadTask) {
        val file = File(task.filePath)
        if (!file.exists() || file.length() == 0L) {
            showMessage("文件不存在，无法继续下载")
            return
        }
        DownloadManager.resumeDownloadTask(task.url)
        showMessage("开始继续下载: ${task.fileName}")
    }
    
    /**
     * 重新下载
     */
    fun restartDownload(task: DownloadTask) {
        _confirmAction.value = ConfirmAction.RestartDownload(task)
    }
    
    /**
     * 确认重新下载
     */
    fun confirmRestartDownload(task: DownloadTask) {
        DownloadManager.restartDownloadTask(task.url)
        showMessage("开始重新下载: ${task.fileName}")
        _confirmAction.value = null
    }
    
    /**
     * 取消下载
     */
    fun cancelDownload(task: DownloadTask) {
        _confirmAction.value = ConfirmAction.CancelDownload(task)
    }
    
    /**
     * 确认取消下载
     */
    fun confirmCancelDownload(task: DownloadTask) {
        DownloadManager.cancelDownloadTask(task.url)
        DownloadManager.notifyDataChanged()
        showMessage("已取消下载")
        _confirmAction.value = null
    }
    
    /**
     * 打开文件
     */
    fun openFile(task: DownloadTask) {
        val file = File(task.filePath)
        if (!file.exists()) {
            showMessage("文件不存在")
            return
        }
        _openFile.value = file
    }
    
    /**
     * 文件已打开，清除状态
     */
    fun onFileOpened() {
        _openFile.value = null
    }
    
    /**
     * 删除文件
     */
    fun deleteFile(task: DownloadTask) {
        _confirmAction.value = ConfirmAction.DeleteFile(task)
    }
    
    /**
     * 确认删除文件
     */
    fun confirmDeleteFile(task: DownloadTask) {
        val file = File(task.filePath)
        if (file.exists()) {
            file.delete()
        }
        DownloadManager.removeDownloadTask(task.url)
        showMessage("已删除")
        _confirmAction.value = null
    }
    
    /**
     * 清理已完成的任务
     */
    fun clearCompletedTasks() {
        DownloadManager.clearCompletedTasks()
        showMessage("已清理已完成的任务")
    }
    
    /**
     * 取消所有下载
     */
    fun cancelAllDownloads() {
        _confirmAction.value = ConfirmAction.CancelAllDownloads
    }
    
    /**
     * 确认取消所有下载
     */
    fun confirmCancelAllDownloads() {
        DownloadManager.cancelAllDownloads()
        showMessage("已取消所有下载任务")
        _confirmAction.value = null
    }
    
    /**
     * 显示消息
     */
    private fun showMessage(message: String) {
        _message.value = message
    }
    
    /**
     * 消息已显示，清除状态
     */
    fun onMessageShown() {
        _message.value = null
    }
    
    /**
     * 确认操作已处理
     */
    fun onConfirmActionHandled() {
        _confirmAction.value = null
    }
    
    /**
     * 获取文件的 MIME 类型
     */
    fun getMimeType(filePath: String): String {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "apk" -> "application/vnd.android.package-archive"
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            else -> "*/*"
        }
    }
    
    /**
     * 创建打开文件的 Intent
     */
    fun createOpenFileIntent(file: File): Intent? {
        return try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
            
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file.absolutePath))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 确认操作类型
     */
    sealed class ConfirmAction {
        data class RestartDownload(val task: DownloadTask) : ConfirmAction()
        data class CancelDownload(val task: DownloadTask) : ConfirmAction()
        data class DeleteFile(val task: DownloadTask) : ConfirmAction()
        object CancelAllDownloads : ConfirmAction()
    }
}

