package com.jun.andprj.ui.tool

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.jun.andprj.R
import com.jun.andprj.databinding.ItemDownloadTaskBinding
import com.jun.core.common.extension.formatFileSize
import com.jun.core.ui.adapter.BaseAdapter
import com.jun.core.ui.webview.download.DownloadTask

/**
 * 下载任务适配器
 * 负责显示下载任务列表的每个 item
 */
class DownloadTaskAdapter(
    private val onPauseClick: (DownloadTask) -> Unit,
    private val onResumeClick: (DownloadTask) -> Unit,
    private val onRestartClick: (DownloadTask) -> Unit,
    private val onCancelClick: (DownloadTask) -> Unit,
    private val onOpenClick: (DownloadTask) -> Unit,
    private val onDeleteClick: (DownloadTask) -> Unit
) : BaseAdapter<DownloadTask, ItemDownloadTaskBinding>(DownloadTaskDiffCallback) {
    
    override fun createBinding(parent: ViewGroup, viewType: Int): ItemDownloadTaskBinding {
        return ItemDownloadTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }
    
    override fun bind(binding: ItemDownloadTaskBinding, item: DownloadTask, position: Int) {
        bindTask(binding, item)
    }
    
    override fun bind(
        binding: ItemDownloadTaskBinding,
        item: DownloadTask,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.isEmpty()) {
            bindTask(binding, item)
            return
        }
        
        // 处理 payload
        val payloadSet = payloads.flatMap { payload ->
            when (payload) {
                is List<*> -> payload.mapNotNull { it as? String }
                else -> listOf(payload.toString())
            }
        }.toSet()
        
        // 状态变化时，完整更新
        if (payloadSet.contains("state")) {
            bindTask(binding, item)
            return
        }
        
        // 只更新进度相关
        if (payloadSet.contains("progress")) {
            val actualFileSize = DownloadTaskUtils.getActualFileSize(item)
            val displayDownloaded = if (actualFileSize > 0) actualFileSize else item.downloadedBytes
            val calculatedProgress = DownloadTaskUtils.calculateProgress(item)
            
            binding.tvFileSize.text = "${displayDownloaded.formatFileSize()} / ${if (item.totalBytes > 0) item.totalBytes.formatFileSize() else "未知"}"
            binding.progressBar.progress = calculatedProgress
            binding.tvProgress.text = "$calculatedProgress%"
        }
        
        // 只更新速度
        if (payloadSet.contains("speed")) {
            binding.tvSpeed.text = when (item.state) {
                DownloadTask.DownloadTaskState.Downloading -> "${item.speed.formatFileSize()}/s"
                DownloadTask.DownloadTaskState.Failed -> item.error ?: ""
                else -> ""
            }
        }
    }
    
    private fun bindTask(binding: ItemDownloadTaskBinding, task: DownloadTask) {
        // 更新基本信息
        binding.tvFileName.text = task.fileName
        
        // 获取实际文件大小
        val actualFileSize = DownloadTaskUtils.getActualFileSize(task)
        
        // 计算并更新进度（根据实际文件大小和预期总大小）
        val calculatedProgress = DownloadTaskUtils.calculateProgress(task)
        
        // 显示文件大小：实际大小 / 总大小（如果有）
        val displayDownloaded = if (actualFileSize > 0) actualFileSize else task.downloadedBytes
        val displayTotal = if (task.totalBytes > 0) task.totalBytes.formatFileSize() else "未知"
        binding.tvFileSize.text = "${displayDownloaded.formatFileSize()} / $displayTotal"
        
        // 判断文件是否完整
        val isComplete = DownloadTaskUtils.isFileComplete(task)
        
        // 根据状态和文件完整性更新 UI
        when (task.state) {
            DownloadTask.DownloadTaskState.Preparing -> {
                binding.tvStatus.text = "准备中"
                binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_secondary))
                binding.progressBar.progress = calculatedProgress.coerceAtLeast(0)
                binding.tvProgress.text = "${calculatedProgress.coerceAtLeast(0)}%"
                binding.tvSpeed.text = ""
                showButtons(binding, showPause = false, showResume = false, showRestart = false, showCancel = true, showOpen = false, showDelete = false)
            }
            
            DownloadTask.DownloadTaskState.Downloading -> {
                binding.tvStatus.text = "下载中"
                binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.blue))
                binding.progressBar.progress = calculatedProgress
                binding.tvProgress.text = "$calculatedProgress%"
                binding.tvSpeed.text = "${task.speed.formatFileSize()}/s"
                showButtons(binding, showPause = true, showResume = false, showRestart = false, showCancel = true, showOpen = false, showDelete = false)
            }
            
            DownloadTask.DownloadTaskState.Paused -> {
                binding.tvStatus.text = "暂停下载"
                binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_secondary))
                binding.progressBar.progress = calculatedProgress
                binding.tvProgress.text = "$calculatedProgress%"
                binding.tvSpeed.text = ""
                // 如果文件完整，显示打开按钮；否则显示继续和重新下载按钮
                if (isComplete) {
                    showButtons(binding, showPause = false, showResume = false, showRestart = false, showCancel = false, showOpen = true, showDelete = true)
                } else {
                    showButtons(binding, showPause = false, showResume = true, showRestart = true, showCancel = false, showOpen = false, showDelete = true)
                }
            }
            
            DownloadTask.DownloadTaskState.Completed -> {
                // 对于 Completed 状态，需要根据文件完整性来判断显示什么按钮
                if (isComplete) {
                    binding.tvStatus.text = "下载完成"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark))
                    binding.progressBar.progress = 100
                    binding.tvProgress.text = "100%"
                    binding.tvSpeed.text = ""
                    showButtons(binding, showPause = false, showResume = false, showRestart = false, showCancel = false, showOpen = true, showDelete = true)
                } else {
                    // 文件不完整，显示为失败状态，允许继续下载或重新下载
                    binding.tvStatus.text = "下载失败"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark))
                    binding.progressBar.progress = calculatedProgress
                    binding.tvProgress.text = "$calculatedProgress%"
                    binding.tvSpeed.text = "文件不完整"
                    showButtons(binding, showPause = false, showResume = true, showRestart = true, showCancel = false, showOpen = false, showDelete = true)
                }
            }
            
            DownloadTask.DownloadTaskState.Failed -> {
                // 判断是否为历史任务（URL 以 "file://" 开头）
                val isHistoricalTask = task.url.startsWith("file://")
                
                // 对于 Failed 状态，需要判断：
                // 1. 如果文件完整，显示"下载完成"（即使状态是 Failed，但文件已经完整）
                // 2. 如果文件不完整且有错误信息（网络问题、地址问题等），显示"下载失败"
                // 3. 如果文件不完整且没有错误信息（可能是历史任务），显示"下载失败"
                if (isComplete) {
                    // 文件完整：显示为"下载完成"
                    binding.tvStatus.text = "下载完成"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark))
                    binding.progressBar.progress = 100
                    binding.tvProgress.text = "100%"
                    binding.tvSpeed.text = ""
                    showButtons(binding, showPause = false, showResume = false, showRestart = false, showCancel = false, showOpen = true, showDelete = true)
                } else {
                    // 文件不完整：显示为"下载失败"（网络问题、地址问题等异常）
                    binding.tvStatus.text = "下载失败"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark))
                    binding.progressBar.progress = calculatedProgress
                    binding.tvProgress.text = "$calculatedProgress%"
                    binding.tvSpeed.text = if (isHistoricalTask) "文件不完整" else (task.error ?: "文件不完整")
                    showButtons(binding, showPause = false, showResume = true, showRestart = true, showCancel = false, showOpen = false, showDelete = true)
                }
            }
            
            DownloadTask.DownloadTaskState.Cancelled -> {
                // 根据文件完整性判断按钮显示
                if (isComplete) {
                    // 文件完整：显示"下载完成"状态和"打开/删除"按钮
                    binding.tvStatus.text = "下载完成"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark))
                    binding.progressBar.progress = 100
                    binding.tvProgress.text = "100%"
                    binding.tvSpeed.text = ""
                    showButtons(binding, showPause = false, showResume = false, showRestart = false, showCancel = false, showOpen = true, showDelete = true)
                } else {
                    // 文件不完整：显示"下载取消"状态和"继续/重新下载/删除"按钮
                    binding.tvStatus.text = "下载取消"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_secondary))
                    binding.progressBar.progress = calculatedProgress
                    binding.tvProgress.text = "$calculatedProgress%"
                    binding.tvSpeed.text = ""
                    showButtons(binding, showPause = false, showResume = true, showRestart = true, showCancel = false, showOpen = false, showDelete = true)
                }
            }
        }
        
        // 设置按钮点击事件（使用 tag 避免重复绑定）
        binding.root.tag = task.url
        
        // 清除之前的点击监听器，避免重复绑定
        binding.btnPause.setOnClickListener(null)
        binding.btnResume.setOnClickListener(null)
        binding.btnRestart.setOnClickListener(null)
        binding.btnCancel.setOnClickListener(null)
        binding.btnOpen.setOnClickListener(null)
        binding.btnDelete.setOnClickListener(null)
        
        // 设置新的点击监听器
        binding.btnPause.setOnClickListener { onPauseClick(task) }
        binding.btnResume.setOnClickListener { onResumeClick(task) }
        binding.btnRestart.setOnClickListener { onRestartClick(task) }
        binding.btnCancel.setOnClickListener { onCancelClick(task) }
        binding.btnOpen.setOnClickListener { onOpenClick(task) }
        binding.btnDelete.setOnClickListener { onDeleteClick(task) }
    }
    
    private fun showButtons(
        binding: ItemDownloadTaskBinding,
        showPause: Boolean,
        showResume: Boolean,
        showRestart: Boolean,
        showCancel: Boolean,
        showOpen: Boolean,
        showDelete: Boolean
    ) {
        binding.btnPause.visibility = if (showPause) View.VISIBLE else View.GONE
        binding.btnResume.visibility = if (showResume) View.VISIBLE else View.GONE
        binding.btnRestart.visibility = if (showRestart) View.VISIBLE else View.GONE
        binding.btnCancel.visibility = if (showCancel) View.VISIBLE else View.GONE
        binding.btnOpen.visibility = if (showOpen) View.VISIBLE else View.GONE
        binding.btnDelete.visibility = if (showDelete) View.VISIBLE else View.GONE
    }
}

