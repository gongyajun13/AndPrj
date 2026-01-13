package com.jun.andprj.ui.tool

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import com.jun.andprj.R
import com.jun.andprj.databinding.ActivityAppUpdateBinding
import com.jun.core.common.extension.formatFileSize
import com.jun.core.common.util.FileUtils
import com.jun.core.ui.base.BaseActivity
import com.jun.core.ui.extension.showConfirmDialog
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * App升级安装Activity
 * 
 * 重新设计的UI特点：
 * - 清晰的卡片式布局
 * - 状态信息集中显示
 * - 操作按钮固定在底部
 * - 更好的视觉层次
 * - 优化的交互体验
 */
@AndroidEntryPoint
class AppUpdateActivity : BaseActivity<ActivityAppUpdateBinding>() {

    private val viewModel: AppUpdateViewModel by viewModels()
    
    // 用于接收安装权限设置页面的返回结果
    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // 检查权限是否已授予
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.canRequestPackageInstalls()) {
                // 权限已授予，开始下载
                val downloadUrl = when (val state = viewModel.appUpdateState.value) {
                    is AppUpdateUiState.UpdateAvailable -> state.downloadUrl
                    else -> {
                        showError("请先检查更新")
                        return@registerForActivityResult
                    }
                }
                startDownload(downloadUrl)
            } else {
                showError("需要安装权限才能下载和安装应用")
            }
        }
    }

    override fun createBinding(): ActivityAppUpdateBinding =
        ActivityAppUpdateBinding.inflate(layoutInflater)

    override fun setupViews() {
        setupToolbar()
        updateVersionInfo()
        
        // 进入界面时自动检查缓存数据
        viewModel.checkLocalDownloadProgress()
    }

    override fun setupObservers() {
        // 观察 ViewModel 的 App 升级状态
        viewModel.appUpdateState.collectOnLifecycle { state ->
            handleUiState(state)
        }
    }

    override fun setupListeners() {
        binding.btnCheckUpdate.setOnClickListener {
            viewModel.checkUpdate()
        }

        binding.btnDownload.setOnClickListener {
            requestInstallPermissionAndDownload()
        }

        binding.btnInstall.setOnClickListener {
            installApk()
        }

        binding.btnPause.setOnClickListener {
            viewModel.pauseDownload()
        }

        binding.btnResume.setOnClickListener {
            val currentState = viewModel.appUpdateState.value
            when (currentState) {
                is AppUpdateUiState.UpdateAvailable -> {
                    viewModel.startDownload(currentState.downloadUrl, resumeFromExisting = true)
                }
                is AppUpdateUiState.IncompleteDownloadDetected -> {
                    if (currentState.downloadUrl != null) {
                        viewModel.startDownload(currentState.downloadUrl, resumeFromExisting = true)
                    } else {
                        showMessage("请先检查更新以获取下载地址")
                    }
                }
                else -> {
                    viewModel.resumeDownload()
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            viewModel.cancelDownload()
        }

        binding.btnRetry.setOnClickListener {
            val currentState = viewModel.appUpdateState.value
            when (currentState) {
                is AppUpdateUiState.IncompleteDownloadDetected -> {
                    val downloaded = currentState.downloadedBytes.formatFileSize()
                    val total = if (currentState.totalBytes > 0) {
                        currentState.totalBytes.formatFileSize()
                    } else {
                        "未知"
                    }
                    showConfirmDialog(
                        title = "重新下载",
                        message = "确定要删除已下载的文件并重新下载吗？\n\n已下载: $downloaded\n总大小: $total",
                        onConfirm = {
                            viewModel.deleteApkFile()
                            if (currentState.downloadUrl != null) {
                                viewModel.startDownload(currentState.downloadUrl, resumeFromExisting = false)
                            } else {
                                showMessage("请先检查更新以获取下载地址")
                            }
                        }
                    )
                }
                is AppUpdateUiState.DownloadFailed -> {
                    if (currentState.canRetry) {
                        val downloadUrl = when (val state = viewModel.appUpdateState.value) {
                            is AppUpdateUiState.UpdateAvailable -> state.downloadUrl
                            is AppUpdateUiState.IncompleteDownloadDetected -> state.downloadUrl
                            else -> null
                        }
                        if (downloadUrl != null) {
                            viewModel.startDownload(downloadUrl, resumeFromExisting = true)
                        } else {
                            showMessage("请先检查更新以获取下载地址")
                        }
                    }
                }
                else -> {
                    val downloadUrl = when (currentState) {
                        is AppUpdateUiState.UpdateAvailable -> currentState.downloadUrl
                        is AppUpdateUiState.IncompleteDownloadDetected -> currentState.downloadUrl
                        else -> null
                    }
                    if (downloadUrl != null) {
                        showConfirmDialog(
                            title = "重新下载",
                            message = "确定要重新下载吗？",
                            onConfirm = {
                                viewModel.deleteApkFile()
                                viewModel.startDownload(downloadUrl, resumeFromExisting = false)
                            }
                        )
                    } else {
                        showMessage("请先检查更新以获取下载地址")
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val gold = ContextCompat.getColor(this, R.color.gold)
        setStatusBarColor(white, lightIcons = false)
        binding.toolbar.setupSimple(
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            title = "应用更新",
            titleTextColor = white,
            backgroundColor = gold,
            onLeftClick = { finish() }
        )
    }

    private fun updateVersionInfo() {
        binding.tvVersionInfo.text = viewModel.currentVersion
    }

    /**
     * 处理 UI 状态 - 重新设计，更清晰的状态管理
     */
    private fun handleUiState(state: AppUpdateUiState) {
        when (state) {
            is AppUpdateUiState.Initial -> {
                showInitialState()
            }
            
            is AppUpdateUiState.Checking -> {
                showCheckingState()
            }
            
            is AppUpdateUiState.CheckFailed -> {
                showCheckFailedState(state.error)
            }
            
            is AppUpdateUiState.UpdateAvailable -> {
                showUpdateAvailableState(state)
            }
            
            is AppUpdateUiState.AlreadyLatest -> {
                showAlreadyLatestState()
            }
            
            is AppUpdateUiState.IncompleteDownloadDetected -> {
                showIncompleteDownloadState(state)
            }
            
            is AppUpdateUiState.ApkFileReady -> {
                showApkFileReadyState(state)
            }
            
            is AppUpdateUiState.Downloading -> {
                showDownloadingState(state)
            }
            
            is AppUpdateUiState.DownloadPaused -> {
                showDownloadPausedState(state)
            }
            
            is AppUpdateUiState.DownloadCompleted -> {
                showDownloadCompletedState(state)
            }
            
            is AppUpdateUiState.DownloadFailed -> {
                showDownloadFailedState(state)
            }
            
            is AppUpdateUiState.DownloadCancelled -> {
                showDownloadCancelledState()
            }
        }
    }

    /**
     * 初始状态
     */
    private fun showInitialState() {
        binding.cardUpdateContent.visibility = View.GONE
        binding.cardProgress.visibility = View.GONE
        binding.cardStatus.visibility = View.VISIBLE
        
        binding.tvStatusTitle.text = "状态"
        binding.tvStatusContent.text = "点击检查更新按钮检查是否有新版本"
        
        binding.btnCheckUpdate.visibility = View.VISIBLE
        binding.btnCheckUpdate.isEnabled = true
        binding.btnDownload.visibility = View.GONE
        binding.btnInstall.visibility = View.GONE
        binding.llDownloadControls.visibility = View.GONE
    }

    /**
     * 检查中状态
     */
    private fun showCheckingState() {
        binding.cardUpdateContent.visibility = View.GONE
        binding.cardProgress.visibility = View.GONE
        binding.cardStatus.visibility = View.VISIBLE
        
        binding.tvStatusTitle.text = "检查更新"
        binding.tvStatusContent.text = "正在检查更新，请稍候..."
        
        binding.btnCheckUpdate.isEnabled = false
        binding.btnCheckUpdate.text = "检查中..."
    }

    /**
     * 检查失败状态
     */
    private fun showCheckFailedState(error: String) {
        binding.cardUpdateContent.visibility = View.GONE
        binding.cardProgress.visibility = View.GONE
        binding.cardStatus.visibility = View.VISIBLE
        
        binding.tvStatusTitle.text = "检查失败"
        binding.tvStatusContent.text = error
        
        binding.btnCheckUpdate.isEnabled = true
        binding.btnCheckUpdate.text = "重新检查"
        
        showError("检查更新失败: $error")
    }

    /**
     * 有更新可用状态
     */
    private fun showUpdateAvailableState(state: AppUpdateUiState.UpdateAvailable) {
        // 更新版本信息
        binding.tvLatestVersion.text = "${state.latestVersion} (${state.latestVersionCode})"
        
        // 显示更新内容
        if (state.updateInfo.isNotBlank()) {
            binding.tvUpdateContent.text = state.updateInfo
            binding.cardUpdateContent.visibility = View.VISIBLE
        } else {
            binding.cardUpdateContent.visibility = View.GONE
        }
        
        // 检查是否有未完成的下载
        val hasIncompleteFile = viewModel.isApkFileIncomplete(state.fileSize)
        
        if (hasIncompleteFile) {
            // 显示未完成下载的进度
            val existingSize = viewModel.getApkFileSize()
            val progress = if (state.fileSize > 0) {
                ((existingSize * 100) / state.fileSize).toInt().coerceIn(0, 100)
            } else {
                0
            }
            
            binding.cardProgress.visibility = View.VISIBLE
            binding.progressBar.progress = progress
            binding.tvProgressText.text = "${progress}%"
            binding.tvDownloadedSize.text = existingSize.formatFileSize()
            binding.tvTotalSize.text = state.fileSize.formatFileSize()
            binding.llProgressDetails.visibility = View.VISIBLE
            
            binding.btnDownload.visibility = View.GONE
            binding.btnResume.visibility = View.VISIBLE
            binding.btnRetry.visibility = View.VISIBLE
            binding.btnPause.visibility = View.GONE
            binding.btnCancel.visibility = View.GONE
            binding.llDownloadControls.visibility = View.VISIBLE
        } else {
            // 显示下载按钮
            binding.cardProgress.visibility = View.GONE
            binding.btnDownload.visibility = View.VISIBLE
            binding.btnInstall.visibility = View.GONE
            binding.llDownloadControls.visibility = View.GONE
        }
        
        // 隐藏状态卡片
        binding.cardStatus.visibility = View.GONE
        
        binding.btnCheckUpdate.visibility = View.VISIBLE
        binding.btnCheckUpdate.isEnabled = true
    }

    /**
     * 已是最新版本状态
     */
    private fun showAlreadyLatestState() {
        binding.cardUpdateContent.visibility = View.GONE
        binding.cardProgress.visibility = View.GONE
        binding.cardStatus.visibility = View.VISIBLE
        
        binding.tvStatusTitle.text = "已是最新版本"
        binding.tvStatusContent.text = "当前已是最新版本，无需更新"
        
        binding.btnCheckUpdate.visibility = View.VISIBLE
        binding.btnCheckUpdate.isEnabled = true
        binding.btnDownload.visibility = View.GONE
        binding.btnInstall.visibility = View.GONE
        binding.llDownloadControls.visibility = View.GONE
        
        showMessage("已是最新版本")
    }

    /**
     * 检测到未完成的下载
     */
    private fun showIncompleteDownloadState(state: AppUpdateUiState.IncompleteDownloadDetected) {
        binding.cardUpdateContent.visibility = View.GONE
        binding.cardProgress.visibility = View.VISIBLE
        binding.cardStatus.visibility = View.GONE
        
        val progress = if (state.totalBytes > 0) {
            ((state.downloadedBytes * 100) / state.totalBytes).toInt().coerceIn(0, 100)
        } else {
            -1
        }
        
        binding.progressBar.progress = if (progress >= 0) progress else 0
        binding.tvProgressText.text = if (progress >= 0) "${progress}%" else "进行中"
        binding.tvDownloadedSize.text = state.downloadedBytes.formatFileSize()
        binding.tvTotalSize.text = if (state.totalBytes > 0) state.totalBytes.formatFileSize() else "未知"
        binding.llProgressDetails.visibility = View.VISIBLE
        
        binding.btnCheckUpdate.visibility = View.VISIBLE
        binding.btnCheckUpdate.isEnabled = true
        binding.btnDownload.visibility = View.GONE
        binding.btnInstall.visibility = View.GONE
        binding.btnResume.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.VISIBLE
        binding.btnPause.visibility = View.GONE
        binding.btnCancel.visibility = View.GONE
        binding.llDownloadControls.visibility = View.VISIBLE
    }

    /**
     * APK文件已准备好
     */
    private fun showApkFileReadyState(state: AppUpdateUiState.ApkFileReady) {
        binding.cardUpdateContent.visibility = View.GONE
        binding.cardProgress.visibility = View.GONE
        binding.cardStatus.visibility = View.VISIBLE
        
        val fileSize = FileUtils.getFileSize(state.file.absolutePath)
        binding.tvStatusTitle.text = "可以安装"
        binding.tvStatusContent.text = "APK文件已存在，可以安装\n文件大小: ${fileSize.formatFileSize()}"
        
        binding.btnCheckUpdate.visibility = View.VISIBLE
        binding.btnCheckUpdate.isEnabled = true
        binding.btnDownload.visibility = View.GONE
        binding.btnInstall.visibility = View.VISIBLE
        binding.llDownloadControls.visibility = View.GONE
    }

    /**
     * 下载中状态
     */
    private fun showDownloadingState(state: AppUpdateUiState.Downloading) {
        binding.cardProgress.visibility = View.VISIBLE
        binding.cardStatus.visibility = View.GONE
        
        binding.progressBar.progress = state.progress
        binding.tvProgressText.text = "${state.progress}%"
        binding.tvDownloadedSize.text = state.downloadedBytes.formatFileSize()
        binding.tvTotalSize.text = state.totalBytes.formatFileSize()
        
        // 显示下载速度
        val speedText = if (state.speed > 0) {
            "${state.speed.formatFileSize()}/s"
        } else {
            "--"
        }
        binding.tvDownloadSpeed.text = "速度: $speedText"
        
        // 显示剩余时间
        val timeText = if (state.estimatedTimeRemaining > 0) {
            formatTime(state.estimatedTimeRemaining)
        } else {
            "--"
        }
        binding.tvDownloadTime.text = "剩余时间: $timeText"
        binding.llProgressDetails.visibility = View.VISIBLE
        
        binding.btnDownload.visibility = View.GONE
        binding.btnInstall.visibility = View.GONE
        binding.btnResume.visibility = View.GONE
        binding.btnPause.visibility = View.VISIBLE
        binding.btnCancel.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.GONE
        binding.llDownloadControls.visibility = View.VISIBLE
    }

    /**
     * 下载已暂停状态
     */
    private fun showDownloadPausedState(state: AppUpdateUiState.DownloadPaused) {
        binding.cardProgress.visibility = View.VISIBLE
        binding.cardStatus.visibility = View.GONE
        
        val progress = if (state.totalBytes > 0) {
            ((state.downloadedBytes * 100) / state.totalBytes).toInt().coerceIn(0, 100)
        } else {
            0
        }
        
        binding.progressBar.progress = progress
        binding.tvProgressText.text = "${progress}%"
        binding.tvDownloadedSize.text = state.downloadedBytes.formatFileSize()
        binding.tvTotalSize.text = state.totalBytes.formatFileSize()
        binding.tvDownloadSpeed.text = "速度: --"
        binding.tvDownloadTime.text = "剩余时间: --"
        binding.llProgressDetails.visibility = View.VISIBLE
        
        binding.btnDownload.visibility = View.GONE
        binding.btnInstall.visibility = View.GONE
        binding.btnResume.visibility = View.VISIBLE
        binding.btnPause.visibility = View.GONE
        binding.btnCancel.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.GONE
        binding.llDownloadControls.visibility = View.VISIBLE
    }

    /**
     * 下载完成状态
     */
    private fun showDownloadCompletedState(state: AppUpdateUiState.DownloadCompleted) {
        binding.cardProgress.visibility = View.GONE
        binding.cardStatus.visibility = View.VISIBLE
        
        val fileSize = FileUtils.getFileSize(state.file.absolutePath)
        binding.tvStatusTitle.text = "下载完成"
        binding.tvStatusContent.text = "下载完成，文件大小: ${fileSize.formatFileSize()}\n可以开始安装"
        
        binding.btnDownload.visibility = View.GONE
        binding.btnInstall.visibility = View.VISIBLE
        binding.llDownloadControls.visibility = View.GONE
        
        showMessage("下载完成")
    }

    /**
     * 下载失败状态
     */
    private fun showDownloadFailedState(state: AppUpdateUiState.DownloadFailed) {
        binding.cardProgress.visibility = View.GONE
        binding.cardStatus.visibility = View.VISIBLE
        
        binding.tvStatusTitle.text = "下载失败"
        binding.tvStatusContent.text = state.error
        
        binding.btnDownload.visibility = View.GONE
        binding.btnInstall.visibility = View.GONE
        binding.llDownloadControls.visibility = View.GONE
        
        if (state.canRetry) {
            binding.btnRetry.visibility = View.VISIBLE
            binding.btnRetry.text = "重试下载"
        } else {
            binding.btnRetry.visibility = View.GONE
        }
        
        showError("下载失败: ${state.error}")
    }

    /**
     * 下载已取消状态
     */
    private fun showDownloadCancelledState() {
        binding.cardProgress.visibility = View.GONE
        binding.cardStatus.visibility = View.VISIBLE
        
        binding.tvStatusTitle.text = "下载已取消"
        binding.tvStatusContent.text = "下载已取消，可以重新开始下载"
        
        binding.btnDownload.visibility = View.VISIBLE
        binding.btnInstall.visibility = View.GONE
        binding.llDownloadControls.visibility = View.GONE
        
        showMessage("下载已取消")
    }

    /**
     * 格式化时间（秒转分钟:秒）
     */
    private fun formatTime(seconds: Long): String {
        if (seconds < 60) {
            return "${seconds}秒"
        }
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (remainingSeconds > 0) {
            "${minutes}分${remainingSeconds}秒"
        } else {
            "${minutes}分钟"
        }
    }

    /**
     * 请求安装权限并开始下载
     */
    private fun requestInstallPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.canRequestPackageInstalls()) {
                // 已有权限，直接下载
                val downloadUrl = when (val state = viewModel.appUpdateState.value) {
                    is AppUpdateUiState.UpdateAvailable -> state.downloadUrl
                    else -> {
                        showError("请先检查更新")
                        return
                    }
                }
                startDownload(downloadUrl)
            } else {
                // 请求权限
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                installPermissionLauncher.launch(intent)
            }
        } else {
            // Android 8.0 以下不需要权限
            val downloadUrl = when (val state = viewModel.appUpdateState.value) {
                is AppUpdateUiState.UpdateAvailable -> state.downloadUrl
                else -> {
                    showError("请先检查更新")
                    return
                }
            }
            startDownload(downloadUrl)
        }
    }

    /**
     * 开始下载
     */
    private fun startDownload(downloadUrl: String) {
        // 如果文件已存在，判断是继续下载还是重新下载
        if (viewModel.isApkFileExists) {
            val apkFile = viewModel.apkFilePath
            val fileSize = FileUtils.getFileSize(apkFile.absolutePath)
            
            // 检查文件是否可能是未完成的
            val currentState = viewModel.appUpdateState.value
            val expectedSize = if (currentState is AppUpdateUiState.UpdateAvailable) {
                currentState.fileSize
            } else {
                0L
            }
            
            if (expectedSize > 0 && fileSize < expectedSize * 0.95) {
                // 文件未完成，询问是继续下载还是重新下载
                showConfirmDialog(
                    title = "检测到未完成的下载",
                    message = "发现未完成的下载文件（${fileSize.formatFileSize()}），是否继续下载？\n\n点击确认继续下载，点击取消将重新下载",
                    onConfirm = {
                        viewModel.startDownload(downloadUrl, resumeFromExisting = true)
                    },
                    onCancel = {
                        viewModel.deleteApkFile()
                        viewModel.startDownload(downloadUrl, resumeFromExisting = false)
                    }
                )
            } else {
                // 文件完整，询问是否重新下载
                showConfirmDialog(
                    title = "文件已存在",
                    message = "APK文件已存在（${fileSize.formatFileSize()}），是否重新下载？",
                    onConfirm = {
                        viewModel.deleteApkFile()
                        viewModel.startDownload(downloadUrl, resumeFromExisting = false)
                    },
                    onCancel = {
                        // 取消重新下载，显示安装按钮
                        binding.btnInstall.visibility = View.VISIBLE
                        binding.btnDownload.visibility = View.GONE
                    }
                )
            }
        } else {
            // 文件不存在，直接下载
            viewModel.startDownload(downloadUrl, resumeFromExisting = false)
        }
    }

    /**
     * 安装APK
     */
    private fun installApk() {
        val apkFile = viewModel.apkFilePath
        
        if (!apkFile.exists() || !viewModel.isApkFileValid()) {
            showError("APK文件损坏，请重新下载")
            viewModel.deleteApkFile()
            binding.btnInstall.visibility = View.GONE
            binding.btnDownload.visibility = View.VISIBLE
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        this@AppUpdateActivity,
                        "${packageName}.fileprovider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                }
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(intent)
            showMessage("正在安装...")
        } catch (e: Exception) {
            Timber.e(e, "安装APK失败")
            showError("安装失败: ${e.message}")
        }
    }
}
