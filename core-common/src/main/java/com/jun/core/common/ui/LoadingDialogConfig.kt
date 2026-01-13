package com.jun.core.common.ui

import android.graphics.Color

/**
 * Loading Dialog 样式配置
 * 用于自定义 loading 对话框的外观和行为
 * 
 * 使用示例：
 * ```kotlin
 * val config = LoadingDialogConfig(
 *     overlayColor = Color.parseColor("#80000000"),
 *     backgroundColor = Color.WHITE,
 *     cornerRadius = 16f,
 *     elevation = 12f,
 *     progressBarSize = 56,
 *     messageTextSize = 15f,
 *     padding = 40,
 *     minWidth = 140
 * )
 * 
 * LoadingManager.dialogFactory = { config ->
 *     CustomLoadingDialog(config)
 * }
 * ```
 */
data class LoadingDialogConfig(
    /**
     * 遮罩层颜色（ARGB）
     * 默认：半透明黑色 #80000000
     */
    val overlayColor: Int = Color.parseColor("#80000000"),
    
    /**
     * 对话框背景颜色（ARGB）
     * 默认：白色 #FFFFFF
     */
    val backgroundColor: Int = Color.WHITE,
    
    /**
     * 圆角半径（dp）
     * 默认：16dp
     */
    val cornerRadius: Float = 16f,
    
    /**
     * 阴影高度（dp）
     * 默认：12dp
     */
    val elevation: Float = 12f,
    
    /**
     * 对话框透明度（0.0 - 1.0）
     * 默认：0.98
     */
    val alpha: Float = 0.98f,
    
    /**
     * ProgressBar 尺寸（dp）
     * 默认：56dp
     */
    val progressBarSize: Int = 56,
    
    /**
     * ProgressBar 颜色（ARGB）
     * null 表示使用主题色 ?attr/colorPrimary
     * 默认：null
     */
    val progressBarColor: Int? = null,
    
    /**
     * 消息文本大小（sp）
     * 默认：15sp
     */
    val messageTextSize: Float = 15f,
    
    /**
     * 消息文本颜色（ARGB）
     * null 表示使用系统主题色 ?android:attr/textColorPrimary
     * 默认：null
     */
    val messageTextColor: Int? = null,
    
    /**
     * 消息文本行间距（dp）
     * 默认：2dp
     */
    val messageLineSpacing: Float = 2f,
    
    /**
     * 消息文本字间距
     * 默认：0.02
     */
    val messageLetterSpacing: Float = 0.02f,
    
    /**
     * 内边距（dp）
     * 默认：40dp
     */
    val padding: Int = 40,
    
    /**
     * ProgressBar 和消息之间的间距（dp）
     * 默认：20dp
     */
    val progressBarMessageSpacing: Int = 20,
    
    /**
     * 对话框最小宽度（dp）
     * 默认：140dp
     */
    val minWidth: Int = 140,
    
    /**
     * 默认消息文本
     * 默认："加载中..."
     */
    val defaultMessage: String = "加载中...",
    
    /**
     * 是否显示消息文本
     * 默认：true
     */
    val showMessage: Boolean = true,
    
    /**
     * 窗口动画样式资源 ID
     * 0 表示使用默认动画
     * 默认：0
     */
    val windowAnimationStyle: Int = 0,
    
    /**
     * 自定义布局资源 ID
     * 0 表示使用默认布局
     * 如果设置了自定义布局，需要确保布局中包含以下 ID：
     * - progressBar (ProgressBar)
     * - tvMessage (TextView，可选，如果 showMessage = false）
     * 默认：0
     */
    val customLayoutResId: Int = 0
) {
    companion object {
        /**
         * 默认配置
         */
        @JvmStatic
        val DEFAULT = LoadingDialogConfig()
        
        /**
         * 深色模式配置
         */
        @JvmStatic
        val DARK = LoadingDialogConfig(
            overlayColor = Color.parseColor("#80000000"),
            backgroundColor = Color.parseColor("#2C2C2C"),
            messageTextColor = Color.parseColor("#E0E0E0")
        )
        
        /**
         * 简约配置（小尺寸，无消息）
         */
        @JvmStatic
        val MINIMAL = LoadingDialogConfig(
            progressBarSize = 48,
            padding = 32,
            minWidth = 120,
            showMessage = false,
            progressBarMessageSpacing = 0
        )
        
        /**
         * 大尺寸配置
         */
        @JvmStatic
        val LARGE = LoadingDialogConfig(
            progressBarSize = 72,
            messageTextSize = 16f,
            padding = 48,
            minWidth = 180,
            progressBarMessageSpacing = 24
        )
    }
    
}

