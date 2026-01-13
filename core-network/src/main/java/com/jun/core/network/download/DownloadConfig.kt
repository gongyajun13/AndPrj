package com.jun.core.network.download

/**
 * 下载配置
 * 用于自定义下载行为
 */
data class DownloadConfig(
    /**
     * 是否从已存在的文件继续下载（断点续传）
     * 默认：true
     */
    val resumeFromExisting: Boolean = true,
    
    /**
     * 缓冲区大小（字节）
     * 默认：8192 (8KB)
     * 建议范围：4096 - 65536
     */
    val bufferSize: Int = 8192,
    
    /**
     * 进度更新间隔（毫秒）
     * 默认：1000 (1秒)
     * 建议范围：500 - 5000
     */
    val progressUpdateInterval: Long = 1000,
    
    /**
     * 下载超时时间（毫秒）
     * 0 表示不设置超时（使用 OkHttpClient 的默认超时）
     * 默认：0
     */
    val timeoutMillis: Long = 0,
    
    /**
     * 是否在下载完成后验证文件大小
     * 如果为 true，下载完成后会检查文件大小是否与 Content-Length 一致
     * 默认：true
     */
    val verifyFileSize: Boolean = true,
    
    /**
     * 进度回调（可选，用于兼容旧代码）
     * 注意：建议使用 Flow 的 DownloadState.Downloading 来获取进度
     */
    val onProgress: ((progress: Int, downloaded: Long, total: Long) -> Unit)? = null
) {
    companion object {
        /**
         * 默认配置
         */
        @JvmStatic
        val DEFAULT = DownloadConfig()
        
        /**
         * 快速下载配置（大缓冲区，频繁更新）
         */
        @JvmStatic
        val FAST = DownloadConfig(
            bufferSize = 16384, // 16KB
            progressUpdateInterval = 500 // 0.5秒
        )
        
        /**
         * 省电配置（小缓冲区，较少更新）
         */
        @JvmStatic
        val POWER_SAVING = DownloadConfig(
            bufferSize = 4096, // 4KB
            progressUpdateInterval = 2000 // 2秒
        )
        
        /**
         * 大文件配置（大缓冲区，适中更新）
         */
        @JvmStatic
        val LARGE_FILE = DownloadConfig(
            bufferSize = 32768, // 32KB
            progressUpdateInterval = 1000 // 1秒
        )
    }
}

