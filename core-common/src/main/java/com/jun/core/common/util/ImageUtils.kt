package com.jun.core.common.util

import com.jun.core.common.extension.formatFileSize
import java.io.File
import java.util.Base64

/**
 * 图片工具类
 * 提供图片相关的通用工具方法
 * 
 * 注意：由于 core-common 是纯 Kotlin 模块，不包含 Android 依赖，
 * 因此不提供 Bitmap 相关的操作（压缩、裁剪、旋转等）。
 * 如需 Bitmap 操作，请使用 core-ui 模块中的扩展函数。
 */
object ImageUtils {
    
    /**
     * 支持的图片格式
     */
    enum class ImageFormat(val extension: String, val mimeType: String) {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        GIF("gif", "image/gif"),
        WEBP("webp", "image/webp"),
        BMP("bmp", "image/bmp"),
        SVG("svg", "image/svg+xml");
        
        companion object {
            /**
             * 根据文件扩展名获取格式
             */
            fun fromExtension(extension: String): ImageFormat? {
                return values().find { 
                    it.extension.equals(extension, ignoreCase = true) 
                }
            }
            
            /**
             * 根据 MIME 类型获取格式
             */
            fun fromMimeType(mimeType: String): ImageFormat? {
                return values().find { 
                    it.mimeType.equals(mimeType, ignoreCase = true) 
                }
            }
        }
    }
    
    /**
     * 图片信息
     */
    data class ImageInfo(
        val path: String,
        val format: ImageFormat?,
        val fileSize: Long,
        val fileName: String,
        val extension: String
    )
    
    /**
     * 从文件路径获取图片格式
     */
    fun getImageFormat(filePath: String): ImageFormat? {
        val extension = FileUtils.getFileExtension(filePath)
        return ImageFormat.fromExtension(extension)
    }
    
    /**
     * 从 MIME 类型获取图片格式
     */
    fun getImageFormatFromMimeType(mimeType: String): ImageFormat? {
        return ImageFormat.fromMimeType(mimeType)
    }
    
    /**
     * 验证是否为支持的图片格式
     */
    fun isImageFile(filePath: String): Boolean {
        return getImageFormat(filePath) != null
    }
    
    /**
     * 验证是否为支持的图片格式（通过扩展名）
     */
    fun isImageFileByExtension(extension: String): Boolean {
        return ImageFormat.fromExtension(extension) != null
    }
    
    /**
     * 获取图片信息
     */
    fun getImageInfo(filePath: String): ImageInfo? {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) {
                return null
            }
            
            val format = getImageFormat(filePath)
            val fileName = file.name
            val extension = FileUtils.getFileExtension(filePath)
            
            ImageInfo(
                path = filePath,
                format = format,
                fileSize = file.length(),
                fileName = fileName,
                extension = extension
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取图片文件大小（格式化）
     */
    fun getImageFileSizeFormatted(filePath: String): String {
        val size = FileUtils.getFileSize(filePath)
        return size.formatFileSize()
    }
    
    /**
     * Base64 编码图片文件
     */
    fun encodeImageToBase64(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) {
                return null
            }
            
            val bytes = file.readBytes()
            Base64.getEncoder().encodeToString(bytes)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Base64 解码并保存为图片文件
     */
    fun decodeBase64ToImage(base64String: String, outputPath: String): Boolean {
        return try {
            val bytes = Base64.getDecoder().decode(base64String)
            writeBytesToFile(outputPath, bytes)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 计算宽高比
     */
    fun calculateAspectRatio(width: Int, height: Int): Float {
        return if (height > 0) {
            width.toFloat() / height.toFloat()
        } else {
            0f
        }
    }
    
    /**
     * 根据宽高比和宽度计算高度
     */
    fun calculateHeightByAspectRatio(width: Int, aspectRatio: Float): Int {
        return if (aspectRatio > 0) {
            (width / aspectRatio).toInt()
        } else {
            0
        }
    }
    
    /**
     * 根据宽高比和高度计算宽度
     */
    fun calculateWidthByAspectRatio(height: Int, aspectRatio: Float): Int {
        return if (aspectRatio > 0) {
            (height * aspectRatio).toInt()
        } else {
            0
        }
    }
    
    /**
     * 计算缩放比例（保持宽高比）
     */
    fun calculateScaleRatio(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Float {
        val widthRatio = if (originalWidth > 0) {
            maxWidth.toFloat() / originalWidth.toFloat()
        } else {
            1f
        }
        
        val heightRatio = if (originalHeight > 0) {
            maxHeight.toFloat() / originalHeight.toFloat()
        } else {
            1f
        }
        
        // 取较小的比例，确保图片完全适应目标尺寸
        return minOf(widthRatio, heightRatio, 1f)
    }
    
    /**
     * 计算缩放后的尺寸（保持宽高比）
     */
    fun calculateScaledSize(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Pair<Int, Int> {
        val scaleRatio = calculateScaleRatio(originalWidth, originalHeight, maxWidth, maxHeight)
        val scaledWidth = (originalWidth * scaleRatio).toInt()
        val scaledHeight = (originalHeight * scaleRatio).toInt()
        return Pair(scaledWidth, scaledHeight)
    }
    
    /**
     * 验证图片尺寸是否在指定范围内
     */
    fun isSizeInRange(
        width: Int,
        height: Int,
        minWidth: Int = 0,
        minHeight: Int = 0,
        maxWidth: Int = Int.MAX_VALUE,
        maxHeight: Int = Int.MAX_VALUE
    ): Boolean {
        return width in minWidth..maxWidth && height in minHeight..maxHeight
    }
    
    /**
     * 获取推荐的图片压缩质量（基于文件大小）
     * 返回 0-100 的质量值
     */
    fun getRecommendedQuality(fileSize: Long, targetSize: Long): Int {
        return when {
            fileSize <= targetSize -> 100
            fileSize <= targetSize * 2 -> 85
            fileSize <= targetSize * 4 -> 70
            fileSize <= targetSize * 8 -> 60
            else -> 50
        }
    }
    
    /**
     * 判断是否为常见图片格式（JPEG、PNG、WEBP）
     */
    fun isCommonImageFormat(filePath: String): Boolean {
        val format = getImageFormat(filePath)
        return format in listOf(ImageFormat.JPEG, ImageFormat.PNG, ImageFormat.WEBP)
    }
    
    /**
     * 判断是否为透明图片格式（PNG、WEBP、GIF）
     */
    fun isTransparentImageFormat(filePath: String): Boolean {
        val format = getImageFormat(filePath)
        return format in listOf(ImageFormat.PNG, ImageFormat.WEBP, ImageFormat.GIF)
    }
    
    /**
     * 获取图片文件建议的保存路径（根据格式）
     */
    fun getSuggestedSavePath(
        baseDir: String,
        fileName: String,
        format: ImageFormat
    ): String {
        val dir = File(baseDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        val nameWithoutExt = FileUtils.getFileNameWithoutExtension(fileName)
        return File(dir, "$nameWithoutExt.${format.extension}").absolutePath
    }
    
    /**
     * 验证图片文件是否有效（存在且可读）
     */
    fun isValidImageFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.exists() && file.isFile && file.canRead() && isImageFile(filePath)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取图片文件列表（从目录）
     */
    fun getImageFiles(dirPath: String, recursive: Boolean = false): List<File> {
        return FileUtils.listFiles(dirPath, recursive)
            .filter { isImageFile(it.absolutePath) }
    }
    
    /**
     * 按格式分组图片文件
     */
    fun groupImagesByFormat(imageFiles: List<File>): Map<ImageFormat, List<File>> {
        return imageFiles
            .mapNotNull { file ->
                getImageFormat(file.absolutePath)?.let { format ->
                    format to file
                }
            }
            .groupBy({ it.first }, { it.second })
    }
}

/**
 * 写入字节数组到文件（内部辅助方法）
 */
private fun writeBytesToFile(filePath: String, bytes: ByteArray): Boolean {
    return try {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        true
    } catch (e: Exception) {
        false
    }
}


