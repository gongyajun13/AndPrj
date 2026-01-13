package com.jun.core.ui.base.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.FileProvider
import java.io.File
import java.io.Serializable

/**
 * Intent 工具类
 * 提供 Intent 创建、操作和解析的便捷方法
 * 
 * 使用示例：
 * ```kotlin
 * // 创建 Intent
 * val intent = IntentUtils.create<DetailActivity>(context) {
 *     putString("key", "value")
 *     putInt("id", 123)
 * }
 * 
 * // 启动 Activity
 * IntentUtils.startActivity(context, intent)
 * 
 * // 从 Intent 获取数据
 * val value = IntentUtils.getString(intent, "key", "default")
 * val id = IntentUtils.getInt(intent, "id", 0)
 * ```
 */
object IntentUtils {
    
    // ==================== Intent 创建 ====================
    
    /**
     * 创建 Intent（使用泛型）
     * @param context Context
     * @param block Intent 配置块
     */
    inline fun <reified T : Activity> create(
        context: Context,
        block: Intent.() -> Unit = {}
    ): Intent {
        return Intent(context, T::class.java).apply(block)
    }
    
    /**
     * 创建 Intent（使用 Class）
     * @param context Context
     * @param clazz Activity 的 Class
     * @param block Intent 配置块
     */
    fun create(
        context: Context,
        clazz: Class<out Activity>,
        block: Intent.() -> Unit = {}
    ): Intent {
        return Intent(context, clazz).apply(block)
    }
    
    /**
     * 创建隐式 Intent
     * @param action Action
     * @param block Intent 配置块
     */
    fun createImplicit(
        action: String,
        block: Intent.() -> Unit = {}
    ): Intent {
        return Intent(action).apply(block)
    }
    
    /**
     * 创建分享 Intent
     * @param text 分享的文本
     * @param subject 主题（可选）
     */
    fun createShareIntent(
        text: String,
        subject: String? = null
    ): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
    }
    
    /**
     * 创建分享文件 Intent
     * @param context Context
     * @param file 要分享的文件
     * @param mimeType MIME 类型（可选）
     * @param authority FileProvider authority
     */
    fun createShareFileIntent(
        context: Context,
        file: File,
        mimeType: String? = null,
        authority: String = "${context.packageName}.fileprovider"
    ): Intent? {
        if (!file.exists()) return null
        
        val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, authority, file)
        } else {
            Uri.fromFile(file)
        }
        
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType ?: getMimeType(file.absolutePath)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * 创建打开文件 Intent
     * @param context Context
     * @param file 要打开的文件
     * @param authority FileProvider authority
     */
    fun createOpenFileIntent(
        context: Context,
        file: File,
        authority: String = "${context.packageName}.fileprovider"
    ): Intent? {
        if (!file.exists()) return null
        
        val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, authority, file)
        } else {
            Uri.fromFile(file)
        }
        
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(file.absolutePath))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
    
    /**
     * 创建拨号 Intent
     * @param phoneNumber 电话号码
     */
    fun createDialIntent(phoneNumber: String): Intent {
        return Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
    }
    
    /**
     * 创建拨打电话 Intent（需要权限）
     * @param phoneNumber 电话号码
     */
    fun createCallIntent(phoneNumber: String): Intent {
        return Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
    }
    
    /**
     * 创建发送短信 Intent
     * @param phoneNumber 电话号码
     * @param message 短信内容（可选）
     */
    fun createSmsIntent(
        phoneNumber: String,
        message: String? = null
    ): Intent {
        return Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber")).apply {
            message?.let { putExtra("sms_body", it) }
        }
    }
    
    /**
     * 创建发送邮件 Intent
     * @param email 邮箱地址
     * @param subject 主题（可选）
     * @param body 正文（可选）
     */
    fun createEmailIntent(
        email: String,
        subject: String? = null,
        body: String? = null
    ): Intent {
        return Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            body?.let { putExtra(Intent.EXTRA_TEXT, it) }
        }
    }
    
    /**
     * 创建打开 URL Intent
     * @param url URL 地址
     */
    fun createOpenUrlIntent(url: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }
    
    /**
     * 创建打开应用市场 Intent
     * @param packageName 包名
     */
    fun createMarketIntent(packageName: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    }
    
    // ==================== Intent 操作 ====================
    
    /**
     * 启动 Activity
     * @param context Context
     * @param intent Intent
     */
    fun startActivity(context: Context, intent: Intent) {
        try {
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("IntentUtils", "启动 Activity 失败", e)
        }
    }
    
    /**
     * 启动 Activity（使用泛型）
     * @param context Context
     * @param block Intent 配置块
     */
    inline fun <reified T : Activity> startActivity(
        context: Context,
        block: Intent.() -> Unit = {}
    ) {
        val intent = create<T>(context, block)
        startActivity(context, intent)
    }
    
    /**
     * 启动 Activity（使用 Class）
     * @param context Context
     * @param clazz Activity 的 Class
     * @param block Intent 配置块
     */
    fun startActivity(
        context: Context,
        clazz: Class<out Activity>,
        block: Intent.() -> Unit = {}
    ) {
        val intent = create(context, clazz, block)
        startActivity(context, intent)
    }
    
    /**
     * 启动 Activity 并关闭当前 Activity
     * @param context Context（必须是 Activity）
     * @param block Intent 配置块
     */
    inline fun <reified T : Activity> startActivityAndFinish(
        context: Context,
        block: Intent.() -> Unit = {}
    ) {
        if (context is Activity) {
            startActivity<T>(context, block)
            context.finish()
        } else {
            startActivity<T>(context, block)
        }
    }
    
    /**
     * 启动 Activity 并清空任务栈
     * @param context Context
     * @param block Intent 配置块
     */
    inline fun <reified T : Activity> startActivityAndClearTask(
        context: Context,
        block: Intent.() -> Unit = {}
    ) {
        val intent = create<T>(context) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            block()
        }
        startActivity(context, intent)
    }
    
    /**
     * 启动 Activity 并返回结果
     * @param activity Activity
     * @param requestCode 请求码
     * @param block Intent 配置块
     */
    inline fun <reified T : Activity> startActivityForResult(
        activity: Activity,
        requestCode: Int,
        block: Intent.() -> Unit = {}
    ) {
        val intent = create<T>(activity, block)
        androidx.core.app.ActivityCompat.startActivityForResult(activity, intent, requestCode, null)
    }
    
    // ==================== Intent Extra 操作（类型安全的 put/get）====================
    
    /**
     * 类型安全的 putExtra 扩展函数
     */
    fun Intent.putString(key: String, value: String?) {
        putExtra(key, value)
    }
    
    fun Intent.putInt(key: String, value: Int) {
        putExtra(key, value)
    }
    
    fun Intent.putLong(key: String, value: Long) {
        putExtra(key, value)
    }
    
    fun Intent.putFloat(key: String, value: Float) {
        putExtra(key, value)
    }
    
    fun Intent.putDouble(key: String, value: Double) {
        putExtra(key, value)
    }
    
    fun Intent.putBoolean(key: String, value: Boolean) {
        putExtra(key, value)
    }
    
    fun Intent.putParcelable(key: String, value: Parcelable?) {
        putExtra(key, value)
    }
    
    fun Intent.putSerializable(key: String, value: Serializable?) {
        putExtra(key, value)
    }
    
    fun Intent.putStringArray(key: String, value: Array<String>?) {
        putExtra(key, value)
    }
    
    fun Intent.putIntArray(key: String, value: IntArray?) {
        putExtra(key, value)
    }
    
    fun Intent.putStringArrayList(key: String, value: ArrayList<String>?) {
        putExtra(key, value)
    }
    
    fun Intent.putBundle(key: String, value: Bundle?) {
        putExtra(key, value)
    }
    
    /**
     * 类型安全的 getExtra 方法
     */
    fun getString(intent: Intent, key: String, defaultValue: String? = null): String? {
        return intent.getStringExtra(key) ?: defaultValue
    }
    
    fun getInt(intent: Intent, key: String, defaultValue: Int = 0): Int {
        return intent.getIntExtra(key, defaultValue)
    }
    
    fun getLong(intent: Intent, key: String, defaultValue: Long = 0L): Long {
        return intent.getLongExtra(key, defaultValue)
    }
    
    fun getFloat(intent: Intent, key: String, defaultValue: Float = 0f): Float {
        return intent.getFloatExtra(key, defaultValue)
    }
    
    fun getDouble(intent: Intent, key: String, defaultValue: Double = 0.0): Double {
        return intent.getDoubleExtra(key, defaultValue)
    }
    
    fun getBoolean(intent: Intent, key: String, defaultValue: Boolean = false): Boolean {
        return intent.getBooleanExtra(key, defaultValue)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : Parcelable> getParcelable(intent: Intent, key: String, clazz: Class<T>): T? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<T>(key) as? T
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : Serializable> getSerializable(intent: Intent, key: String, clazz: Class<T>): T? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(key) as? T
        }
    }
    
    fun getStringArray(intent: Intent, key: String): Array<String>? {
        return intent.getStringArrayExtra(key)
    }
    
    fun getIntArray(intent: Intent, key: String): IntArray? {
        return intent.getIntArrayExtra(key)
    }
    
    fun getStringArrayList(intent: Intent, key: String): ArrayList<String>? {
        return intent.getStringArrayListExtra(key)
    }
    
    fun getBundle(intent: Intent, key: String): Bundle? {
        return intent.getBundleExtra(key)
    }
    
    // ==================== Intent 检查 ====================
    
    /**
     * 检查 Intent 是否可以启动
     * @param context Context
     * @param intent Intent
     */
    fun canResolve(context: Context, intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }
    
    /**
     * 检查是否有应用可以处理该 Intent
     * @param context Context
     * @param intent Intent
     */
    fun hasHandler(context: Context, intent: Intent): Boolean {
        return context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取文件的 MIME 类型
     */
    private fun getMimeType(filePath: String): String {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension) ?: "*/*"
    }
    
    /**
     * 复制 Intent
     * @param intent 原始 Intent
     */
    fun copy(intent: Intent): Intent {
        return Intent(intent)
    }
    
    /**
     * 清空 Intent 的所有 Extra
     */
    fun Intent.clearExtras() {
        extras?.clear()
    }
    
    /**
     * 获取所有 Extra 的键
     */
    fun Intent.getExtraKeys(): Set<String> {
        return extras?.keySet() ?: emptySet()
    }
    
    /**
     * 检查是否包含指定的 Extra（扩展函数，提供更清晰的 API）
     */
    fun Intent.containsExtra(key: String): Boolean {
        return hasExtra(key)
    }
}

