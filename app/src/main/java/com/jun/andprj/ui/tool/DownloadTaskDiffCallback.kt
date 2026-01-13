package com.jun.andprj.ui.tool

import androidx.recyclerview.widget.DiffUtil
import com.jun.core.ui.webview.download.DownloadTask

/**
 * DownloadTask 的 DiffUtil.ItemCallback
 * 用于优化 RecyclerView 的更新性能
 */
object DownloadTaskDiffCallback : DiffUtil.ItemCallback<DownloadTask>() {
    
    override fun areItemsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
        return oldItem.url == newItem.url
    }
    
    override fun areContentsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
        // 始终返回 false，强制 DiffUtil 进行详细比较
        // 这样可以确保即使对象引用相同，内容变化也能被检测到
        // 注意：这会导致性能略有下降，但对于下载进度更新是必要的
        return false
    }
    
    override fun getChangePayload(oldItem: DownloadTask, newItem: DownloadTask): Any? {
        val payloads = mutableListOf<String>()
        
        // 状态变化是最重要的
        if (oldItem.state != newItem.state) {
            payloads.add("state")
        }
        
        // 进度变化
        if (oldItem.progress != newItem.progress || 
            oldItem.downloadedBytes != newItem.downloadedBytes ||
            oldItem.totalBytes != newItem.totalBytes) {
            payloads.add("progress")
        }
        
        // 速度变化
        if (oldItem.speed != newItem.speed) {
            payloads.add("speed")
        }
        
        return payloads.ifEmpty { null }
    }
}

