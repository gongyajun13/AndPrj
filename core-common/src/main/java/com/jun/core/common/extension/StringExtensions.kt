package com.jun.core.common.extension

/**
 * String 扩展函数集合
 */

/**
 * 检查字符串是否为空或空白
 */
fun String?.isNullOrBlank(): Boolean {
    return this == null || this.isBlank()
}

/**
 * 检查字符串是否不为空且非空白
 */
fun String?.isNotNullOrBlank(): Boolean {
    return !isNullOrBlank()
}

/**
 * 如果字符串为空或 null，返回默认值
 */
fun String?.orDefault(default: String = ""): String {
    return this ?: default
}

/**
 * 如果字符串为空或空白，返回默认值
 */
fun String?.orDefaultIfBlank(default: String = ""): String {
    return if (isNullOrBlank()) default else this!!
}

/**
 * 截断字符串到指定长度，超出部分用省略号表示
 */
fun String.truncate(maxLength: Int, suffix: String = "..."): String {
    return if (length <= maxLength) {
        this
    } else {
        take(maxLength - suffix.length) + suffix
    }
}

/**
 * 首字母大写
 */
fun String.capitalizeFirst(): String {
    return if (isEmpty()) {
        this
    } else {
        this[0].uppercaseChar() + substring(1)
    }
}

/**
 * 首字母小写
 */
fun String.decapitalizeFirst(): String {
    return if (isEmpty()) {
        this
    } else {
        this[0].lowercaseChar() + substring(1)
    }
}

/**
 * 移除所有空白字符
 */
fun String.removeWhitespace(): String {
    return replace("\\s".toRegex(), "")
}

/**
 * 移除指定字符
 */
fun String.remove(vararg chars: Char): String {
    return filter { it !in chars }
}

/**
 * 移除指定字符串
 */
fun String.remove(vararg strings: String): String {
    var result = this
    strings.forEach { result = result.replace(it, "") }
    return result
}

/**
 * 提取数字
 */
fun String.extractNumbers(): String {
    return filter { it.isDigit() }
}

/**
 * 提取字母
 */
fun String.extractLetters(): String {
    return filter { it.isLetter() }
}

/**
 * 提取字母和数字
 */
fun String.extractAlphanumeric(): String {
    return filter { it.isLetterOrDigit() }
}

/**
 * 检查是否为邮箱格式
 */
fun String.isEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * 检查是否为 URL 格式
 */
fun String.isUrl(): Boolean {
    return android.util.Patterns.WEB_URL.matcher(this).matches()
}

/**
 * 检查是否为手机号格式（11位数字）
 */
fun String.isPhoneNumber(): Boolean {
    return matches("^1[3-9]\\d{9}$".toRegex())
}

/**
 * 检查是否为身份证号格式（18位）
 */
fun String.isIdCard(): Boolean {
    return matches("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$".toRegex())
}

/**
 * 隐藏手机号中间4位
 */
fun String.maskPhone(): String {
    return if (length == 11 && all { it.isDigit() }) {
        "${substring(0, 3)}****${substring(7)}"
    } else {
        this
    }
}

/**
 * 隐藏邮箱用户名部分
 */
fun String.maskEmail(): String {
    val atIndex = indexOf('@')
    return if (atIndex > 0) {
        val username = substring(0, atIndex)
        val domain = substring(atIndex)
        "${username.take(2)}***$domain"
    } else {
        this
    }
}


/**
 * 字符串转 Int（安全）
 */
fun String.toIntOrZero(): Int {
    return toIntOrNull() ?: 0
}

/**
 * 字符串转 Long（安全）
 */
fun String.toLongOrZero(): Long {
    return toLongOrNull() ?: 0L
}

/**
 * 字符串转 Double（安全）
 */
fun String.toDoubleOrZero(): Double {
    return toDoubleOrNull() ?: 0.0
}

/**
 * 字符串转 Float（安全）
 */
fun String.toFloatOrZero(): Float {
    return toFloatOrNull() ?: 0f
}

/**
 * 字符串转 Boolean（安全）
 */
fun String.toBooleanOrFalse(): Boolean {
    return toBooleanStrictOrNull() ?: false
}

/**
 * 重复字符串指定次数
 */
fun String.repeat(times: Int): String {
    return buildString {
        repeat(times) {
            append(this@repeat)
        }
    }
}

/**
 * 在指定位置插入字符串
 */
fun String.insert(index: Int, text: String): String {
    return if (index in 0..length) {
        substring(0, index) + text + substring(index)
    } else {
        this
    }
}

/**
 * 移除指定范围的字符
 */
fun String.removeRange(startIndex: Int, endIndex: Int): String {
    return if (startIndex in 0..length && endIndex in startIndex..length) {
        substring(0, startIndex) + substring(endIndex)
    } else {
        this
    }
}

