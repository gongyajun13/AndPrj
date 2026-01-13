package com.jun.andprj.ui.tool

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jun.andprj.R
import com.jun.andprj.databinding.ActivityImageUtilsDemoBinding
import com.jun.core.common.util.ImageUtils
import com.jun.core.ui.base.BaseActivity
import com.jun.core.ui.extension.compressAndSaveToGallery
import com.jun.core.ui.extension.compressByFileSize
import com.jun.core.ui.extension.compressByQuality
import com.jun.core.ui.extension.compressByScale
import com.jun.core.ui.extension.compressBySize
import com.jun.core.ui.extension.saveBitmapToGallery
import com.jun.core.ui.extension.saveRegionToGallery
import com.jun.core.ui.extension.saveToGallery
import com.jun.core.ui.extension.toBitmap
import com.jun.core.ui.extension.toBitmapRegion
import java.io.File

/**
 * ImageUtils 和 ViewExtensions 图片工具示例
 */
class ImageUtilsDemoActivity : BaseActivity<ActivityImageUtilsDemoBinding>() {

    private lateinit var adapter: ImageUtilsDemoAdapter
    private val demoItems = mutableListOf<ImageUtilsDemoItem>()
    private val testImageDir: File by lazy { File(filesDir, "test_images") }
    private val testImageFile: File by lazy { File(testImageDir, "test_image.jpg") }

    override fun createBinding(): ActivityImageUtilsDemoBinding = ActivityImageUtilsDemoBinding.inflate(layoutInflater)

    override fun setupViews() {
        setupToolbar()
        setupRecyclerView()
        setupButtonListeners()
        refreshDemoInfo()
    }

    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val blue = ContextCompat.getColor(this, R.color.blue)
        setStatusBarColor(white, lightIcons = false)
        binding.toolbar.setupSimple(
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            title = "图片工具示例",
            titleTextColor = white,
            backgroundColor = blue,
            onLeftClick = { finish() }
        )
    }

    private fun setupRecyclerView() {
        adapter = ImageUtilsDemoAdapter(demoItems)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtonListeners() {
        // ImageUtils 功能演示
        binding.btnCreateTestImage.setOnClickListener {
            createTestImage()
        }

        binding.btnGetImageInfo.setOnClickListener {
            refreshDemoInfo()
        }

        binding.btnEncodeBase64.setOnClickListener {
            encodeToBase64()
        }

        binding.btnDecodeBase64.setOnClickListener {
            decodeFromBase64()
        }

        // ViewExtensions 功能演示
        binding.btnViewToBitmap.setOnClickListener {
            viewToBitmap()
        }

        binding.btnViewToBitmapCustomSize.setOnClickListener {
            viewToBitmapCustomSize()
        }

        binding.btnSaveViewToGallery.setOnClickListener {
            saveViewToGallery()
        }

        binding.btnSaveViewRegionToGallery.setOnClickListener {
            saveViewRegionToGallery()
        }

        binding.btnSaveTestViewToGallery.setOnClickListener {
            saveTestViewToGallery()
        }

        // 图片压缩功能
        binding.btnCompressBySize.setOnClickListener {
            compressBySize()
        }

        binding.btnCompressByQuality.setOnClickListener {
            compressByQuality()
        }

        binding.btnCompressByFileSize.setOnClickListener {
            compressByFileSize()
        }

        binding.btnCompressAndSave.setOnClickListener {
            compressAndSave()
        }
    }

    private fun createTestImage() {
        // 创建一个测试图片（通过保存 View 为图片）
        testImageDir.mkdirs()
        val bitmap = binding.cardTestView.toBitmap() ?: run {
            showError("无法创建测试图片")
            return
        }
        
        val uri = saveBitmapToGallery(
            context = this,
            bitmap = bitmap,
            displayName = "test_image",
            mimeType = "image/jpeg",
            quality = 90
        )
        
        if (uri != null) {
            showSuccess("测试图片已创建并保存到相册")
            refreshDemoInfo()
        } else {
            showError("创建测试图片失败")
        }
    }

    private fun refreshDemoInfo() {
        demoItems.clear()
        
        // ImageUtils 功能演示
        val testImagePath = testImageFile.absolutePath
        val imageFormat = ImageUtils.getImageFormat(testImagePath)
        val imageInfo = ImageUtils.getImageInfo(testImagePath)
        val isImage = ImageUtils.isImageFile(testImagePath)
        val isCommonFormat = if (testImageFile.exists()) ImageUtils.isCommonImageFormat(testImagePath) else false
        val isTransparent = if (testImageFile.exists()) ImageUtils.isTransparentImageFormat(testImagePath) else false
        val fileSizeFormatted = if (testImageFile.exists()) ImageUtils.getImageFileSizeFormatted(testImagePath) else "0 B"
        
        demoItems.addAll(
            listOf(
                ImageUtilsDemoItem(
                    "图片格式识别",
                    imageFormat?.name ?: "（文件不存在）",
                    "ImageUtils.getImageFormat(\"${testImagePath}\")"
                ),
                ImageUtilsDemoItem(
                    "是否为图片文件",
                    if (isImage) "是" else "否",
                    "ImageUtils.isImageFile(\"${testImagePath}\")"
                ),
                ImageUtilsDemoItem(
                    "是否为常见格式",
                    if (isCommonFormat) "是" else "否",
                    "ImageUtils.isCommonImageFormat(\"${testImagePath}\")"
                ),
                ImageUtilsDemoItem(
                    "是否为透明格式",
                    if (isTransparent) "是" else "否",
                    "ImageUtils.isTransparentImageFormat(\"${testImagePath}\")"
                ),
                ImageUtilsDemoItem(
                    "文件大小（格式化）",
                    fileSizeFormatted,
                    "ImageUtils.getImageFileSizeFormatted(\"${testImagePath}\")"
                ),
                ImageUtilsDemoItem(
                    "图片信息",
                    if (imageInfo != null) {
                        "格式: ${imageInfo.format?.name ?: "未知"}, " +
                        "大小: ${imageInfo.fileSize} 字节, " +
                        "文件名: ${imageInfo.fileName}"
                    } else {
                        "（文件不存在，请先创建测试图片）"
                    },
                    "ImageUtils.getImageInfo(\"${testImagePath}\")"
                ),
                ImageUtilsDemoItem(
                    "宽高比计算",
                    "16:9 = ${ImageUtils.calculateAspectRatio(1920, 1080)}",
                    "ImageUtils.calculateAspectRatio(1920, 1080)"
                ),
                ImageUtilsDemoItem(
                    "缩放比例计算",
                    "缩放比例: ${ImageUtils.calculateScaleRatio(1920, 1080, 800, 600)}",
                    "ImageUtils.calculateScaleRatio(1920, 1080, 800, 600)"
                ),
                ImageUtilsDemoItem(
                    "缩放后尺寸",
                    ImageUtils.calculateScaledSize(1920, 1080, 800, 600).let {
                        "${it.first} x ${it.second}"
                    },
                    "ImageUtils.calculateScaledSize(1920, 1080, 800, 600)"
                ),
                ImageUtilsDemoItem(
                    "推荐压缩质量",
                    "质量: ${ImageUtils.getRecommendedQuality(2_000_000, 500_000)}",
                    "ImageUtils.getRecommendedQuality(2MB, 500KB)"
                )
            )
        )
        adapter.notifyDataSetChanged()
    }

    private fun encodeToBase64() {
        val bitmap = binding.cardTestView.toBitmap() ?: run {
            showError("无法获取 View 的 Bitmap")
            return
        }
        
        // 先保存为临时文件
        val tempFile = File(cacheDir, "temp_image.jpg")
        tempFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        
        val base64 = ImageUtils.encodeImageToBase64(tempFile.absolutePath)
        if (base64 != null) {
            showSuccess("Base64 编码成功，长度: ${base64.length}")
            refreshDemoInfo()
        } else {
            showError("Base64 编码失败")
        }
    }

    private fun decodeFromBase64() {
        // 这里演示解码功能，实际使用时需要先有 Base64 字符串
        showMessage("解码功能需要 Base64 字符串，请参考代码示例")
    }

    private fun viewToBitmap() {
        val bitmap = binding.cardTestView.toBitmap()
        if (bitmap != null) {
            showSuccess("View 转 Bitmap 成功，尺寸: ${bitmap.width} x ${bitmap.height}")
        } else {
            showError("View 转 Bitmap 失败")
        }
    }

    private fun viewToBitmapCustomSize() {
        val bitmap = binding.cardTestView.toBitmap(400, 300)
        if (bitmap != null) {
            showSuccess("View 转 Bitmap（自定义尺寸）成功，尺寸: ${bitmap.width} x ${bitmap.height}")
        } else {
            showError("View 转 Bitmap（自定义尺寸）失败")
        }
    }

    private fun saveViewToGallery() {
        val uri = binding.cardTestView.saveToGallery(
            context = this,
            displayName = "view_screenshot_${System.currentTimeMillis()}",
            mimeType = "image/jpeg",
            quality = 90
        )
        
        if (uri != null) {
            showSuccess("View 已保存到相册")
        } else {
            showError("保存失败，请检查存储权限")
        }
    }

    private fun saveViewRegionToGallery() {
        val view = binding.cardTestView
        val width = view.width
        val height = view.height
        
        if (width <= 0 || height <= 0) {
            showError("View 尺寸无效，请等待布局完成")
            return
        }
        
        // 裁剪中间区域（留出 20% 的边距）
        val left = (width * 0.2).toInt()
        val top = (height * 0.2).toInt()
        val right = (width * 0.8).toInt()
        val bottom = (height * 0.8).toInt()
        
        val uri = view.saveRegionToGallery(
            context = this,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            displayName = "view_region_${System.currentTimeMillis()}",
            mimeType = "image/png",
            quality = 100
        )
        
        if (uri != null) {
            showSuccess("View 区域已保存到相册（裁剪区域: $left, $top, $right, $bottom）")
        } else {
            showError("保存失败，请检查存储权限")
        }
    }

    private fun saveTestViewToGallery() {
        val uri = binding.cardTestView.saveToGallery(
            context = this,
            width = 800,
            height = 600,
            displayName = "test_view_custom_${System.currentTimeMillis()}",
            mimeType = "image/png",
            quality = 100
        )
        
        if (uri != null) {
            showSuccess("测试 View（自定义尺寸）已保存到相册")
        } else {
            showError("保存失败，请检查存储权限")
        }
    }

    private fun compressBySize() {
        val bitmap = binding.cardTestView.toBitmap() ?: run {
            showError("无法获取 View 的 Bitmap")
            return
        }
        
        val originalSize = bitmap.getByteCount()
        val compressed = bitmap.compressBySize(400, 300)
        
        if (compressed != null) {
            val compressedSize = compressed.getByteCount()
            val ratio = (compressedSize.toFloat() / originalSize * 100).toInt()
            showSuccess("按尺寸压缩成功：${bitmap.width}x${bitmap.height} -> ${compressed.width}x${compressed.height}，压缩率: ${ratio}%")
        } else {
            showError("压缩失败")
        }
    }

    private fun compressByQuality() {
        val bitmap = binding.cardTestView.toBitmap() ?: run {
            showError("无法获取 View 的 Bitmap")
            return
        }
        
        val originalSize = bitmap.getByteCount()
        val compressed = bitmap.compressByQuality(50)
        
        if (compressed != null) {
            val compressedSize = compressed.getByteCount()
            val ratio = (compressedSize.toFloat() / originalSize * 100).toInt()
            showSuccess("按质量压缩成功（质量50），压缩率: ${ratio}%")
        } else {
            showError("压缩失败")
        }
    }

    private fun compressByFileSize() {
        val bitmap = binding.cardTestView.toBitmap() ?: run {
            showError("无法获取 View 的 Bitmap")
            return
        }
        
        val originalSize = bitmap.getByteCount().toLong()
        val result = bitmap.compressByFileSize(100_000) // 目标 100KB
        
        if (result != null) {
            val ratio = (result.compressionRatio * 100).toInt()
            showSuccess("按文件大小压缩成功：${formatFileSize(originalSize)} -> ${formatFileSize(result.compressedSize)}，压缩率: ${ratio}%")
        } else {
            showError("压缩失败")
        }
    }

    private fun compressAndSave() {
        val uri = binding.cardTestView.compressAndSaveToGallery(
            context = this,
            maxWidth = 800,
            maxHeight = 600,
            quality = 80,
            targetSizeBytes = 200_000, // 目标 200KB
            displayName = "compressed_view_${System.currentTimeMillis()}",
            mimeType = "image/jpeg"
        )
        
        if (uri != null) {
            showSuccess("压缩并保存成功")
        } else {
            showError("压缩保存失败，请检查存储权限")
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

data class ImageUtilsDemoItem(
    val title: String,
    val result: String,
    val code: String
)

