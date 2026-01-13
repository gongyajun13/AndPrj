package com.jun.core.common.util

/**
 * 数据验证工具类
 */
object Validator {
    
    /**
     * 验证结果
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }
    
    /**
     * 验证邮箱
     */
    fun validateEmail(email: String?): ValidationResult {
        if (email.isNullOrBlank()) {
            return ValidationResult.Invalid("邮箱不能为空")
        }
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        return if (emailRegex.matches(email)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("邮箱格式不正确")
        }
    }
    
    /**
     * 验证手机号（中国）
     */
    fun validatePhone(phone: String?): ValidationResult {
        if (phone.isNullOrBlank()) {
            return ValidationResult.Invalid("手机号不能为空")
        }
        val phoneRegex = "^1[3-9]\\d{9}\$".toRegex()
        return if (phoneRegex.matches(phone)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("手机号格式不正确")
        }
    }
    
    /**
     * 验证密码强度
     */
    fun validatePassword(password: String?, minLength: Int = 6): ValidationResult {
        if (password.isNullOrBlank()) {
            return ValidationResult.Invalid("密码不能为空")
        }
        if (password.length < minLength) {
            return ValidationResult.Invalid("密码长度至少为 $minLength 位")
        }
        return ValidationResult.Valid
    }
    
    /**
     * 验证密码强度（包含大小写字母、数字、特殊字符）
     */
    fun validateStrongPassword(password: String?): ValidationResult {
        if (password.isNullOrBlank()) {
            return ValidationResult.Invalid("密码不能为空")
        }
        if (password.length < 8) {
            return ValidationResult.Invalid("密码长度至少为 8 位")
        }
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        return when {
            !hasUpperCase -> ValidationResult.Invalid("密码必须包含大写字母")
            !hasLowerCase -> ValidationResult.Invalid("密码必须包含小写字母")
            !hasDigit -> ValidationResult.Invalid("密码必须包含数字")
            !hasSpecialChar -> ValidationResult.Invalid("密码必须包含特殊字符")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * 验证 URL
     */
    fun validateUrl(url: String?): ValidationResult {
        if (url.isNullOrBlank()) {
            return ValidationResult.Invalid("URL 不能为空")
        }
        val urlRegex = "^https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?\$".toRegex()
        return if (urlRegex.matches(url)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("URL 格式不正确")
        }
    }
    
    /**
     * 验证非空
     */
    fun validateNotEmpty(value: String?, fieldName: String = "字段"): ValidationResult {
        return if (value.isNullOrBlank()) {
            ValidationResult.Invalid("$fieldName 不能为空")
        } else {
            ValidationResult.Valid
        }
    }
    
    /**
     * 验证长度范围
     */
    fun validateLength(
        value: String?,
        minLength: Int,
        maxLength: Int,
        fieldName: String = "字段"
    ): ValidationResult {
        if (value.isNullOrBlank()) {
            return ValidationResult.Invalid("$fieldName 不能为空")
        }
        return when {
            value.length < minLength -> ValidationResult.Invalid("$fieldName 长度不能少于 $minLength 个字符")
            value.length > maxLength -> ValidationResult.Invalid("$fieldName 长度不能超过 $maxLength 个字符")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * 验证数字范围
     */
    fun validateNumberRange(
        value: Number?,
        min: Number,
        max: Number,
        fieldName: String = "数值"
    ): ValidationResult {
        if (value == null) {
            return ValidationResult.Invalid("$fieldName 不能为空")
        }
        val doubleValue = value.toDouble()
        val minValue = min.toDouble()
        val maxValue = max.toDouble()
        return when {
            doubleValue < minValue -> ValidationResult.Invalid("$fieldName 不能小于 $min")
            doubleValue > maxValue -> ValidationResult.Invalid("$fieldName 不能大于 $max")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * 批量验证
     */
    fun validateAll(vararg validations: ValidationResult): ValidationResult {
        return validations.firstOrNull { it is ValidationResult.Invalid }
            ?: ValidationResult.Valid
    }
}

