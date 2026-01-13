package com.jun.core.ui.extension

import android.widget.ImageView
import coil.load
import coil.request.ImageRequest
import coil.size.Scale

/**
 * 图片加载扩展函数（基于 Coil）
 */

/**
 * 加载网络图片
 */
fun ImageView.loadUrl(
    url: String?,
    placeholder: Int? = null,
    error: Int? = null,
    crossfade: Boolean = true
) {
    if (url.isNullOrBlank()) {
        error?.let { setImageResource(it) }
        return
    }
    
    load(url) {
        placeholder?.let { placeholder(it) }
        error?.let { error(it) }
        if (crossfade) {
            crossfade(true)
        }
        scale(Scale.FIT)
    }
}

/**
 * 加载本地资源图片
 */
fun ImageView.loadResource(
    resId: Int,
    placeholder: Int? = null,
    error: Int? = null
) {
    load(resId) {
        placeholder?.let { placeholder(it) }
        error?.let { error(it) }
    }
}

/**
 * 加载圆形图片
 */
fun ImageView.loadCircle(
    url: String?,
    placeholder: Int? = null,
    error: Int? = null
) {
    if (url.isNullOrBlank()) {
        error?.let { setImageResource(it) }
        return
    }
    
    load(url) {
        placeholder?.let { placeholder(it) }
        error?.let { error(it) }
        transformations(coil.transform.CircleCropTransformation())
    }
}

/**
 * 加载圆角图片
 */
fun ImageView.loadRounded(
    url: String?,
    radius: Float,
    placeholder: Int? = null,
    error: Int? = null
) {
    if (url.isNullOrBlank()) {
        error?.let { setImageResource(it) }
        return
    }
    
    load(url) {
        placeholder?.let { placeholder(it) }
        error?.let { error(it) }
        transformations(coil.transform.RoundedCornersTransformation(radius))
    }
}

/**
 * 清除图片
 */
fun ImageView.clear() {
    setImageDrawable(null)
}

