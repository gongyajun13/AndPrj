package com.jun.core.common.util.permission

import android.app.Activity
import android.app.Fragment
import android.content.Context
import androidx.annotation.IntRange
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment as AndroidXFragment
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.OnPermissionDescription
import com.hjq.permissions.OnPermissionInterceptor
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.base.IPermission

/**
 * 权限请求辅助类
 *
 * 提供链式调用的权限请求接口，简化权限请求流程
 *
 * 使用示例：
 * ```
 * PermissionHelper.with(this)
 *     .permission(permission1, permission2)
 *     .interceptor(PermissionInterceptor())
 *     .description(PermissionDescription())
 *     .request { grantedList, deniedList ->
 *         if (deniedList.isEmpty()) {
 *             // 所有权限已授予
 *             showSuccess("权限已授予")
 *         } else {
 *             // 有权限被拒绝
 *             showError("权限被拒绝")
 *         }
 *     }
 * ```
 */
class PermissionHelper private constructor(
    private val context: Context
) {

    private var permissions: List<IPermission>? = null
    private var interceptor: OnPermissionInterceptor? = null
    private var description: OnPermissionDescription? = null

    companion object {
        /**
         * 创建权限请求辅助类实例
         *
         * @param context 上下文对象（Activity 或 Fragment）
         * @return PermissionHelper 实例
         */
        @JvmStatic
        fun with(context: Context): PermissionHelper {
            return PermissionHelper(context)
        }

        /**
         * 创建权限请求辅助类实例（Activity 版本）
         *
         * @param activity Activity 对象
         * @return PermissionHelper 实例
         */
        @JvmStatic
        fun with(activity: Activity): PermissionHelper {
            return PermissionHelper(activity)
        }

        /**
         * 创建权限请求辅助类实例（Fragment 版本）
         *
         * @param fragment Fragment 对象
         * @return PermissionHelper 实例
         */
        @JvmStatic
        fun with(fragment: Fragment): PermissionHelper {
            return PermissionHelper(fragment.activity ?: throw IllegalStateException("Fragment 未附加到 Activity"))
        }

        /**
         * 创建权限请求辅助类实例（AndroidX Fragment 版本）
         *
         * @param fragment AndroidX Fragment 对象
         * @return PermissionHelper 实例
         */
        @JvmStatic
        fun with(fragment: AndroidXFragment): PermissionHelper {
            return PermissionHelper(fragment.requireContext())
        }
    }

    /**
     * 设置要请求的权限
     *
     * @param permissions 权限列表
     * @return PermissionHelper 实例，支持链式调用
     */
    fun permission(vararg permissions: IPermission): PermissionHelper {
        this.permissions = permissions.toList()
        return this
    }

    /**
     * 设置要请求的权限（列表形式）
     *
     * @param permissions 权限列表
     * @return PermissionHelper 实例，支持链式调用
     */
    fun permission(permissions: List<IPermission>): PermissionHelper {
        this.permissions = permissions
        return this
    }

    /**
     * 设置权限拦截器
     *
     * @param interceptor 权限拦截器
     * @return PermissionHelper 实例，支持链式调用
     */
    fun interceptor(interceptor: OnPermissionInterceptor): PermissionHelper {
        this.interceptor = interceptor
        return this
    }

    /**
     * 设置权限描述
     *
     * @param description 权限描述
     * @return PermissionHelper 实例，支持链式调用
     */
    fun description(description: OnPermissionDescription): PermissionHelper {
        this.description = description
        return this
    }

    /**
     * 发起权限请求
     *
     * @param callback 权限请求结果回调
     */
    fun request(callback: OnPermissionCallback) {
        if (permissions.isNullOrEmpty()) {
            throw IllegalArgumentException("权限列表不能为空，请先调用 permission() 方法设置权限")
        }

        val activity = context as? Activity
            ?: throw IllegalArgumentException("Context 必须是 Activity 类型")

        // XXPermissions 的 permission 方法可能只接受单个 IPermission
        // 尝试使用反射或者检查是否有其他重载方法
        // 如果只有一个权限，直接调用；如果有多个，需要逐个添加
        val requestBuilder = if (permissions!!.size == 1) {
            XXPermissions.with(activity).permission(permissions!![0])
        } else {
            // 对于多个权限，尝试使用第一个权限初始化，然后逐个添加
            // 注意：这可能需要根据 XXPermissions 的实际 API 调整
            var builder = XXPermissions.with(activity).permission(permissions!![0])
            for (i in 1 until permissions!!.size) {
                builder = builder.permission(permissions!![i])
            }
            builder
        }

        // 设置拦截器
        interceptor?.let {
            requestBuilder.interceptor(it)
        }

        // 设置描述
        description?.let {
            requestBuilder.description(it)
        }

        // 发起请求
        requestBuilder.request(callback)
    }

    /**
     * 发起权限请求（使用 Lambda 回调）
     *
     * @param onResult 权限请求结果回调
     *                  grantedList: 已授予的权限列表
     *                  deniedList: 被拒绝的权限列表
     */
    fun request(
        onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
    ) {
        request(object : OnPermissionCallback {
            override fun onResult(
                @NonNull grantedList: MutableList<IPermission>,
                @NonNull deniedList: MutableList<IPermission>
            ) {
                onResult(grantedList, deniedList)
            }
        })
    }
}

// ==================== 权限检查相关 ====================

/**
 * 判断单个权限是否已授予
 *
 * @param context 上下文对象
 * @param permission 权限对象
 * @return 是否已授予
 */

fun isGrantedPermission(context: Context, permission: IPermission): Boolean {
    return XXPermissions.isGrantedPermission(context, permission)
}

/**
 * 判断多个权限是否全部已授予
 *
 * @param context 上下文对象
 * @param permissions 权限数组
 * @return 是否全部已授予
 */
fun isGrantedPermissions(context: Context, vararg permissions: IPermission): Boolean {
    return XXPermissions.isGrantedPermissions(context, permissions)
}

/**
 * 判断多个权限是否全部已授予（列表形式）
 *
 * @param context 上下文对象
 * @param permissions 权限列表
 * @return 是否全部已授予
 */
fun isGrantedPermissions(context: Context, permissions: List<IPermission>): Boolean {
    return XXPermissions.isGrantedPermissions(context, permissions)
}

// ==================== 权限获取相关 ====================

/**
 * 从权限列表中获取已授予的权限
 *
 * @param context 上下文对象
 * @param permissions 权限数组
 * @return 已授予的权限列表
 */
fun getGrantedPermissions(context: Context, vararg permissions: IPermission): List<IPermission> {
    return XXPermissions.getGrantedPermissions(context, permissions)
}

/**
 * 从权限列表中获取已授予的权限（列表形式）
 *
 * @param context 上下文对象
 * @param permissions 权限列表
 * @return 已授予的权限列表
 */
fun getGrantedPermissions(context: Context, permissions: List<IPermission>): List<IPermission> {
    return XXPermissions.getGrantedPermissions(context, permissions)
}

/**
 * 从权限列表中获取被拒绝的权限
 *
 * @param context 上下文对象
 * @param permissions 权限数组
 * @return 被拒绝的权限列表
 */
fun getDeniedPermissions(context: Context, vararg permissions: IPermission): List<IPermission> {
    return XXPermissions.getDeniedPermissions(context, permissions)
}

/**
 * 从权限列表中获取被拒绝的权限（列表形式）
 *
 * @param context 上下文对象
 * @param permissions 权限列表
 * @return 被拒绝的权限列表
 */
fun getDeniedPermissions(context: Context, permissions: List<IPermission>): List<IPermission> {
    return XXPermissions.getDeniedPermissions(context, permissions)
}

// ==================== 权限比较相关 ====================

/**
 * 判断两个权限是否相等
 *
 * @param permission1 权限对象1
 * @param permission2 权限对象2
 * @return 是否相等
 */
fun equalsPermission(permission1: IPermission, permission2: IPermission): Boolean {
    return XXPermissions.equalsPermission(permission1, permission2)
}

/**
 * 判断权限对象和权限名称是否相等
 *
 * @param permission 权限对象
 * @param permissionName 权限名称
 * @return 是否相等
 */
fun equalsPermission(permission: IPermission, permissionName: String): Boolean {
    return XXPermissions.equalsPermission(permission, permissionName)
}

/**
 * 判断两个权限名称是否相等
 *
 * @param permissionName1 权限名称1
 * @param permissionName2 权限名称2
 * @return 是否相等
 */
fun equalsPermission(permissionName1: String, permissionName2: String): Boolean {
    return XXPermissions.equalsPermission(permissionName1, permissionName2)
}

/**
 * 判断权限列表中是否包含某个权限
 *
 * @param permissions 权限列表
 * @param permission 权限对象
 * @return 是否包含
 */
fun containsPermission(permissions: List<IPermission>, permission: IPermission): Boolean {
    return XXPermissions.containsPermission(permissions, permission)
}

/**
 * 判断权限列表中是否包含某个权限名称
 *
 * @param permissions 权限列表
 * @param permissionName 权限名称
 * @return 是否包含
 */
fun containsPermission(permissions: List<IPermission>, permissionName: String): Boolean {
    return XXPermissions.containsPermission(permissions, permissionName)
}

/**
 * 判断某个权限是否为健康权限
 *
 * @param permission 权限对象
 * @return 是否为健康权限
 */
fun isHealthPermission(permission: IPermission): Boolean {
    return XXPermissions.isHealthPermission(permission)
}

// ==================== 永久拒绝检查相关 ====================

/**
 * 判断单个权限是否被永久拒绝（一定要在权限申请的回调方法中调用才有效果）
 *
 * @param activity Activity 对象
 * @param permission 权限对象
 * @return 是否被永久拒绝
 */
fun isDoNotAskAgainPermission(activity: Activity, permission: IPermission): Boolean {
    return XXPermissions.isDoNotAskAgainPermission(activity, permission)
}

/**
 * 判断多个权限是否被永久拒绝（一定要在权限申请的回调方法中调用才有效果）
 *
 * @param activity Activity 对象
 * @param permissions 权限数组
 * @return 是否全部被永久拒绝
 */
fun isDoNotAskAgainPermissions(activity: Activity, vararg permissions: IPermission): Boolean {
    return XXPermissions.isDoNotAskAgainPermissions(activity, permissions)
}

/**
 * 判断多个权限是否被永久拒绝（列表形式，一定要在权限申请的回调方法中调用才有效果）
 *
 * @param activity Activity 对象
 * @param permissions 权限列表
 * @return 是否全部被永久拒绝
 */
fun isDoNotAskAgainPermissions(activity: Activity, permissions: List<IPermission>): Boolean {
    return XXPermissions.isDoNotAskAgainPermissions(activity, permissions)
}

// ==================== 跳转到权限设置页相关 ====================

/**
 * 跳转到权限设置页（Context 版本）
 *
 * @param context 上下文对象
 */
fun startPermissionActivity(context: Context) {
    XXPermissions.startPermissionActivity(context)
}

/**
 * 跳转到权限设置页（Context 版本，指定权限）
 *
 * @param context 上下文对象
 * @param permissions 权限数组
 */
fun startPermissionActivity(context: Context, vararg permissions: IPermission) {
    XXPermissions.startPermissionActivity(context, *permissions)
}

/**
 * 跳转到权限设置页（Context 版本，指定权限列表）
 *
 * @param context 上下文对象
 * @param permissions 权限列表
 */
fun startPermissionActivity(context: Context, permissions: List<IPermission>) {
    XXPermissions.startPermissionActivity(context, permissions)
}

/**
 * 跳转到权限设置页（Activity 版本）
 *
 * @param activity Activity 对象
 */
fun startPermissionActivity(activity: Activity) {
    XXPermissions.startPermissionActivity(activity)
}

/**
 * 跳转到权限设置页（Activity 版本，指定权限）
 *
 * @param activity Activity 对象
 * @param permissions 权限数组
 */
fun startPermissionActivity(activity: Activity, vararg permissions: IPermission) {
    XXPermissions.startPermissionActivity(activity, *permissions)
}

/**
 * 跳转到权限设置页（Activity 版本，指定权限列表）
 *
 * @param activity Activity 对象
 * @param permissions 权限列表
 */
fun startPermissionActivity(activity: Activity, permissions: List<IPermission>) {
    XXPermissions.startPermissionActivity(activity, permissions)
}

/**
 * 跳转到权限设置页（Activity 版本，指定权限列表和请求码）
 *
 * @param activity Activity 对象
 * @param permissions 权限列表
 * @param requestCode 请求码（1-65535）
 */
fun startPermissionActivity(
    activity: Activity,
    permissions: List<IPermission>,
    @IntRange(from = 1, to = 65535) requestCode: Int
) {
    XXPermissions.startPermissionActivity(activity, permissions, requestCode)
}

/**
 * 跳转到权限设置页（Activity 版本，指定单个权限和回调）
 *
 * @param activity Activity 对象
 * @param permission 权限对象
 * @param callback 权限请求结果回调
 */
fun startPermissionActivity(
    activity: Activity,
    permission: IPermission,
    @Nullable callback: OnPermissionCallback?
) {
    XXPermissions.startPermissionActivity(activity, permission, callback)
}

/**
 * 跳转到权限设置页（Activity 版本，指定权限列表和回调）
 *
 * @param activity Activity 对象
 * @param permissions 权限列表
 * @param callback 权限请求结果回调
 */
fun startPermissionActivity(
    activity: Activity,
    permissions: List<IPermission>,
    @Nullable callback: OnPermissionCallback?
) {
    XXPermissions.startPermissionActivity(activity, permissions, callback)
}

/**
 * 跳转到权限设置页（Fragment 版本）
 *
 * @param fragment Fragment 对象
 */
fun startPermissionActivity(fragment: Fragment) {
    XXPermissions.startPermissionActivity(fragment)
}

/**
 * 跳转到权限设置页（Fragment 版本，指定权限）
 *
 * @param fragment Fragment 对象
 * @param permissions 权限数组
 */
fun startPermissionActivity(fragment: Fragment, vararg permissions: IPermission) {
    XXPermissions.startPermissionActivity(fragment, *permissions)
}

/**
 * 跳转到权限设置页（Fragment 版本，指定权限列表）
 *
 * @param fragment Fragment 对象
 * @param permissions 权限列表
 */
fun startPermissionActivity(fragment: Fragment, permissions: List<IPermission>) {
    XXPermissions.startPermissionActivity(fragment, permissions)
}

/**
 * 跳转到权限设置页（Fragment 版本，指定权限列表和请求码）
 *
 * @param fragment Fragment 对象
 * @param permissions 权限列表
 * @param requestCode 请求码（1-65535）
 */
fun startPermissionActivity(
    fragment: Fragment,
    permissions: List<IPermission>,
    @IntRange(from = 1, to = 65535) requestCode: Int
) {
    XXPermissions.startPermissionActivity(fragment, permissions, requestCode)
}

/**
 * 跳转到权限设置页（Fragment 版本，指定单个权限和回调）
 *
 * @param fragment Fragment 对象
 * @param permission 权限对象
 * @param callback 权限请求结果回调
 */
fun startPermissionActivity(
    fragment: Fragment,
    permission: IPermission,
    @Nullable callback: OnPermissionCallback?
) {
    XXPermissions.startPermissionActivity(fragment, permission, callback)
}

/**
 * 跳转到权限设置页（Fragment 版本，指定权限列表和回调）
 *
 * @param fragment Fragment 对象
 * @param permissions 权限列表
 * @param callback 权限请求结果回调
 */
fun startPermissionActivity(
    fragment: Fragment,
    permissions: List<IPermission>,
    @Nullable callback: OnPermissionCallback?
) {
    XXPermissions.startPermissionActivity(fragment, permissions, callback)
}

/**
 * 跳转到权限设置页（AndroidX Fragment 版本）
 *
 * @param fragment AndroidX Fragment 对象
 */
fun startPermissionActivity(fragment: AndroidXFragment) {
    XXPermissions.startPermissionActivity(fragment)
}

/**
 * 跳转到权限设置页（AndroidX Fragment 版本，指定权限）
 *
 * @param fragment AndroidX Fragment 对象
 * @param permissions 权限数组
 */
fun startPermissionActivity(fragment: AndroidXFragment, vararg permissions: IPermission) {
    XXPermissions.startPermissionActivity(fragment, *permissions)
}

/**
 * 跳转到权限设置页（AndroidX Fragment 版本，指定权限列表）
 *
 * @param fragment AndroidX Fragment 对象
 * @param permissions 权限列表
 */
fun startPermissionActivity(fragment: AndroidXFragment, permissions: List<IPermission>) {
    XXPermissions.startPermissionActivity(fragment, permissions)
}

/**
 * 跳转到权限设置页（AndroidX Fragment 版本，指定权限列表和请求码）
 *
 * @param fragment AndroidX Fragment 对象
 * @param permissions 权限列表
 * @param requestCode 请求码（1-65535）
 */
fun startPermissionActivity(
    fragment: AndroidXFragment,
    permissions: List<IPermission>,
    @IntRange(from = 1, to = 65535) requestCode: Int
) {
    XXPermissions.startPermissionActivity(fragment, permissions, requestCode)
}

/**
 * 跳转到权限设置页（AndroidX Fragment 版本，指定单个权限和回调）
 *
 * @param fragment AndroidX Fragment 对象
 * @param permission 权限对象
 * @param callback 权限请求结果回调
 */
fun startPermissionActivity(
    fragment: AndroidXFragment,
    permission: IPermission,
    @Nullable callback: OnPermissionCallback?
) {
    XXPermissions.startPermissionActivity(fragment, permission, callback)
}

/**
 * 跳转到权限设置页（AndroidX Fragment 版本，指定权限列表和回调）
 *
 * @param fragment AndroidX Fragment 对象
 * @param permissions 权限列表
 * @param callback 权限请求结果回调
 */
fun startPermissionActivity(
    fragment: AndroidXFragment,
    permissions: List<IPermission>,
    @Nullable callback: OnPermissionCallback?
) {
    XXPermissions.startPermissionActivity(fragment, permissions, callback)
}

// ==================== 全局设置相关 ====================

/**
 * 设置权限描述器（全局设置）
 *
 * @param clazz 权限描述器类
 */
fun setPermissionDescription(clazz: Class<out OnPermissionDescription>) {
    XXPermissions.setPermissionDescription(clazz)
}

/**
 * 设置权限申请拦截器（全局设置）
 *
 * @param clazz 权限拦截器类
 */
fun setPermissionInterceptor(clazz: Class<out OnPermissionInterceptor>) {
    XXPermissions.setPermissionInterceptor(clazz)
}

/**
 * 设置是否开启错误检测模式（全局设置）
 *
 * @param checkMode 是否开启错误检测模式
 */
fun setCheckMode(checkMode: Boolean) {
    XXPermissions.setCheckMode(checkMode)
}

