package com.jun.core.common.util

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

/**
 * 资源提供者接口
 * 用于在非 Context 环境中访问资源
 */
interface ResourceProvider {
    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
    fun getStringArray(@ArrayRes resId: Int): Array<String>
    fun getColor(@ColorRes resId: Int): Int
    fun getDimension(@DimenRes resId: Int): Float
    fun getQuantityString(@PluralsRes resId: Int, quantity: Int): String
    fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any): String
}

/**
 * Context 资源提供者实现
 */
class ContextResourceProvider(
    private val context: Context
) : ResourceProvider {
    
    override fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }
    
    override fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }
    
    override fun getStringArray(@ArrayRes resId: Int): Array<String> {
        return context.resources.getStringArray(resId)
    }
    
    override fun getColor(@ColorRes resId: Int): Int {
        return ContextCompat.getColor(context, resId)
    }
    
    override fun getDimension(@DimenRes resId: Int): Float {
        return context.resources.getDimension(resId)
    }
    
    override fun getQuantityString(@PluralsRes resId: Int, quantity: Int): String {
        return context.resources.getQuantityString(resId, quantity)
    }
    
    override fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any): String {
        return context.resources.getQuantityString(resId, quantity, *formatArgs)
    }
}

