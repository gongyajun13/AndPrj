package com.jun.core.common.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 日期时间工具类
 */
object DateUtils {
    
    /**
     * 常用日期格式
     */
    object Format {
        const val DATE_TIME = "yyyy-MM-dd HH:mm:ss"
        const val DATE = "yyyy-MM-dd"
        const val TIME = "HH:mm:ss"
        const val DATE_TIME_CN = "yyyy年MM月dd日 HH:mm:ss"
        const val DATE_CN = "yyyy年MM月dd日"
        const val MONTH_DAY = "MM-dd"
        const val YEAR_MONTH = "yyyy-MM"
    }
    
    /**
     * 格式化日期
     */
    fun format(date: Date, pattern: String, locale: Locale = Locale.getDefault()): String {
        return SimpleDateFormat(pattern, locale).format(date)
    }
    
    /**
     * 格式化时间戳
     */
    fun format(timestamp: Long, pattern: String, locale: Locale = Locale.getDefault()): String {
        return format(Date(timestamp), pattern, locale)
    }
    
    /**
     * 解析日期字符串
     */
    fun parse(dateString: String, pattern: String, locale: Locale = Locale.getDefault()): Date? {
        return try {
            SimpleDateFormat(pattern, locale).parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取当前时间戳（毫秒）
     */
    fun currentTimestamp(): Long = System.currentTimeMillis()
    
    /**
     * 获取当前日期
     */
    fun currentDate(): Date = Date()
    
    /**
     * 获取今天开始的时间戳
     */
    fun todayStartTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 获取今天结束的时间戳
     */
    fun todayEndTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * 判断是否为今天
     */
    fun isToday(timestamp: Long): Boolean {
        val todayStart = todayStartTimestamp()
        val todayEnd = todayEndTimestamp()
        return timestamp in todayStart..todayEnd
    }
    
    /**
     * 判断是否为昨天
     */
    fun isYesterday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val yesterdayStart = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val yesterdayEnd = calendar.timeInMillis
        
        return timestamp in yesterdayStart..yesterdayEnd
    }
    
    /**
     * 获取相对时间描述（如：刚刚、5分钟前、昨天、2024-01-01）
     */
    fun getRelativeTime(timestamp: Long): String {
        val now = currentTimestamp()
        val diff = now - timestamp
        
        return when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
            isToday(timestamp) -> "今天 ${format(timestamp, Format.TIME)}"
            isYesterday(timestamp) -> "昨天 ${format(timestamp, Format.TIME)}"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
            else -> format(timestamp, Format.DATE)
        }
    }
    
    /**
     * 获取时间差（毫秒）
     */
    fun getTimeDifference(timestamp1: Long, timestamp2: Long): Long {
        return kotlin.math.abs(timestamp1 - timestamp2)
    }
}

