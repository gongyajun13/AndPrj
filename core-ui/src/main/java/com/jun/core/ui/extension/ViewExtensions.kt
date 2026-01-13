package com.jun.core.ui.extension

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * View 扩展函数集合
 * 
 * 提供 View 相关的常用操作：
 * - 动画效果（显示/隐藏、淡入淡出、滑动等）
 * - 点击防抖
 * - 属性设置（尺寸、边距、透明度等）
 * - 视图转图片并保存到相册
 */

// ==================== 动画相关 ====================

/**
 * 显示 View（带动画）
 * 
 * @param duration 动画时长（毫秒），默认 300ms
 */
fun View.showWithAnimation(duration: Long = 300) {
    if (visibility == View.VISIBLE) return
    
    visibility = View.VISIBLE
    alpha = 0f
    
    animate()
        .alpha(1f)
        .setDuration(duration)
        .setListener(null)
        .start()
}

/**
 * 隐藏 View（带动画）
 * 
 * @param duration 动画时长（毫秒），默认 300ms
 * @param onEnd 动画结束回调
 */
fun View.hideWithAnimation(duration: Long = 300, onEnd: (() -> Unit)? = null) {
    if (visibility == View.GONE) {
        onEnd?.invoke()
        return
    }
    
    animate()
        .alpha(0f)
        .setDuration(duration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = View.GONE
                alpha = 1f
                onEnd?.invoke()
            }
        })
        .start()
}

/**
 * 淡入动画
 * 
 * @param duration 动画时长（毫秒），默认 300ms
 * @param onEnd 动画结束回调
 */
fun View.fadeIn(duration: Long = 300, onEnd: (() -> Unit)? = null) {
    if (visibility == View.VISIBLE && alpha == 1f) {
        onEnd?.invoke()
        return
    }
    
    visibility = View.VISIBLE
    alpha = 0f
    
    animate()
        .alpha(1f)
        .setDuration(duration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        .start()
}

/**
 * 淡出动画
 * 
 * @param duration 动画时长（毫秒），默认 300ms
 * @param onEnd 动画结束回调
 */
fun View.fadeOut(duration: Long = 300, onEnd: (() -> Unit)? = null) {
    animate()
        .alpha(0f)
        .setDuration(duration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = View.GONE
                alpha = 1f
                onEnd?.invoke()
            }
        })
        .start()
}

/**
 * 滑动显示
 * 
 * @param duration 动画时长（毫秒），默认 300ms
 * @param fromBottom 是否从底部滑入，true 为从底部，false 为从顶部
 */
fun View.slideIn(duration: Long = 300, fromBottom: Boolean = true) {
    if (visibility == View.VISIBLE) return
    
    visibility = View.VISIBLE
    val startY = if (fromBottom) height.toFloat() else -height.toFloat()
    translationY = startY
    alpha = 0f
    
    animate()
        .translationY(0f)
        .alpha(1f)
        .setDuration(duration)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .start()
}

/**
 * 滑动隐藏
 * 
 * @param duration 动画时长（毫秒），默认 300ms
 * @param toBottom 是否滑向底部，true 为滑向底部，false 为滑向顶部
 * @param onEnd 动画结束回调
 */
fun View.slideOut(duration: Long = 300, toBottom: Boolean = true, onEnd: (() -> Unit)? = null) {
    val endY = if (toBottom) height.toFloat() else -height.toFloat()
    
    animate()
        .translationY(endY)
        .alpha(0f)
        .setDuration(duration)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = View.GONE
                translationY = 0f
                alpha = 1f
                onEnd?.invoke()
            }
        })
        .start()
}

// ==================== 点击防抖 ====================

/**
 * 点击防抖（时间戳版本）
 * 
 * @param debounceTime 防抖时间（毫秒），默认 500ms
 * @param onClick 点击回调
 */
fun View.setOnClickListenerDebounced(
    debounceTime: Long = 500,
    onClick: (View) -> Unit
) {
    var lastClickTime = 0L
    
    setOnClickListener { view ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > debounceTime) {
            lastClickTime = currentTime
            onClick(view)
        }
    }
}

/**
 * 点击防抖（协程版本）
 * 
 * @param scope 协程作用域
 * @param debounceTime 防抖时间（毫秒），默认 500ms
 * @param onClick 点击回调
 */
fun View.setOnClickListenerDebounced(
    scope: CoroutineScope,
    debounceTime: Long = 500,
    onClick: (View) -> Unit
) {
    var job: Job? = null
    
    setOnClickListener { view ->
        job?.cancel()
        job = scope.launch {
            delay(debounceTime)
            onClick(view)
        }
    }
}

/**
 * 设置点击监听器（带防抖）
 * 
 * @param debounceTime 防抖时间（毫秒），默认 500ms
 * @param onClick 点击回调
 */
fun View.click(debounceTime: Long = 500, onClick: (View) -> Unit) {
    setOnClickListenerDebounced(debounceTime, onClick)
}

/**
 * 设置长按监听器
 * 
 * @param onLongClick 长按回调，返回 true 表示已处理
 */
fun View.longClick(onLongClick: (View) -> Boolean) {
    setOnLongClickListener(onLongClick)
}

// ==================== 可见性控制 ====================

/**
 * 设置可见性（带动画）
 * 
 * @param visible 是否可见
 * @param duration 动画时长（毫秒），默认 300ms
 */
fun View.setVisibleWithAnimation(visible: Boolean, duration: Long = 300) {
    if (visible) {
        showWithAnimation(duration)
    } else {
        hideWithAnimation(duration)
    }
}

/**
 * 设置可见性（无动画）
 */
fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

/**
 * 设置可见性为 GONE
 */
fun View.setGone() {
    visibility = View.GONE
}

/**
 * 设置可见性为 INVISIBLE
 */
fun View.setInvisible() {
    visibility = View.INVISIBLE
}

// ==================== 属性设置 ====================

/**
 * 设置透明度
 * 
 * @param alpha 透明度值（0.0-1.0）
 * @param duration 动画时长（毫秒），0 表示无动画
 */
fun View.setAlpha(alpha: Float, duration: Long = 0) {
    if (duration > 0) {
        animate().alpha(alpha).setDuration(duration).start()
    } else {
        this.alpha = alpha
    }
}

/**
 * 设置缩放
 * 
 * @param scaleX X 轴缩放比例
 * @param scaleY Y 轴缩放比例
 * @param duration 动画时长（毫秒），0 表示无动画
 */
fun View.setScale(scaleX: Float, scaleY: Float, duration: Long = 0) {
    if (duration > 0) {
        animate()
            .scaleX(scaleX)
            .scaleY(scaleY)
            .setDuration(duration)
            .start()
    } else {
        this.scaleX = scaleX
        this.scaleY = scaleY
    }
}

/**
 * 设置旋转
 * 
 * @param rotation 旋转角度（度）
 * @param duration 动画时长（毫秒），0 表示无动画
 */
fun View.setRotation(rotation: Float, duration: Long = 0) {
    if (duration > 0) {
        animate().rotation(rotation).setDuration(duration).start()
    } else {
        this.rotation = rotation
    }
}

/**
 * 设置平移
 * 
 * @param x X 轴平移距离
 * @param y Y 轴平移距离
 * @param duration 动画时长（毫秒），0 表示无动画
 */
fun View.setTranslation(x: Float = 0f, y: Float = 0f, duration: Long = 0) {
    if (duration > 0) {
        animate()
            .translationX(x)
            .translationY(y)
            .setDuration(duration)
            .start()
    } else {
        translationX = x
        translationY = y
    }
}

/**
 * 设置 View 的宽度
 * 
 * @param width 宽度（像素）
 */
fun View.setWidth(width: Int) {
    val params = layoutParams
    params.width = width
    layoutParams = params
}

/**
 * 设置 View 的高度
 * 
 * @param height 高度（像素）
 */
fun View.setHeight(height: Int) {
    val params = layoutParams
    params.height = height
    layoutParams = params
}

/**
 * 设置 View 的尺寸
 * 
 * @param width 宽度（像素）
 * @param height 高度（像素）
 */
fun View.setSize(width: Int, height: Int) {
    val params = layoutParams
    params.width = width
    params.height = height
    layoutParams = params
}

/**
 * 设置 View 的边距
 * 
 * @param left 左边距（像素）
 * @param top 上边距（像素）
 * @param right 右边距（像素）
 * @param bottom 下边距（像素）
 */
fun View.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
    val params = layoutParams as? ViewGroup.MarginLayoutParams
    params?.setMargins(left, top, right, bottom)
    layoutParams = params
}

/**
 * 设置 View 的内边距（四个方向相同）
 * 
 * @param padding 内边距（像素）
 */
fun View.setPadding(padding: Int) {
    setPadding(padding, padding, padding, padding)
}

/**
 * 设置 View 的背景透明度
 * 
 * @param alpha 透明度值（0.0-1.0）
 */
fun View.setBackgroundAlpha(alpha: Float) {
    background?.alpha = (alpha * 255).toInt()
}

/**
 * 启用/禁用 View
 * 
 * @param enabled 是否启用
 * @param alphaWhenDisabled 禁用时的透明度，默认 0.5f
 */
fun View.setEnabled(enabled: Boolean, alphaWhenDisabled: Float = 0.5f) {
    isEnabled = enabled
    this.alpha = if (enabled) 1f else alphaWhenDisabled
}

/**
 * 添加点击波纹效果
 * 注意：需要在主题中设置 ?attr/selectableItemBackground
 */
fun View.addRippleEffect() {
    isClickable = true
    isFocusable = true
}

// ==================== 位置和可见性检查 ====================

/**
 * 获取 View 在屏幕中的位置
 * 
 * @return Pair<X坐标, Y坐标>
 */
fun View.getLocationOnScreen(): Pair<Int, Int> {
    val location = IntArray(2)
    getLocationOnScreen(location)
    return Pair(location[0], location[1])
}

/**
 * 检查 View 是否在屏幕中可见
 * 
 * @return true 表示可见，false 表示不可见
 */
fun View.isVisibleOnScreen(): Boolean {
    if (!isVisible) return false
    
    val (x, y) = getLocationOnScreen()
    val screenWidth = context.resources.displayMetrics.widthPixels
    val screenHeight = context.resources.displayMetrics.heightPixels
    
    return x >= 0 && y >= 0 && 
           x + width <= screenWidth && 
           y + height <= screenHeight
}

// ==================== 视图转图片 ====================

/**
 * 将 View 转换为 Bitmap
 * 
 * @param config Bitmap 配置，默认 ARGB_8888
 * @return Bitmap 对象，失败返回 null
 */
fun View.toBitmap(config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
    return try {
        // 确保 View 已经完成布局
        if (width <= 0 || height <= 0) {
            measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            layout(0, 0, measuredWidth, measuredHeight)
        }
        
        val bitmap = Bitmap.createBitmap(width, height, config)
        val canvas = Canvas(bitmap)
        draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}

/**
 * 将 View 转换为 Bitmap（指定尺寸）
 * 
 * @param width 目标宽度（像素）
 * @param height 目标高度（像素）
 * @param config Bitmap 配置，默认 ARGB_8888
 * @return Bitmap 对象，失败返回 null
 */
fun View.toBitmap(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
    return try {
        // 测量并布局 View
        measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, width, height)
        
        val bitmap = Bitmap.createBitmap(width, height, config)
        val canvas = Canvas(bitmap)
        draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}

// ==================== 保存到相册 ====================

/**
 * 保存 Bitmap 到相册
 * 
 * @param context Context 对象
 * @param bitmap 要保存的 Bitmap
 * @param displayName 显示名称（不含扩展名）
 * @param mimeType MIME 类型，默认 "image/jpeg"
 * @param quality 图片质量（0-100），仅对 JPEG 有效，默认 90
 * @param relativePath 相对路径，Android 10+ 使用，默认 Environment.DIRECTORY_PICTURES
 * @return 保存的图片 URI，失败返回 null
 */
fun saveBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
    displayName: String = "image_${System.currentTimeMillis()}",
    mimeType: String = "image/jpeg",
    quality: Int = 90,
    relativePath: String = Environment.DIRECTORY_PICTURES
): Uri? {
    return try {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            
            // Android 10+ 使用 RELATIVE_PATH
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null
        
        // 写入图片数据
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val format = when (mimeType) {
                "image/png" -> Bitmap.CompressFormat.PNG
                "image/webp" -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }
            bitmap.compress(format, quality, outputStream)
        }
        
        // Android 10+ 需要更新 IS_PENDING 状态
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
        }
        
        // Android 10+ 使用 MediaStore API，不需要手动通知媒体库
        // Android 9 及以下版本，MediaStore.insert 会自动触发媒体库扫描
        
        uri
    } catch (e: Exception) {
        null
    }
}

/**
 * 将 View 转换为图片并保存到相册
 * 
 * @param context Context 对象
 * @param displayName 显示名称（不含扩展名），默认使用时间戳
 * @param mimeType MIME 类型，默认 "image/jpeg"
 * @param quality 图片质量（0-100），仅对 JPEG 有效，默认 90
 * @param relativePath 相对路径，Android 10+ 使用，默认 Environment.DIRECTORY_PICTURES
 * @return 保存的图片 URI，失败返回 null
 */
fun View.saveToGallery(
    context: Context,
    displayName: String = "view_${System.currentTimeMillis()}",
    mimeType: String = "image/jpeg",
    quality: Int = 90,
    relativePath: String = Environment.DIRECTORY_PICTURES
): Uri? {
    val bitmap = toBitmap() ?: return null
    return saveBitmapToGallery(context, bitmap, displayName, mimeType, quality, relativePath)
}

/**
 * 将 View 转换为图片并保存到相册（指定尺寸）
 * 
 * @param context Context 对象
 * @param width 目标宽度（像素）
 * @param height 目标高度（像素）
 * @param displayName 显示名称（不含扩展名），默认使用时间戳
 * @param mimeType MIME 类型，默认 "image/jpeg"
 * @param quality 图片质量（0-100），仅对 JPEG 有效，默认 90
 * @param relativePath 相对路径，Android 10+ 使用，默认 Environment.DIRECTORY_PICTURES
 * @return 保存的图片 URI，失败返回 null
 */
fun View.saveToGallery(
    context: Context,
    width: Int,
    height: Int,
    displayName: String = "view_${System.currentTimeMillis()}",
    mimeType: String = "image/jpeg",
    quality: Int = 90,
    relativePath: String = Environment.DIRECTORY_PICTURES
): Uri? {
    val bitmap = toBitmap(width, height) ?: return null
    return saveBitmapToGallery(context, bitmap, displayName, mimeType, quality, relativePath)
}

/**
 * 将 View 的指定区域转换为图片并保存到相册
 * 
 * @param context Context 对象
 * @param left 裁剪区域左边界（像素），相对于 View 的左上角
 * @param top 裁剪区域上边界（像素），相对于 View 的左上角
 * @param right 裁剪区域右边界（像素），相对于 View 的左上角
 * @param bottom 裁剪区域下边界（像素），相对于 View 的左上角
 * @param displayName 显示名称（不含扩展名），默认使用时间戳
 * @param mimeType MIME 类型，默认 "image/jpeg"
 * @param quality 图片质量（0-100），仅对 JPEG 有效，默认 90
 * @param relativePath 相对路径，Android 10+ 使用，默认 Environment.DIRECTORY_PICTURES
 * @return 保存的图片 URI，失败返回 null
 */
fun View.saveRegionToGallery(
    context: Context,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    displayName: String = "view_region_${System.currentTimeMillis()}",
    mimeType: String = "image/jpeg",
    quality: Int = 90,
    relativePath: String = Environment.DIRECTORY_PICTURES
): Uri? {
    // 验证参数
    if (left < 0 || top < 0 || right <= left || bottom <= top) {
        return null
    }
    
    // 获取完整的 View Bitmap
    val fullBitmap = toBitmap() ?: return null
    
    // 验证裁剪区域是否在 Bitmap 范围内
    val bitmapWidth = fullBitmap.width
    val bitmapHeight = fullBitmap.height
    
    if (right > bitmapWidth || bottom > bitmapHeight) {
        return null
    }
    
    // 裁剪 Bitmap
    val croppedBitmap = try {
        Bitmap.createBitmap(
            fullBitmap,
            left,
            top,
            right - left,
            bottom - top
        )
    } catch (e: Exception) {
        null
    } ?: return null
    
    // 保存到相册
    return saveBitmapToGallery(
        context = context,
        bitmap = croppedBitmap,
        displayName = displayName,
        mimeType = mimeType,
        quality = quality,
        relativePath = relativePath
    )
}

/**
 * 将 View 的指定区域转换为 Bitmap
 * 
 * @param left 裁剪区域左边界（像素），相对于 View 的左上角
 * @param top 裁剪区域上边界（像素），相对于 View 的左上角
 * @param right 裁剪区域右边界（像素），相对于 View 的左上角
 * @param bottom 裁剪区域下边界（像素），相对于 View 的左上角
 * @param config Bitmap 配置，默认 ARGB_8888
 * @return 裁剪后的 Bitmap 对象，失败返回 null
 */
fun View.toBitmapRegion(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888
): Bitmap? {
    // 验证参数
    if (left < 0 || top < 0 || right <= left || bottom <= top) {
        return null
    }
    
    // 获取完整的 View Bitmap
    val fullBitmap = toBitmap(config) ?: return null
    
    // 验证裁剪区域是否在 Bitmap 范围内
    val bitmapWidth = fullBitmap.width
    val bitmapHeight = fullBitmap.height
    
    if (right > bitmapWidth || bottom > bitmapHeight) {
        return null
    }
    
    // 裁剪 Bitmap
    return try {
        Bitmap.createBitmap(
            fullBitmap,
            left,
            top,
            right - left,
            bottom - top
        )
    } catch (e: Exception) {
        null
    }
}

// ==================== 图片压缩 ====================

/**
 * 压缩结果
 */
data class CompressResult(
    val bitmap: Bitmap,
    val originalSize: Long,
    val compressedSize: Long,
    val compressionRatio: Float
)

/**
 * 按尺寸压缩 Bitmap（保持宽高比）
 * 
 * @param maxWidth 最大宽度（像素）
 * @param maxHeight 最大高度（像素）
 * @param config Bitmap 配置，默认 ARGB_8888
 * @return 压缩后的 Bitmap，失败返回 null
 */
fun Bitmap.compressBySize(
    maxWidth: Int,
    maxHeight: Int,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888
): Bitmap? {
    return try {
        val originalWidth = width
        val originalHeight = height
        
        // 如果尺寸已经小于目标尺寸，直接返回
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return this
        }
        
        // 计算缩放比例
        val scaleWidth = maxWidth.toFloat() / originalWidth
        val scaleHeight = maxHeight.toFloat() / originalHeight
        val scale = minOf(scaleWidth, scaleHeight)
        
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        
        Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
    } catch (e: Exception) {
        null
    }
}

/**
 * 按比例压缩 Bitmap
 * 
 * @param scale 缩放比例（0.0-1.0），例如 0.5 表示压缩到原来的 50%
 * @param config Bitmap 配置，默认 ARGB_8888
 * @return 压缩后的 Bitmap，失败返回 null
 */
fun Bitmap.compressByScale(
    scale: Float,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888
): Bitmap? {
    return try {
        if (scale <= 0f || scale >= 1f) {
            return this
        }
        
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()
        
        if (scaledWidth <= 0 || scaledHeight <= 0) {
            return null
        }
        
        Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
    } catch (e: Exception) {
        null
    }
}

/**
 * 按质量压缩 Bitmap（通过降低压缩质量）
 * 
 * @param quality 压缩质量（0-100），数值越小压缩率越高
 * @param format 压缩格式，默认 JPEG
 * @return 压缩后的 Bitmap（重新解码），失败返回 null
 */
fun Bitmap.compressByQuality(
    quality: Int,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
): Bitmap? {
    return try {
        if (quality < 0 || quality > 100) {
            return this
        }
        
        // 将 Bitmap 压缩到 ByteArray
        val outputStream = java.io.ByteArrayOutputStream()
        compress(format, quality, outputStream)
        val compressedData = outputStream.toByteArray()
        outputStream.close()
        
        // 重新解码为 Bitmap
        android.graphics.BitmapFactory.decodeByteArray(compressedData, 0, compressedData.size)
    } catch (e: Exception) {
        null
    }
}

/**
 * 按文件大小压缩 Bitmap（循环压缩直到达到目标大小）
 * 
 * @param targetSizeBytes 目标文件大小（字节）
 * @param format 压缩格式，默认 JPEG
 * @param minQuality 最低质量（0-100），默认 20
 * @return 压缩结果，失败返回 null
 */
fun Bitmap.compressByFileSize(
    targetSizeBytes: Long,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    minQuality: Int = 20
): CompressResult? {
    return try {
        val originalSize = getByteCount().toLong()
        
        // 如果已经小于目标大小，直接返回
        if (originalSize <= targetSizeBytes) {
            return CompressResult(
                bitmap = this,
                originalSize = originalSize,
                compressedSize = originalSize,
                compressionRatio = 1f
            )
        }
        
        // 先尝试按尺寸压缩
        var compressedBitmap = this
        var currentSize = originalSize
        
        // 如果文件大小仍然太大，按比例缩小尺寸
        if (currentSize > targetSizeBytes * 2) {
            val scale = kotlin.math.sqrt(targetSizeBytes.toFloat() / currentSize).coerceAtLeast(0.5f)
            compressedBitmap = compressByScale(scale) ?: return null
            currentSize = compressedBitmap.getByteCount().toLong()
        }
        
        // 如果仍然太大，降低质量
        if (currentSize > targetSizeBytes) {
            var quality = 90
            var result: CompressResult? = null
            
            while (quality >= minQuality && currentSize > targetSizeBytes) {
                val tempBitmap = compressedBitmap.compressByQuality(quality, format)
                if (tempBitmap == null) break
                
                val tempSize = tempBitmap.getByteCount().toLong()
                result = CompressResult(
                    bitmap = tempBitmap,
                    originalSize = originalSize,
                    compressedSize = tempSize,
                    compressionRatio = tempSize.toFloat() / originalSize
                )
                
                currentSize = tempSize
                compressedBitmap = tempBitmap
                quality -= 10
            }
            
            return result ?: CompressResult(
                bitmap = compressedBitmap,
                originalSize = originalSize,
                compressedSize = currentSize,
                compressionRatio = currentSize.toFloat() / originalSize
            )
        }
        
        CompressResult(
            bitmap = compressedBitmap,
            originalSize = originalSize,
            compressedSize = currentSize,
            compressionRatio = currentSize.toFloat() / originalSize
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * 压缩 Bitmap 并保存到相册
 * 
 * @param context Context 对象
 * @param maxWidth 最大宽度（像素），0 表示不限制
 * @param maxHeight 最大高度（像素），0 表示不限制
 * @param quality 压缩质量（0-100），默认 90
 * @param targetSizeBytes 目标文件大小（字节），0 表示不限制
 * @param displayName 显示名称（不含扩展名）
 * @param mimeType MIME 类型，默认 "image/jpeg"
 * @param relativePath 相对路径，Android 10+ 使用，默认 Environment.DIRECTORY_PICTURES
 * @return 保存的图片 URI，失败返回 null
 */
fun Bitmap.compressAndSaveToGallery(
    context: Context,
    maxWidth: Int = 0,
    maxHeight: Int = 0,
    quality: Int = 90,
    targetSizeBytes: Long = 0,
    displayName: String = "compressed_image_${System.currentTimeMillis()}",
    mimeType: String = "image/jpeg",
    relativePath: String = Environment.DIRECTORY_PICTURES
): Uri? {
    return try {
        var compressedBitmap: Bitmap? = this
        
        // 按尺寸压缩
        if (maxWidth > 0 && maxHeight > 0) {
            compressedBitmap = compressedBitmap?.compressBySize(maxWidth, maxHeight)
        }
        
        // 按文件大小压缩
        if (targetSizeBytes > 0 && compressedBitmap != null) {
            val format = when (mimeType) {
                "image/png" -> Bitmap.CompressFormat.PNG
                "image/webp" -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }
            val result = compressedBitmap.compressByFileSize(targetSizeBytes, format)
            compressedBitmap = result?.bitmap
        } else if (quality < 100 && compressedBitmap != null) {
            // 按质量压缩
            val format = when (mimeType) {
                "image/png" -> Bitmap.CompressFormat.PNG
                "image/webp" -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }
            compressedBitmap = compressedBitmap.compressByQuality(quality, format)
        }
        
        compressedBitmap ?: return null
        
        // 保存到相册
        saveBitmapToGallery(
            context = context,
            bitmap = compressedBitmap,
            displayName = displayName,
            mimeType = mimeType,
            quality = 100, // 已经压缩过了，这里用最高质量保存
            relativePath = relativePath
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * 将 View 转换为图片，压缩后保存到相册
 * 
 * @param context Context 对象
 * @param maxWidth 最大宽度（像素），0 表示不限制
 * @param maxHeight 最大高度（像素），0 表示不限制
 * @param quality 压缩质量（0-100），默认 90
 * @param targetSizeBytes 目标文件大小（字节），0 表示不限制
 * @param displayName 显示名称（不含扩展名）
 * @param mimeType MIME 类型，默认 "image/jpeg"
 * @param relativePath 相对路径，Android 10+ 使用，默认 Environment.DIRECTORY_PICTURES
 * @return 保存的图片 URI，失败返回 null
 */
fun View.compressAndSaveToGallery(
    context: Context,
    maxWidth: Int = 0,
    maxHeight: Int = 0,
    quality: Int = 90,
    targetSizeBytes: Long = 0,
    displayName: String = "compressed_view_${System.currentTimeMillis()}",
    mimeType: String = "image/jpeg",
    relativePath: String = Environment.DIRECTORY_PICTURES
): Uri? {
    val bitmap = toBitmap() ?: return null
    return bitmap.compressAndSaveToGallery(
        context = context,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        quality = quality,
        targetSizeBytes = targetSizeBytes,
        displayName = displayName,
        mimeType = mimeType,
        relativePath = relativePath
    )
}
