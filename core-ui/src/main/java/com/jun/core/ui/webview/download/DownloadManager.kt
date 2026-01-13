package com.jun.core.ui.webview.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jun.core.network.client.NetworkClient
import com.jun.core.network.download.DownloadConfig
import com.jun.core.network.download.DownloadState
import com.jun.core.network.download.FileDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference
import java.net.URLDecoder
import androidx.core.net.toUri

/**
 * 下载任务信息
 */
data class DownloadTask(
    val id: String, // 下载任务ID（通常使用URL）
    val url: String,
    val fileName: String,
    val filePath: String,
    var totalBytes: Long = -1,
    var downloadedBytes: Long = 0,
    var progress: Int = 0,
    var speed: Long = 0, // 字节/秒
    var state: DownloadTaskState = DownloadTaskState.Preparing,
    var error: String? = null,
    // 用于继续下载和重新下载的元数据
    var userAgent: String? = null,
    var contentDisposition: String? = null,
    var mimeType: String? = null,
    var contentLength: Long = -1,
) {
    /**
     * 下载任务状态
     */
    enum class DownloadTaskState {
        Preparing,    // 准备中
        Downloading,  // 下载中
        Paused,       // 已暂停
        Completed,    // 已完成
        Failed,       // 失败
        Cancelled     // 已取消
    }
}

/**
 * 下载管理器（全局单例）
 * 统一管理所有下载任务
 * 
 * 功能：
 * - 管理所有下载任务的状态
 * - 提供下载任务列表的 LiveData
 * - 支持取消下载
 * - 支持暂停/恢复下载
 * - 持久化任务信息（进程被杀死后恢复）
 * - 支持并发下载数量限制
 * - 支持自动清理已完成的任务（可选）
 */
object DownloadManager {
    
    private val downloadTasks = mutableMapOf<String, DownloadTask>()
    private val downloadJobs = mutableMapOf<String, Job>()
    
    private val _downloadTasksLiveData = MutableLiveData<List<DownloadTask>>()
    val downloadTasksLiveData: LiveData<List<DownloadTask>> = _downloadTasksLiveData
    
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * Context 引用（用于持久化任务信息）
     * 使用 WeakReference 避免内存泄漏
     * 需要在初始化时设置
     */
    private var contextRef: WeakReference<Context>? = null
    
    /**
     * 获取 Context（如果还存在）
     */
    private fun getContext(): Context? = contextRef?.get()
    
    /**
     * 最大并发下载数量（默认无限制）
     */
    private var maxConcurrentDownloads: Int = Int.MAX_VALUE
    
    /**
     * 是否自动清理已完成的任务（默认 false）
     */
    private var autoCleanCompletedTasks: Boolean = false
    
    /**
     * NetworkClient 引用（用于创建 FileDownloader）
     * 使用 WeakReference 避免内存泄漏
     */
    private var networkClientRef: WeakReference<NetworkClient>? = null
    
    /**
     * 获取 NetworkClient（如果还存在）
     */
    private fun getNetworkClient(): NetworkClient? = networkClientRef?.get()
    
    /**
     * FileDownloader 实例（延迟初始化）
     */
    private val fileDownloader: FileDownloader?
        get() = getNetworkClient()?.let { FileDownloader(it) }
    
    /**
     * 下载协程作用域
     */
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * 初始化 Context 和 NetworkClient（用于持久化任务信息和执行下载）
     * @param context Context 实例
     * @param networkClient NetworkClient 实例（用于创建 FileDownloader）
     * @param maxConcurrentDownloads 最大并发下载数量，默认无限制
     * @param autoCleanCompletedTasks 是否自动清理已完成的任务，默认 false
     */
    fun init(
        context: Context,
        networkClient: NetworkClient,
        maxConcurrentDownloads: Int = Int.MAX_VALUE,
        autoCleanCompletedTasks: Boolean = false,
    ) {
        // 使用 WeakReference 存储 ApplicationContext，避免内存泄漏
        this.contextRef = WeakReference(context.applicationContext)
        this.networkClientRef = WeakReference(networkClient)
        this.maxConcurrentDownloads = maxConcurrentDownloads
        this.autoCleanCompletedTasks = autoCleanCompletedTasks
    }
    
    /**
     * 注册下载任务
     */
    fun registerDownloadTask(
        url: String,
        fileName: String,
        filePath: String,
        totalBytes: Long = -1,
        downloadJob: Job,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        contentLength: Long = -1,
        skipLiveDataUpdate: Boolean = false,
    ): DownloadTask {
        // 检查并发下载数量限制
        val activeCount = getActiveDownloadTasks().size
        if (activeCount >= maxConcurrentDownloads) {
            // 如果达到最大并发数，取消最旧的任务
            val oldestTask = downloadTasks.values
                .filter { it.state == DownloadTask.DownloadTaskState.Downloading || 
                         it.state == DownloadTask.DownloadTaskState.Preparing }
                .minByOrNull { it.id }
            oldestTask?.let {
                cancelDownloadTask(it.url)
            }
        }
        
        // 检查任务是否已存在（继续下载时）
        val existingTask = downloadTasks[url]
        val task = if (existingTask != null && existingTask.state == DownloadTask.DownloadTaskState.Preparing) {
            // 继续下载：保留已有的进度和已下载字节数
            // 只更新可变的字段，保留进度相关字段（progress, downloadedBytes）
            if (totalBytes > 0) existingTask.totalBytes = totalBytes
            if (userAgent != null) existingTask.userAgent = userAgent
            if (contentDisposition != null) existingTask.contentDisposition = contentDisposition
            if (mimeType != null) existingTask.mimeType = mimeType
            if (contentLength > 0) existingTask.contentLength = contentLength
            existingTask
        } else {
            // 新下载：创建新任务
            DownloadTask(
                id = url,
                url = url,
                fileName = fileName,
                filePath = filePath,
                totalBytes = totalBytes,
                state = DownloadTask.DownloadTaskState.Preparing,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                contentLength = contentLength
            )
        }
        
        downloadTasks[url] = task
        downloadJobs[url] = downloadJob
        
        // 保存任务信息到 SharedPreferences（用于进程被杀死后恢复）
        getContext()?.let { saveTaskToPreferences(it, task) }
        
        // 只有在不跳过更新时才立即更新 LiveData
        // 对于继续下载，等待下载状态更新时再更新 UI，避免显示空进度
        if (!skipLiveDataUpdate) {
            updateLiveData()
        }
        
        return task
    }
    
    /**
     * 更新下载任务状态
     * @param updateLiveDataImmediately 是否立即更新 LiveData，默认为 true
     *                                  对于频繁的进度更新，可以设置为 false，然后手动批量更新
     */
    fun updateDownloadTask(
        url: String,
        state: DownloadTask.DownloadTaskState? = null,
        progress: Int? = null,
        downloadedBytes: Long? = null,
        totalBytes: Long? = null,
        speed: Long? = null,
        error: String? = null,
        updateLiveDataImmediately: Boolean = true,
    ) {
        
        downloadTasks[url]?.let { task ->
            var changed = false
            
            state?.let { 
                if (task.state != it) {
                    task.state = it
                    changed = true
                }
            }
            progress?.let { 
                if (task.progress != it) {
                    task.progress = it
                    changed = true
                }
            }
            downloadedBytes?.let { 
                if (task.downloadedBytes != it) {
                    task.downloadedBytes = it
                    changed = true
                }
            }
            totalBytes?.let { 
                if (task.totalBytes != it) {
                    task.totalBytes = it
                    changed = true
                }
            }
            speed?.let { 
                if (task.speed != it) {
                    task.speed = it
                    changed = true
                }
            }
            error?.let { 
                if (task.error != it) {
                    task.error = it
                    changed = true
                }
            }
            
            // 对于下载中的任务，即使数据没变化也更新（确保速度等信息及时刷新）
            // 其他状态只在数据真正改变时更新
            val isDownloading = state == DownloadTask.DownloadTaskState.Downloading || 
                    task.state == DownloadTask.DownloadTaskState.Downloading
            val shouldUpdate = if (isDownloading) {
                // 下载中：只要有进度或速度更新就刷新
                (progress != null || speed != null) || changed
            } else {
                // 其他状态：只在数据真正改变时更新
                changed
            }
            
            if (shouldUpdate && updateLiveDataImmediately) {
                updateLiveData()
            }
            
            // 保存任务信息到 SharedPreferences（用于进程被杀死后恢复）
            // 只在状态、总大小、错误信息等关键信息变化时保存
            if (changed && (state != null || totalBytes != null || error != null)) {
                getContext()?.let { saveTaskToPreferences(it, task) }
            }
        }
    }
    
    /**
     * 手动触发 LiveData 更新（用于批量更新后）
     */
    fun notifyDataChanged() {
        updateLiveData()
    }
    
    /**
     * 完成下载任务
     */
    fun completeDownloadTask(url: String, file: File) {
        downloadTasks[url]?.let { task ->
            task.state = DownloadTask.DownloadTaskState.Completed
            task.progress = 100
            task.downloadedBytes = file.length()
            task.totalBytes = file.length()
            
            // 保存任务信息到 SharedPreferences（用于进程被杀死后恢复）
            getContext()?.let { saveTaskToPreferences(it, task) }
            
            updateLiveData()
            
            // 如果启用了自动清理，延迟清理已完成的任务
            if (autoCleanCompletedTasks) {
                managerScope.launch {
                    delay(5000) // 延迟 5 秒后清理
                    if (task.state == DownloadTask.DownloadTaskState.Completed) {
                        removeDownloadTask(url)
                    }
                }
            }
        }
        downloadJobs.remove(url)
    }
    
    /**
     * 失败下载任务
     */
    fun failDownloadTask(url: String, error: String) {
        downloadTasks[url]?.let { task ->
            task.state = DownloadTask.DownloadTaskState.Failed
            task.error = error
            
            // 保存任务信息到 SharedPreferences（用于进程被杀死后恢复）
            getContext()?.let { saveTaskToPreferences(it, task) }
            
            updateLiveData()
        }
        downloadJobs.remove(url)
    }
    
    /**
     * 暂停下载任务
     */
    fun pauseDownloadTask(url: String) {
        val task = downloadTasks[url]
        if (task == null) {
            return
        }
        
        if (task.state != DownloadTask.DownloadTaskState.Downloading && 
            task.state != DownloadTask.DownloadTaskState.Preparing) {
            return
        }
        
        // 先更新状态为暂停（在取消 Job 之前，确保状态先更新）
        // 这样当 Flow 发出 Cancelled 时，handleDownload 的 collect 中会检查到状态已经是 Paused，就不会覆盖
        task.state = DownloadTask.DownloadTaskState.Paused
        // 更新已下载字节数为实际文件大小（如果文件存在）
        val file = java.io.File(task.filePath)
        if (file.exists()) {
            task.downloadedBytes = file.length()
            // 重新计算进度
            if (task.totalBytes > 0) {
                task.progress = ((task.downloadedBytes * 100) / task.totalBytes).toInt().coerceIn(0, 100)
            }
        }
        
        // 保存任务信息到 SharedPreferences（用于进程被杀死后恢复）
        getContext()?.let { saveTaskToPreferences(it, task) }
        
        updateLiveData()
        
        // 然后取消下载 Job（这会停止下载，但文件已部分下载）
        // 注意：取消 Job 会导致 Flow 发出 Cancelled，但我们已经先设置了 Paused 状态
        downloadJobs[url]?.cancel()
        downloadJobs.remove(url)
        
    }
    
    /**
     * 取消下载任务
     */
    fun cancelDownloadTask(url: String) {
        downloadJobs[url]?.cancel()
        downloadJobs.remove(url)
        downloadTasks[url]?.let { task ->
            task.state = DownloadTask.DownloadTaskState.Cancelled
            
            // 保存任务信息到 SharedPreferences（用于进程被杀死后恢复）
            getContext()?.let { saveTaskToPreferences(it, task) }
            
            updateLiveData()
        }
    }
    
    /**
     * 取消所有下载任务
     */
    fun cancelAllDownloads() {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        downloadTasks.values.forEach { task ->
            if (task.state == DownloadTask.DownloadTaskState.Downloading || 
                task.state == DownloadTask.DownloadTaskState.Preparing) {
                task.state = DownloadTask.DownloadTaskState.Cancelled
            }
        }
        updateLiveData()
    }
    
    /**
     * 获取下载任务
     */
    fun getDownloadTask(url: String): DownloadTask? = downloadTasks[url]
    
    /**
     * 获取所有下载任务
     */
    fun getAllDownloadTasks(): List<DownloadTask> = downloadTasks.values.toList()
    
    /**
     * 获取正在下载的任务
     */
    fun getActiveDownloadTasks(): List<DownloadTask> = 
        downloadTasks.values.filter { 
            it.state == DownloadTask.DownloadTaskState.Downloading || 
            it.state == DownloadTask.DownloadTaskState.Preparing 
        }
    
    /**
     * 移除下载任务（已完成、失败、取消的任务）
     */
    fun removeDownloadTask(url: String) {
        val task = downloadTasks[url]
        downloadTasks.remove(url)
        downloadJobs.remove(url)
        
        // 从 SharedPreferences 删除任务信息
        task?.let { getContext()?.let { ctx -> removeTaskFromPreferences(ctx, it.filePath) } }
        
        updateLiveData()
    }
    
    /**
     * 清理所有已完成/失败/取消的任务
     */
    fun clearCompletedTasks() {
        val toRemove = downloadTasks.values.filter { task ->
            task.state == DownloadTask.DownloadTaskState.Completed ||
            task.state == DownloadTask.DownloadTaskState.Failed ||
            task.state == DownloadTask.DownloadTaskState.Cancelled
        }.map { it.id }
        
        toRemove.forEach { url ->
            downloadTasks.remove(url)
            downloadJobs.remove(url)
        }
        
        if (toRemove.isNotEmpty()) {
            updateLiveData()
        }
    }

    /**
     * 从本地下载目录中加载已完成的下载任务（用于下载管理界面初次进入时读取"缓存数据"）
     *
     * 说明：
     * - 扫描本地文件，根据文件大小判断是否完整
     * - 如果内存中已经有对应 filePath 的任务，则不会重复添加
     * - 对于这些从本地恢复的任务，URL 使用 filePath 作为唯一标识，主要用于"打开 / 删除"操作
     * - 文件大小会同时记录到 totalBytes 和 downloadedBytes，用于判断完整性
     * 
     * @param downloadDir 下载目录
     * @param context Context，用于从 SharedPreferences 恢复任务信息（可选）
     */
    fun loadCachedCompletedTasks(downloadDir: File?, context: Context? = null) {
        if (downloadDir == null || !downloadDir.exists() || !downloadDir.isDirectory) {
            return
        }

        // 以 filePath 为 key 建立索引，避免重复添加
        val existingByPath = downloadTasks.values.associateBy { it.filePath }

        val files = downloadDir.listFiles() ?: emptyArray()
        if (files.isEmpty()) {
            return
        }

        // 从 SharedPreferences 恢复任务信息（如果 Context 已初始化或提供了 Context）
        val ctx = context ?: getContext()
        val savedTasks = if (ctx != null) {
            loadSavedTasksFromPreferences(ctx)
        } else {
            emptyMap<String, SavedTaskInfo>()
        }
        
        var addedCount = 0
        files.filter { it.isFile }.forEach { file ->
            val path = file.absolutePath
            // 检查是否已经有对应这个文件路径的任务（通过 filePath 匹配）
            if (existingByPath.containsKey(path)) {
                return@forEach
            }
            
            // 检查是否已经有对应这个文件路径的任务（通过 URL 匹配，因为历史任务可能使用 filePath 作为 URL）
            val existingByUrl = downloadTasks.values.firstOrNull { it.filePath == path }
            if (existingByUrl != null) {
                return@forEach
            }

            val actualFileSize = file.length()
            
            // 尝试从保存的任务信息中恢复
            val savedTask = savedTasks[path]
            val task = if (savedTask != null) {
                // 从持久化存储中恢复任务信息
                DownloadTask(
                    id = savedTask.url,
                    url = savedTask.url, // 使用保存的真实 URL
                    fileName = file.name,
                    filePath = path,
                    totalBytes = savedTask.totalBytes, // 使用保存的总大小
                    downloadedBytes = actualFileSize, // 使用实际文件大小
                    progress = if (savedTask.totalBytes > 0) {
                        ((actualFileSize * 100) / savedTask.totalBytes).toInt().coerceIn(0, 100)
                    } else {
                        0
                    },
                    speed = 0,
                    state = savedTask.state, // 使用保存的状态
                    error = savedTask.error,
                    userAgent = savedTask.userAgent,
                    contentDisposition = savedTask.contentDisposition,
                    mimeType = savedTask.mimeType,
                    contentLength = savedTask.contentLength
                )
            } else {
                // 没有保存的任务信息，当作历史任务处理
                // 对于历史任务，使用特殊的 URL 格式：file://<filePath>
                val idAndUrl = "file://$path"
                DownloadTask(
                    id = idAndUrl,
                    url = idAndUrl, // 使用 file:// 前缀，表示这是历史任务
                    fileName = file.name,
                    filePath = path,
                    totalBytes = -1, // 未知总大小（历史任务无法获取服务器总大小）
                    downloadedBytes = actualFileSize, // 实际文件大小
                    progress = 0, // 进度未知，UI 会根据实际文件大小和总大小计算
                    speed = 0,
                    state = DownloadTask.DownloadTaskState.Failed, // 设置为 Failed，让 UI 根据文件完整性判断
                    error = null // 没有错误信息，UI 会根据文件完整性显示不同的状态
                )
            }

            downloadTasks[task.url] = task
            addedCount++
        }

        if (addedCount > 0) {
            updateLiveData()
        }
    }
    
    /**
     * 更新 LiveData
     * 每次创建新的列表对象，确保 DiffUtil 能检测到变化
     */
    private fun updateLiveData() {
        // 创建新的列表对象（使用 ArrayList 确保每次都是新对象）
        // 这样即使列表内容相同，引用也不同，DiffUtil 会进行完整的比较
        val newList = ArrayList(downloadTasks.values)
        _downloadTasksLiveData.postValue(newList)
    }
    
    /**
     * 获取下载目录
     */
    private fun getDownloadDirectory(context: Context): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir.resolve("downloads").apply { mkdirs() }
    }
    
    /**
     * 从 URL 中提取文件名
     */
    private fun extractFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        // 尝试从 Content-Disposition 中提取文件名
        contentDisposition?.let {
            val fileNameRegex = Regex("filename[*]?=(?:UTF-8['\"]?)?([^;\\r\\n]+)", RegexOption.IGNORE_CASE)
            fileNameRegex.find(it)?.groupValues?.get(1)?.trim()?.let { name ->
                return URLDecoder.decode(name.trim('"', '\''), "UTF-8")
            }
        }
        
        // 从 URL 中提取文件名
        try {
            val path = url.toUri().path
            if (path != null) {
                val fileName = path.substringAfterLast('/')
                if (fileName.isNotEmpty() && fileName.contains('.')) {
                    return fileName
                }
            }
        } catch (e: Exception) {
        }
        
        // 根据 MIME 类型生成默认文件名
        val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "bin"
        return "download_${System.currentTimeMillis()}.$extension"
    }
    
    /**
     * 处理下载请求
     * @param resumeFromExisting 是否从已存在的文件继续下载（断点续传），默认为 true
     */
    fun handleDownload(
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        contentLength: Long = -1,
        resumeFromExisting: Boolean = true,
    ) {
        val context = getContext() ?: return
        val downloader = fileDownloader ?: return
        
        // 检查本地是否已有正在进行的下载
        if (downloadJobs.containsKey(url)) {
            val job = downloadJobs[url]
            if (job != null && job.isActive) {
                // 如果是要继续下载，清理残留的 Job 并继续
                if (resumeFromExisting) {
                    job.cancel()
                    downloadJobs.remove(url)
                } else {
                    return
                }
            } else {
                // Job 已取消或完成，清理
                downloadJobs.remove(url)
            }
        }
        
        val fileName = extractFileName(url, contentDisposition, mimeType)
        val downloadDir = getDownloadDirectory(context)
        
        // 检查是否已有未完成的任务，如果有且支持断点续传，使用原文件路径
        val existingTask = downloadTasks[url]
        val finalFile = if (existingTask != null &&
            resumeFromExisting &&
            File(existingTask.filePath).exists()
        ) {
            // 使用已存在的文件路径（断点续传）
            File(existingTask.filePath)
        } else {
            // 新下载或重新下载，生成新文件路径
            val file = File(downloadDir, fileName)
            var finalFile = file
            var counter = 1
            while (finalFile.exists() && !resumeFromExisting) {
                val nameWithoutExt = fileName.substringBeforeLast('.')
                val ext = fileName.substringAfterLast('.', "")
                finalFile = File(downloadDir, "${nameWithoutExt}_$counter.$ext")
                counter++
            }
            finalFile
        }
        
        // 启动下载任务
        val downloadJob = downloadScope.launch {
            try {
                val requestConfig = com.jun.core.network.client.requestConfig {
                    userAgent?.let { header("User-Agent", it) }
                }
                
                val downloadConfig = DownloadConfig.DEFAULT.copy(
                    resumeFromExisting = resumeFromExisting
                )
                
                // 注册到全局管理器（使用当前的 Job）
                val currentJob = coroutineContext[Job]
                if (currentJob != null) {
                    val task = registerDownloadTask(
                        url = url,
                        fileName = finalFile.name,
                        filePath = finalFile.absolutePath,
                        totalBytes = contentLength,
                        downloadJob = currentJob,
                        userAgent = userAgent,
                        contentDisposition = contentDisposition,
                        mimeType = mimeType,
                        contentLength = contentLength,
                        skipLiveDataUpdate = true
                    )
                    
                    // 注册完成后，立即更新 UI（确保继续下载时进度正确显示）
                    withContext(Dispatchers.Main) {
                        updateDownloadTask(
                            url = url,
                            state = DownloadTask.DownloadTaskState.Preparing,
                            progress = task.progress,
                            downloadedBytes = task.downloadedBytes,
                            totalBytes = task.totalBytes,
                            updateLiveDataImmediately = true
                        )
                    }
                }
                
                downloader.download(url, finalFile, requestConfig, downloadConfig)
                    .collect { state ->
                        when (state) {
                            is DownloadState.Downloading -> {
                                updateDownloadTask(
                                    url = url,
                                    state = DownloadTask.DownloadTaskState.Downloading,
                                    progress = state.progress,
                                    downloadedBytes = state.downloadedBytes,
                                    totalBytes = state.totalBytes,
                                    speed = state.speed,
                                    updateLiveDataImmediately = true
                                )
                            }
                            
                            is DownloadState.Completed -> {
                                downloadJobs.remove(url)
                                completeDownloadTask(url, state.file)
                            }
                            
                            is DownloadState.Failed -> {
                                downloadJobs.remove(url)
                                failDownloadTask(url, state.error)
                            }
                            
                            is DownloadState.Cancelled -> {
                                downloadJobs.remove(url)
                                // 检查任务当前状态，如果已经是 Paused，则保持 Paused 状态
                                val currentTask = getDownloadTask(url)
                                if (currentTask?.state != DownloadTask.DownloadTaskState.Paused) {
                                    updateDownloadTask(
                                        url = url,
                                        state = DownloadTask.DownloadTaskState.Cancelled
                                    )
                                }
                            }
                            
                            else -> {}
                        }
                    }
            } catch (e: Exception) {
                downloadJobs.remove(url)
                // 检查任务当前状态，如果已经是 Paused 或 Cancelled，则保持当前状态
                val currentTask = getDownloadTask(url)
                if (currentTask?.state != DownloadTask.DownloadTaskState.Paused &&
                    currentTask?.state != DownloadTask.DownloadTaskState.Cancelled) {
                    val errorMsg = e.message ?: "未知错误"
                    failDownloadTask(url, errorMsg)
                }
            }
        }
        
        downloadJobs[url] = downloadJob
    }
    
    /**
     * 继续下载任务（断点续传）
     * @param url 下载地址
     */
    fun resumeDownloadTask(url: String) {
        val task = downloadTasks[url] ?: return
        
        // 检查任务状态是否允许继续下载
        if (task.state != DownloadTask.DownloadTaskState.Failed && 
            task.state != DownloadTask.DownloadTaskState.Cancelled &&
            task.state != DownloadTask.DownloadTaskState.Paused) {
            return
        }
        
        // 检查文件是否存在
        val file = File(task.filePath)
        if (!file.exists()) {
            return
        }
        
        // 更新已下载字节数为实际文件大小（用于断点续传）
        val actualFileSize = file.length()
        if (actualFileSize > 0) {
            task.downloadedBytes = actualFileSize
            // 重新计算进度
            if (task.totalBytes > 0) {
                task.progress = ((task.downloadedBytes * 100) / task.totalBytes).toInt().coerceIn(0, 100)
            }
        }
        
        // 更新状态为准备中（在启动下载之前）
        task.state = DownloadTask.DownloadTaskState.Preparing
        updateLiveData()
        
        // 确保没有残留的下载 Job
        downloadJobs[url]?.cancel()
        downloadJobs.remove(url)
        
        // 重新启动下载（断点续传）
        handleDownload(
            url = task.url,
            userAgent = task.userAgent,
            contentDisposition = task.contentDisposition,
            mimeType = task.mimeType,
            contentLength = task.contentLength,
            resumeFromExisting = true
        )
    }
    
    /**
     * 重新下载任务（从头开始）
     * @param url 下载地址
     */
    fun restartDownloadTask(url: String) {
        val task = downloadTasks[url] ?: return
        
        // 删除已存在的文件（如果未完成）
        val file = File(task.filePath)
        if (file.exists() && task.state != DownloadTask.DownloadTaskState.Completed) {
            file.delete()
        }
        
        // 重置任务状态
        task.downloadedBytes = 0
        task.progress = 0
        task.speed = 0
        task.error = null
        
        // 更新状态为准备中
        task.state = DownloadTask.DownloadTaskState.Preparing
        updateLiveData()
        
        // 确保没有残留的下载 Job
        downloadJobs[url]?.cancel()
        downloadJobs.remove(url)
        
        // 重新启动下载（不使用断点续传）
        handleDownload(
            url = task.url,
            userAgent = task.userAgent,
            contentDisposition = task.contentDisposition,
            mimeType = task.mimeType,
            contentLength = task.contentLength,
            resumeFromExisting = false
        )
    }
    
    /**
     * 保存的任务信息（用于持久化）
     */
    private data class SavedTaskInfo(
        val url: String,
        val totalBytes: Long,
        val state: DownloadTask.DownloadTaskState,
        val error: String?,
        val userAgent: String?,
        val contentDisposition: String?,
        val mimeType: String?,
        val contentLength: Long,
    )
    
    /**
     * 从 SharedPreferences 加载保存的任务信息
     */
    private fun loadSavedTasksFromPreferences(context: Context): Map<String, SavedTaskInfo> {
        val prefs = context.getSharedPreferences("download_tasks", Context.MODE_PRIVATE)
        val tasksJson = prefs.getString("tasks", null) ?: return emptyMap()
        
        return try {
            // 简单的格式：每行一个任务，格式为：filePath|url|totalBytes|state|error|userAgent|contentDisposition|mimeType|contentLength
            val tasks = mutableMapOf<String, SavedTaskInfo>()
            tasksJson.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    val parts = line.split("|")
                    if (parts.size >= 3) {
                        val filePath = parts[0]
                        val url = parts[1]
                        val totalBytes = parts.getOrNull(2)?.toLongOrNull() ?: -1L
                        val stateStr = parts.getOrNull(3) ?: "Failed"
                        val state = try {
                            DownloadTask.DownloadTaskState.valueOf(stateStr)
                        } catch (e: Exception) {
                            DownloadTask.DownloadTaskState.Failed
                        }
                        val error = parts.getOrNull(4)?.takeIf { it.isNotBlank() }
                        val userAgent = parts.getOrNull(5)?.takeIf { it.isNotBlank() }
                        val contentDisposition = parts.getOrNull(6)?.takeIf { it.isNotBlank() }
                        val mimeType = parts.getOrNull(7)?.takeIf { it.isNotBlank() }
                        val contentLength = parts.getOrNull(8)?.toLongOrNull() ?: -1L
                        
                        tasks[filePath] = SavedTaskInfo(
                            url = url,
                            totalBytes = totalBytes,
                            state = state,
                            error = error,
                            userAgent = userAgent,
                            contentDisposition = contentDisposition,
                            mimeType = mimeType,
                            contentLength = contentLength
                        )
                    }
                }
            }
            tasks
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * 保存任务信息到 SharedPreferences（用于进程被杀死后恢复）
     * 使用 StringBuilder 提高字符串拼接性能
     */
    private fun saveTaskToPreferences(context: Context, task: DownloadTask) {
        try {
            val prefs = context.getSharedPreferences("download_tasks", Context.MODE_PRIVATE)
            val existingTasks = loadSavedTasksFromPreferences(context).toMutableMap()
            
            // 更新或添加任务信息
            existingTasks[task.filePath] = SavedTaskInfo(
                url = task.url,
                totalBytes = task.totalBytes,
                state = task.state,
                error = task.error,
                userAgent = task.userAgent,
                contentDisposition = task.contentDisposition,
                mimeType = task.mimeType,
                contentLength = task.contentLength
            )
            
            // 使用 StringBuilder 提高性能
            val sb = StringBuilder()
            existingTasks.entries.forEachIndexed { index, (filePath, info) ->
                if (index > 0) sb.append("\n")
                sb.append(filePath)
                    .append("|").append(info.url)
                    .append("|").append(info.totalBytes)
                    .append("|").append(info.state)
                    .append("|").append(info.error ?: "")
                    .append("|").append(info.userAgent ?: "")
                    .append("|").append(info.contentDisposition ?: "")
                    .append("|").append(info.mimeType ?: "")
                    .append("|").append(info.contentLength)
            }
            
            prefs.edit().putString("tasks", sb.toString()).apply()
        } catch (e: Exception) {
            // 保存失败，静默处理
        }
    }
    
    /**
     * 从 SharedPreferences 删除任务信息
     * 使用 StringBuilder 提高字符串拼接性能
     */
    private fun removeTaskFromPreferences(context: Context, filePath: String) {
        try {
            val prefs = context.getSharedPreferences("download_tasks", Context.MODE_PRIVATE)
            val existingTasks = loadSavedTasksFromPreferences(context).toMutableMap()
            existingTasks.remove(filePath)
            
            // 使用 StringBuilder 提高性能
            val sb = StringBuilder()
            existingTasks.entries.forEachIndexed { index, (path, info) ->
                if (index > 0) sb.append("\n")
                sb.append(path)
                    .append("|").append(info.url)
                    .append("|").append(info.totalBytes)
                    .append("|").append(info.state)
                    .append("|").append(info.error ?: "")
                    .append("|").append(info.userAgent ?: "")
                    .append("|").append(info.contentDisposition ?: "")
                    .append("|").append(info.mimeType ?: "")
                    .append("|").append(info.contentLength)
            }
            
            prefs.edit().putString("tasks", sb.toString()).apply()
        } catch (e: Exception) {
            // 删除失败，静默处理
        }
    }
    
    /**
     * 清理资源（应用退出时调用）
     */
    fun destroy() {
        cancelAllDownloads()
        downloadTasks.clear()
        downloadJobs.clear()
        managerScope.cancel()
        downloadScope.cancel()
    }
}

