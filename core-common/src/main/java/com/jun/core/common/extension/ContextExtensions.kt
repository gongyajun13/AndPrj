package com.jun.core.common.extension

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

/**
 * Context 扩展函数集合
 */

/**
 * 获取字符串资源（安全）
 */
fun Context.getStringSafe(@StringRes resId: Int, vararg formatArgs: Any): String {
    return try {
        getString(resId, *formatArgs)
    } catch (e: Exception) {
        ""
    }
}

/**
 * 获取颜色资源
 */
fun Context.getColorCompat(@ColorRes resId: Int): Int {
    return ContextCompat.getColor(this, resId)
}

/**
 * 获取尺寸资源（返回像素值）
 */
fun Context.getDimenPx(@DimenRes resId: Int): Float {
    return resources.getDimension(resId)
}

/**
 * dp 转 px
 */
fun Context.dpToPx(dp: Float): Int {
    return (dp * resources.displayMetrics.density).toInt()
}

/**
 * px 转 dp
 */
fun Context.pxToDp(px: Float): Int {
    return (px / resources.displayMetrics.density).toInt()
}

/**
 * sp 转 px
 */
fun Context.spToPx(sp: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        resources.displayMetrics
    ).toInt()
}

/**
 * px 转 sp
 */
@Suppress("DEPRECATION")
fun Context.pxToSp(px: Float): Int {
    return (px / resources.displayMetrics.scaledDensity).toInt()
}

/**
 * 获取屏幕宽度（像素）
 */
fun Context.getScreenWidth(): Int {
    return resources.displayMetrics.widthPixels
}

/**
 * 获取屏幕高度（像素）
 */
fun Context.getScreenHeight(): Int {
    return resources.displayMetrics.heightPixels
}

/**
 * 获取屏幕密度
 */
fun Context.getScreenDensity(): Float {
    return resources.displayMetrics.density
}

/**
 * 获取状态栏高度
 */
fun Context.getStatusBarHeight(): Int {
    var result = 0
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = resources.getDimensionPixelSize(resourceId)
    }
    return result
}

/**
 * 获取导航栏高度
 */
fun Context.getNavigationBarHeight(): Int {
    var result = 0
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = resources.getDimensionPixelSize(resourceId)
    }
    return result
}

/**
 * 检查是否为平板设备
 */
fun Context.isTablet(): Boolean {
    val configuration = resources.configuration
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val smallestWidthDp = minOf(screenWidthDp, screenHeightDp)
    return smallestWidthDp >= 600
}

/**
 * 检查是否为横屏
 */
fun Context.isLandscape(): Boolean {
    return resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
}

/**
 * 检查是否为竖屏
 */
fun Context.isPortrait(): Boolean {
    return resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
}

/**
 * Resources 扩展：dp 转 px
 */
fun Resources.dpToPx(dp: Float): Int {
    return (dp * displayMetrics.density).toInt()
}

/**
 * Resources 扩展：px 转 dp
 */
fun Resources.pxToDp(px: Float): Int {
    return (px / displayMetrics.density).toInt()
}

/**
 * Resources 扩展：sp 转 px
 */
fun Resources.spToPx(sp: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        displayMetrics
    ).toInt()
}

/**
 * View 扩展：dp 转 px
 */
fun View.dpToPx(dp: Float): Int {
    return context.dpToPx(dp)
}

/**
 * View 扩展：px 转 dp
 */
fun View.pxToDp(px: Float): Int {
    return context.pxToDp(px)
}

/**
 * View 扩展：sp 转 px
 */
fun View.spToPx(sp: Float): Int {
    return context.spToPx(sp)
}

