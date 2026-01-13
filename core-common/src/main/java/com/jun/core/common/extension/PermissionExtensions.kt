package com.jun.core.common.extension

import android.app.Activity
import android.app.Fragment
import androidx.fragment.app.Fragment as AndroidXFragment
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.OnPermissionDescription
import com.hjq.permissions.OnPermissionInterceptor
import com.hjq.permissions.permission.base.IPermission
import com.jun.core.common.util.permission.PermissionHelper
import com.jun.core.common.util.permission.PermissionInterceptor
import com.jun.core.common.util.permission.PermissionDescription

/**
 * 权限请求扩展函数
 * 
 * 提供便捷的权限请求接口，使用默认的拦截器和描述
 * 如需自定义，可以使用 PermissionHelper 链式调用
 */

// ==================== 内部辅助函数 ====================

/**
 * 创建默认的权限描述实例
 * 注意：PermissionDescription 有状态，每次需要创建新实例
 */
private fun createDefaultDescription(): PermissionDescription {
    return PermissionDescription()
}

/**
 * 获取默认的权限拦截器实例（单例）
 */
private fun getDefaultInterceptor(): PermissionInterceptor {
    return PermissionInterceptor.INSTANCE
}

/**
 * 通用的权限请求实现
 */
private fun requestPermissionInternal(
    helper: PermissionHelper,
    permissions: List<IPermission>,
    interceptor: OnPermissionInterceptor?,
    description: OnPermissionDescription?,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    helper.apply {
        permission(permissions)
        interceptor?.let { interceptor(it) } ?: interceptor(getDefaultInterceptor())
        description?.let { description(it) } ?: description(createDefaultDescription())
        request(onResult)
    }
}

// ==================== Activity 扩展函数 ====================

/**
 * 请求权限（使用默认拦截器和描述）
 *
 * @param permissions 权限列表
 * @param onResult 权限请求结果回调
 */
fun Activity.requestPermission(
    vararg permissions: IPermission,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    requestPermissionInternal(
        PermissionHelper.with(this),
        permissions.toList(),
        null,
        null,
        onResult
    )
}

/**
 * 请求权限（使用默认拦截器和描述，列表形式）
 *
 * @param permissions 权限列表
 * @param onResult 权限请求结果回调
 */
fun Activity.requestPermission(
    permissions: List<IPermission>,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    requestPermissionInternal(
        PermissionHelper.with(this),
        permissions,
        null,
        null,
        onResult
    )
}

/**
 * 请求权限（自定义拦截器和描述）
 *
 * @param permissions 权限列表
 * @param interceptor 权限拦截器（可选，默认使用 PermissionInterceptor.INSTANCE）
 * @param description 权限描述（可选，默认创建新实例）
 * @param onResult 权限请求结果回调
 */
fun Activity.requestPermission(
    vararg permissions: IPermission,
    interceptor: OnPermissionInterceptor? = null,
    description: OnPermissionDescription? = null,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    requestPermissionInternal(
        PermissionHelper.with(this),
        permissions.toList(),
        interceptor,
        description,
        onResult
    )
}

/**
 * 请求权限（自定义拦截器和描述，列表形式）
 *
 * @param permissions 权限列表
 * @param interceptor 权限拦截器（可选，默认使用 PermissionInterceptor.INSTANCE）
 * @param description 权限描述（可选，默认创建新实例）
 * @param onResult 权限请求结果回调
 */
fun Activity.requestPermission(
    permissions: List<IPermission>,
    interceptor: OnPermissionInterceptor? = null,
    description: OnPermissionDescription? = null,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    requestPermissionInternal(
        PermissionHelper.with(this),
        permissions,
        interceptor,
        description,
        onResult
    )
}

// ==================== Fragment 扩展函数（Android.app.Fragment）====================

/**
 * 请求权限（使用默认拦截器和描述）
 *
 * @param permissions 权限列表
 * @param onResult 权限请求结果回调
 */
fun Fragment.requestPermission(
    vararg permissions: IPermission,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    requestPermissionInternal(
        PermissionHelper.with(this),
        permissions.toList(),
        null,
        null,
        onResult
    )
}

/**
 * 请求权限（使用默认拦截器和描述，列表形式）
 *
 * @param permissions 权限列表
 * @param onResult 权限请求结果回调
 */
fun Fragment.requestPermission(
    permissions: List<IPermission>,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    requestPermissionInternal(
        PermissionHelper.with(this),
        permissions,
        null,
        null,
        onResult
    )
}

/**
 * 请求权限（自定义拦截器和描述）
 *
 * @param permissions 权限列表
 * @param interceptor 权限拦截器（可选，默认使用 PermissionInterceptor.INSTANCE）
 * @param description 权限描述（可选，默认创建新实例）
 * @param onResult 权限请求结果回调
 */
fun Fragment.requestPermission(
    vararg permissions: IPermission,
    interceptor: OnPermissionInterceptor? = null,
    description: OnPermissionDescription? = null,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    requestPermissionInternal(
        PermissionHelper.with(this),
        permissions.toList(),
        interceptor,
        description,
        onResult
    )
}

/**
 * 请求权限（自定义拦截器和描述，列表形式）
 *
 * @param permissions 权限列表
 * @param interceptor 权限拦截器（可选，默认使用 PermissionInterceptor.INSTANCE）
 * @param description 权限描述（可选，默认创建新实例）
 * @param onResult 权限请求结果回调
 */
fun Fragment.requestPermission(
    permissions: List<IPermission>,
    interceptor: OnPermissionInterceptor? = null,
    description: OnPermissionDescription? = null,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    requestPermissionInternal(
        PermissionHelper.with(this),
        permissions,
        interceptor,
        description,
        onResult
    )
}

// ==================== Fragment 扩展函数（AndroidX Fragment）====================

/**
 * 请求权限（使用默认拦截器和描述）
 *
 * @param permissions 权限列表
 * @param onResult 权限请求结果回调
 */
fun AndroidXFragment.requestPermission(
    vararg permissions: IPermission,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    requestPermissionInternal(
        PermissionHelper.with(this),
        permissions.toList(),
        null,
        null,
        onResult
    )
}

/**
 * 请求权限（使用默认拦截器和描述，列表形式）
 *
 * @param permissions 权限列表
 * @param onResult 权限请求结果回调
 */
fun AndroidXFragment.requestPermission(
    permissions: List<IPermission>,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    requestPermissionInternal(
        PermissionHelper.with(this),
        permissions,
        null,
        null,
        onResult
    )
}

/**
 * 请求权限（自定义拦截器和描述）
 *
 * @param permissions 权限列表
 * @param interceptor 权限拦截器（可选，默认使用 PermissionInterceptor.INSTANCE）
 * @param description 权限描述（可选，默认创建新实例）
 * @param onResult 权限请求结果回调
 */
fun AndroidXFragment.requestPermission(
    vararg permissions: IPermission,
    interceptor: OnPermissionInterceptor? = null,
    description: OnPermissionDescription? = null,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    requestPermissionInternal(
        PermissionHelper.with(this),
        permissions.toList(),
        interceptor,
        description,
        onResult
    )
}

/**
 * 请求权限（自定义拦截器和描述，列表形式）
 *
 * @param permissions 权限列表
 * @param interceptor 权限拦截器（可选，默认使用 PermissionInterceptor.INSTANCE）
 * @param description 权限描述（可选，默认创建新实例）
 * @param onResult 权限请求结果回调
 */
fun AndroidXFragment.requestPermission(
    permissions: List<IPermission>,
    interceptor: OnPermissionInterceptor? = null,
    description: OnPermissionDescription? = null,
    onResult: (grantedList: List<IPermission>, deniedList: List<IPermission>) -> Unit
) {
    requestPermissionInternal(
        PermissionHelper.with(this),
        permissions,
        interceptor,
        description,
        onResult
    )
}

