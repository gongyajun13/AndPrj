package com.jun.andprj.ui.tool

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jun.andprj.R
import com.jun.andprj.databinding.ActivityDownloadManagerBinding
import com.jun.core.ui.base.BaseActivity
import com.jun.core.ui.extension.showConfirmDialog
import com.jun.core.ui.webview.download.DownloadManager
import com.jun.core.ui.webview.download.WebViewDownloadInterceptor
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

/**
 * 下载管理 Activity
 * 显示所有 WebView 下载任务，支持暂停、取消、打开、删除、继续下载、重新下载等操作
 */
@AndroidEntryPoint
class DownloadManagerActivity : BaseActivity<ActivityDownloadManagerBinding>() {
    
    private val viewModel: DownloadManagerViewModel by viewModels()
    
    override fun createBinding(): ActivityDownloadManagerBinding =
        ActivityDownloadManagerBinding.inflate(layoutInflater)
    
    private val adapter by lazy {
        DownloadTaskAdapter(
            onPauseClick = { viewModel.pauseDownload(it) },
            onResumeClick = { viewModel.resumeDownload(it) },
            onRestartClick = { viewModel.restartDownload(it) },
            onCancelClick = { viewModel.cancelDownload(it) },
            onOpenClick = { viewModel.openFile(it) },
            onDeleteClick = { viewModel.deleteFile(it) }
        )
    }
    
    override fun setupViews() {
        super.setupViews()
        
        setupToolbar()
        setupRecyclerView()
        setupActions()
        observeViewModel()
        
        // 检查是否有待处理的下载任务（从 Intent 传入）
        viewModel.handlePendingDownload(intent)
    }
    
    private fun observeViewModel() {
        // 观察下载任务列表
        DownloadManager.downloadTasksLiveData.observe(this) { tasks ->
            viewModel.updateDownloadTasks(tasks)
        }
        
        // 观察 ViewModel 的下载任务列表
        viewModel.downloadTasks.collectOnLifecycle { tasks ->
            adapter.submitList(tasks) {}
            updateEmptyState(tasks.isEmpty())
        }
        
        // 观察空状态
        viewModel.isEmpty.collectOnLifecycle { isEmpty ->
            updateEmptyState(isEmpty)
        }
        
        // 观察消息
        viewModel.message.collectOnLifecycle { message ->
            message?.let {
                showMessage(it)
                viewModel.onMessageShown()
            }
        }
        
        // 观察确认操作
        viewModel.confirmAction.collectOnLifecycle { action ->
            if (action != null) {
                handleConfirmAction(action)
            } else {
                // 确认操作已处理，清除对话框（如果需要）
            }
        }
        
        // 观察打开文件
        viewModel.openFile.collectOnLifecycle { file ->
            file?.let {
                openFile(it)
                viewModel.onFileOpened()
            }
        }
    }
    
    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        binding.toolbar.setupSimple(
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            title = "下载管理",
            titleTextColor = white,
            backgroundColor = ContextCompat.getColor(this, R.color.blue),
            onLeftClick = { finish() }
        )
        
        setStatusBarColor(white, lightIcons = false)
    }
    
    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupActions() {
        binding.btnClearCompleted.setOnClickListener {
            viewModel.clearCompletedTasks()
        }
        
        binding.btnCancelAll.setOnClickListener {
            viewModel.cancelAllDownloads()
        }
    }
    
    /**
     * 处理确认操作
     */
    private fun handleConfirmAction(action: DownloadManagerViewModel.ConfirmAction) {
        when (action) {
            is DownloadManagerViewModel.ConfirmAction.RestartDownload -> {
                showConfirmDialog(
                    title = "确认重新下载",
                    message = "确定要重新下载 \"${action.task.fileName}\" 吗？已下载的部分将被删除。",
                    onConfirm = {
                        viewModel.confirmRestartDownload(action.task)
                    },
                    onCancel = {
                        viewModel.onConfirmActionHandled()
                    }
                )
            }
            is DownloadManagerViewModel.ConfirmAction.CancelDownload -> {
                showConfirmDialog(
                    title = "确认取消",
                    message = "确定要取消下载 \"${action.task.fileName}\" 吗？",
                    onConfirm = {
                        viewModel.confirmCancelDownload(action.task)
                    },
                    onCancel = {
                        viewModel.onConfirmActionHandled()
                    }
                )
            }
            is DownloadManagerViewModel.ConfirmAction.DeleteFile -> {
                showConfirmDialog(
                    title = "确认删除",
                    message = "确定要删除 \"${action.task.fileName}\" 吗？",
                    onConfirm = {
                        viewModel.confirmDeleteFile(action.task)
                    },
                    onCancel = {
                        viewModel.onConfirmActionHandled()
                    }
                )
            }
            is DownloadManagerViewModel.ConfirmAction.CancelAllDownloads -> {
                showConfirmDialog(
                    title = "确认取消",
                    message = "确定要取消所有正在下载的任务吗？",
                    onConfirm = {
                        viewModel.confirmCancelAllDownloads()
                    },
                    onCancel = {
                        viewModel.onConfirmActionHandled()
                    }
                )
            }
        }
    }
    
    /**
     * 打开文件
     */
    private fun openFile(file: File) {
        val intent = viewModel.createOpenFileIntent(file)
        if (intent != null && intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            showMessage("无法打开此文件类型")
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    companion object {
        // Intent Extra Keys
        private const val EXTRA_DOWNLOAD_URL = "download_url"
        private const val EXTRA_DOWNLOAD_USER_AGENT = "download_user_agent"
        private const val EXTRA_DOWNLOAD_CONTENT_DISPOSITION = "download_content_disposition"
        private const val EXTRA_DOWNLOAD_MIME_TYPE = "download_mime_type"
        private const val EXTRA_DOWNLOAD_CONTENT_LENGTH = "download_content_length"
        private const val EXTRA_DOWNLOAD_RESUME_FROM_EXISTING = "download_resume_from_existing"
        
        /**
         * 启动下载管理界面
         */
        fun start(context: android.content.Context) {
            val intent = Intent(context, DownloadManagerActivity::class.java)
            context.startActivity(intent)
        }
        
        /**
         * 启动下载管理界面并开始下载（使用完整的下载请求）
         * 
         * @param context Context
         * @param request 下载请求（包含 URL、User-Agent、Content-Disposition 等信息）
         */
        fun startWithDownload(context: android.content.Context, request: WebViewDownloadInterceptor.DownloadRequest) {
            val intent = Intent(context, DownloadManagerActivity::class.java).apply {
                putExtra(EXTRA_DOWNLOAD_URL, request.url)
                putExtra(EXTRA_DOWNLOAD_USER_AGENT, request.userAgent)
                putExtra(EXTRA_DOWNLOAD_CONTENT_DISPOSITION, request.contentDisposition)
                putExtra(EXTRA_DOWNLOAD_MIME_TYPE, request.mimeType)
                putExtra(EXTRA_DOWNLOAD_CONTENT_LENGTH, request.contentLength)
            }
            context.startActivity(intent)
        }
        
        /**
         * 启动下载管理界面并开始下载（仅使用 URL，简化版本）
         * 文件名将从 URL 中自动提取，如果无法提取则使用默认文件名
         * 
         * @param context Context
         * @param url 下载链接
         * @param resumeFromExisting 是否支持断点续传，默认为 true
         */
        fun startWithUrl(
            context: android.content.Context,
            url: String,
            resumeFromExisting: Boolean = true
        ) {
            val intent = Intent(context, DownloadManagerActivity::class.java).apply {
                putExtra(EXTRA_DOWNLOAD_URL, url)
                putExtra(EXTRA_DOWNLOAD_RESUME_FROM_EXISTING, resumeFromExisting)
            }
            context.startActivity(intent)
        }
    }
}
