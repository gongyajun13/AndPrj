package com.jun.core.common.util

import com.jun.core.common.extension.formatFileSize
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 文件工具类
 */
object FileUtils {
    
    /**
     * 检查文件是否存在
     */
    fun exists(filePath: String): Boolean {
        return File(filePath).exists()
    }
    
    /**
     * 创建目录（如果不存在）
     */
    fun createDirectory(dirPath: String): Boolean {
        return try {
            val dir = File(dirPath)
            if (!dir.exists()) {
                dir.mkdirs()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除文件或目录
     */
    fun delete(filePath: String): Boolean {
        return try {
            File(filePath).deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取文件大小（字节）
     */
    fun getFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 获取文件扩展名
     */
    fun getFileExtension(filePath: String): String {
        val fileName = File(filePath).name
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
            fileName.substring(lastDotIndex + 1).lowercase()
        } else {
            ""
        }
    }
    
    /**
     * 获取文件名（不含扩展名）
     */
    fun getFileNameWithoutExtension(filePath: String): String {
        val fileName = File(filePath).name
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            fileName.substring(0, lastDotIndex)
        } else {
            fileName
        }
    }
    
    /**
     * 复制文件
     */
    fun copyFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            
            // 创建目标目录
            destFile.parentFile?.mkdirs()
            
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 读取文件内容为字符串
     */
    fun readFileAsString(filePath: String, charset: String = "UTF-8"): String? {
        return try {
            File(filePath).readText(charset(charset))
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 写入字符串到文件
     */
    fun writeStringToFile(filePath: String, content: String, charset: String = "UTF-8"): Boolean {
        return try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            file.writeText(content, charset(charset))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取目录下的所有文件
     */
    fun listFiles(dirPath: String, recursive: Boolean = false): List<File> {
        return try {
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory) {
                if (recursive) {
                    dir.walk().filter { it.isFile }.toList()
                } else {
                    dir.listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 格式化文件大小
     */
    fun formatFileSize(size: Long): String {
        return size.formatFileSize()
    }
}

/**
 * File 扩展函数
 */

/**
 * 获取文件扩展名
 */
fun File.getExtension(): String {
    return FileUtils.getFileExtension(absolutePath)
}

/**
 * 获取文件名（不含扩展名）
 */
fun File.getNameWithoutExtension(): String {
    return FileUtils.getFileNameWithoutExtension(absolutePath)
}

/**
 * 格式化文件大小
 */
fun File.formatSize(): String {
    return length().formatFileSize()
}

