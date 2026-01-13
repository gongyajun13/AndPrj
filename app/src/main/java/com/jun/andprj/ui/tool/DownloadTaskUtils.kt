package com.jun.andprj.ui.tool

import com.jun.core.ui.webview.download.DownloadTask
import java.io.File

/**
 * 下载任务工具类
 * 提供文件完整性判断、进度计算等工具方法
 */
object DownloadTaskUtils {
    
    /**
     * 判断文件是否完整
     * 根据实际文件大小和预期总大小来判断
     */
    fun isFileComplete(task: DownloadTask): Boolean {
        val file = File(task.filePath)
        if (!file.exists()) {
            return false
        }
        
        val actualSize = file.length()
        
        // 如果有预期的总大小，比较实际文件大小和总大小
        if (task.totalBytes > 0) {
            // 文件大小应该在预期的 95%-105% 范围内（允许一些误差）
            return actualSize >= task.totalBytes * 0.95 && actualSize <= task.totalBytes * 1.05
        }
        
        // 如果没有预期的总大小（历史任务），判断是否为历史任务
        // 历史任务的 URL 以 "file://" 开头，表示这是从本地加载的任务
        val isHistoricalTask = task.url.startsWith("file://")
        
        if (isHistoricalTask) {
            // 对于历史任务，我们需要更智能地判断文件是否完整
            // 1. 检查文件大小是否合理（至少 1KB）
            if (actualSize < 1024) {
                return false
            }
            
            // 2. 尝试根据文件扩展名和文件头来判断
            // 对于 APK 文件，可以检查 ZIP 文件头（APK 是 ZIP 格式）
            // 对于其他文件，只能根据文件大小和扩展名粗略判断
            val fileName = task.fileName.lowercase()
            if (fileName.endsWith(".apk")) {
                // APK 文件：检查 ZIP 文件头（APK 是 ZIP 格式）
                try {
                    val file = File(task.filePath)
                    if (file.length() < 22) { // ZIP 文件头至少需要 22 字节
                        return false
                    }
                    val buffer = ByteArray(4)
                    file.inputStream().use { it.read(buffer) }
                    // ZIP 文件头：PK\x03\x04 或 PK\x05\x06（空 ZIP）或 PK\x07\x08（ZIP64）
                    val isZip = buffer[0] == 0x50.toByte() && buffer[1] == 0x4B.toByte()
                    if (!isZip) {
                        return false
                    }
                    // 进一步检查：尝试读取 ZIP 文件结构
                    // 简单判断：如果文件大小小于 1MB，可能不完整
                    if (actualSize < 1024 * 1024) {
                        return false
                    }
                    // 对于 APK，如果文件大小合理且 ZIP 头正确，假设完整
                    return true
                } catch (e: Exception) {
                    // 如果检查失败，保守地假设不完整
                    return false
                }
            } else {
                // 其他文件类型：只能根据文件大小粗略判断
                // 如果文件大小小于 1KB，认为不完整
                // 如果文件大小 >= 1KB，假设完整（因为我们无法知道真实总大小）
                return actualSize >= 1024
            }
        }
        
        // 对于其他情况，如果状态是 Completed，假设文件完整
        return task.state == DownloadTask.DownloadTaskState.Completed
    }
    
    /**
     * 计算并更新任务的进度
     * 根据实际文件大小和预期总大小计算
     */
    fun calculateProgress(task: DownloadTask): Int {
        val file = File(task.filePath)
        if (!file.exists()) {
            return task.progress
        }
        
        val actualSize = file.length()
        
        // 如果有预期的总大小，根据实际文件大小计算进度
        if (task.totalBytes > 0) {
            val progress = ((actualSize * 100) / task.totalBytes).toInt().coerceIn(0, 100)
            return progress
        }
        
        // 如果没有预期的总大小，使用任务中保存的进度
        return task.progress
    }
    
    /**
     * 获取实际文件大小
     */
    fun getActualFileSize(task: DownloadTask): Long {
        val file = File(task.filePath)
        return if (file.exists()) file.length() else 0L
    }
}

