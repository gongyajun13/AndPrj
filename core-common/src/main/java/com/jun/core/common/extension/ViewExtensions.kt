package com.jun.core.common.extension

import android.view.View

/**
 * 扩展函数：显示 View
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * 扩展函数：隐藏 View
 */
fun View.hide() {
    visibility = View.GONE
}

/**
 * 扩展函数：设置 View 可见性
 */
fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

/**
 * 扩展函数：设置 View 是否可见（INVISIBLE）
 */
fun View.setInvisible(invisible: Boolean) {
    visibility = if (invisible) View.INVISIBLE else View.VISIBLE
}

/**
 * 扩展函数：切换 View 可见性
 */
fun View.toggleVisibility() {
    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

