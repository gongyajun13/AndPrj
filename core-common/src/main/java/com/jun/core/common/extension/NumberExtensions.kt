package com.jun.core.common.extension

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Number 扩展函数集合
 */

/**
 * Int 扩展：格式化数字（添加千分位分隔符）
 */
fun Int.formatWithCommas(): String {
    return NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
}

/**
 * Long 扩展：格式化数字（添加千分位分隔符）
 */
fun Long.formatWithCommas(): String {
    return NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
}

/**
 * Double 扩展：格式化数字（添加千分位分隔符，保留指定位小数）
 */
fun Double.formatWithCommas(decimalPlaces: Int = 2): String {
    val formatter = DecimalFormat("#,##0.${"0".repeat(decimalPlaces)}")
    return formatter.format(this)
}

/**
 * Float 扩展：格式化数字（添加千分位分隔符，保留指定位小数）
 */
fun Float.formatWithCommas(decimalPlaces: Int = 2): String {
    return toDouble().formatWithCommas(decimalPlaces)
}

/**
 * Int 扩展：格式化文件大小
 */
fun Int.formatFileSize(): String {
    return toLong().formatFileSize()
}

/**
 * Long 扩展：格式化文件大小
 */
fun Long.formatFileSize(): String {
    val kb = 1024L
    val mb = kb * 1024
    val gb = mb * 1024
    val tb = gb * 1024
    
    return when {
        this >= tb -> String.format("%.2f TB", this / tb.toDouble())
        this >= gb -> String.format("%.2f GB", this / gb.toDouble())
        this >= mb -> String.format("%.2f MB", this / mb.toDouble())
        this >= kb -> String.format("%.2f KB", this / kb.toDouble())
        else -> "$this B"
    }
}

/**
 * Int 扩展：格式化货币
 */
fun Int.formatCurrency(symbol: String = "¥"): String {
    return "$symbol${formatWithCommas()}"
}

/**
 * Long 扩展：格式化货币
 */
fun Long.formatCurrency(symbol: String = "¥"): String {
    return "$symbol${formatWithCommas()}"
}

/**
 * Double 扩展：格式化货币
 */
fun Double.formatCurrency(symbol: String = "¥", decimalPlaces: Int = 2): String {
    return "$symbol${formatWithCommas(decimalPlaces)}"
}

/**
 * Float 扩展：格式化货币
 */
fun Float.formatCurrency(symbol: String = "¥", decimalPlaces: Int = 2): String {
    return toDouble().formatCurrency(symbol, decimalPlaces)
}

/**
 * Int 扩展：格式化百分比
 */
fun Int.formatPercent(decimalPlaces: Int = 0): String {
    val formatter = DecimalFormat("0.${"0".repeat(decimalPlaces)}%")
    return formatter.format(this / 100.0)
}

/**
 * Double 扩展：格式化百分比
 */
fun Double.formatPercent(decimalPlaces: Int = 2): String {
    val formatter = DecimalFormat("0.${"0".repeat(decimalPlaces)}%")
    return formatter.format(this)
}

/**
 * Float 扩展：格式化百分比
 */
fun Float.formatPercent(decimalPlaces: Int = 2): String {
    return toDouble().formatPercent(decimalPlaces)
}

/**
 * Int 扩展：限制在指定范围内
 */
fun Int.coerceIn(min: Int, max: Int): Int {
    return coerceAtLeast(min).coerceAtMost(max)
}

/**
 * Long 扩展：限制在指定范围内
 */
fun Long.coerceIn(min: Long, max: Long): Long {
    return coerceAtLeast(min).coerceAtMost(max)
}

/**
 * Double 扩展：限制在指定范围内
 */
fun Double.coerceIn(min: Double, max: Double): Double {
    return coerceAtLeast(min).coerceAtMost(max)
}

/**
 * Float 扩展：限制在指定范围内
 */
fun Float.coerceIn(min: Float, max: Float): Float {
    return coerceAtLeast(min).coerceAtMost(max)
}

/**
 * Int 扩展：检查是否在范围内
 */
fun Int.isInRange(min: Int, max: Int): Boolean {
    return this in min..max
}

/**
 * Long 扩展：检查是否在范围内
 */
fun Long.isInRange(min: Long, max: Long): Boolean {
    return this in min..max
}

/**
 * Double 扩展：检查是否在范围内
 */
fun Double.isInRange(min: Double, max: Double): Boolean {
    return this in min..max
}

/**
 * Float 扩展：检查是否在范围内
 */
fun Float.isInRange(min: Float, max: Float): Boolean {
    return this in min..max
}

/**
 * Int 扩展：转换为带单位的字符串（K, M, B）
 */
fun Int.formatWithUnit(): String {
    return when {
        this >= 1_000_000_000 -> String.format("%.2fB", this / 1_000_000_000.0)
        this >= 1_000_000 -> String.format("%.2fM", this / 1_000_000.0)
        this >= 1_000 -> String.format("%.2fK", this / 1_000.0)
        else -> toString()
    }
}

/**
 * Long 扩展：转换为带单位的字符串（K, M, B）
 */
fun Long.formatWithUnit(): String {
    return when {
        this >= 1_000_000_000L -> String.format("%.2fB", this / 1_000_000_000.0)
        this >= 1_000_000L -> String.format("%.2fM", this / 1_000_000.0)
        this >= 1_000L -> String.format("%.2fK", this / 1_000.0)
        else -> toString()
    }
}

