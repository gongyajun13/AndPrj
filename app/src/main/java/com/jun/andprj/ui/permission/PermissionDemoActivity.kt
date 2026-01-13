package com.jun.andprj.ui.permission

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.hjq.permissions.permission.PermissionLists
import com.hjq.permissions.permission.base.IPermission
import com.jun.andprj.R
import com.jun.andprj.databinding.ActivityPermissionDemoBinding
import com.jun.core.common.extension.requestPermission
import com.jun.core.common.util.permission.PermissionHelper
import com.jun.core.common.util.permission.PermissionDescription
import com.jun.core.common.util.permission.PermissionInterceptor
import com.jun.core.ui.base.BaseActivity

/**
 * 权限请求示例 Activity
 * 展示多种权限请求场景，包括：
 * 1. 单个权限请求（使用扩展函数）
 * 2. 多个权限请求（使用扩展函数）
 * 3. 链式调用（使用 PermissionHelper）
 * 4. 自定义拦截器和描述
 */
class PermissionDemoActivity : BaseActivity<ActivityPermissionDemoBinding>() {

    private lateinit var adapter: PermissionDemoAdapter

    // 权限请求示例数据
    private val permissionItems = listOf(
        PermissionItem(
            title = "相机权限",
            description = "单个权限请求（使用扩展函数）",
            permissions = listOf(PermissionLists.getCameraPermission()),
            requestType = RequestType.EXTENSION_SINGLE
        ),
        PermissionItem(
            title = "定位权限",
            description = "多个权限请求（使用扩展函数）",
            permissions = listOf(
                PermissionLists.getAccessCoarseLocationPermission(),
                PermissionLists.getAccessFineLocationPermission()
            ),
            requestType = RequestType.EXTENSION_MULTIPLE
        ),
        PermissionItem(
            title = "存储权限",
            description = "链式调用（使用 PermissionHelper）",
            permissions = listOf(
                PermissionLists.getReadMediaImagesPermission(),
                PermissionLists.getReadMediaVideoPermission()
            ),
            requestType = RequestType.HELPER_CHAIN
        ),
        PermissionItem(
            title = "通知权限",
            description = "自定义拦截器和描述",
            permissions = listOf(PermissionLists.getPostNotificationsPermission()),
            requestType = RequestType.CUSTOM_INTERCEPTOR
        ),
        PermissionItem(
            title = "录音权限",
            description = "使用扩展函数（列表形式）",
            permissions = listOf(PermissionLists.getRecordAudioPermission()),
            requestType = RequestType.EXTENSION_LIST
        ),
        PermissionItem(
            title = "日历权限组",
            description = "多个权限（日历读写）",
            permissions = listOf(
                PermissionLists.getReadCalendarPermission(),
                PermissionLists.getWriteCalendarPermission()
            ),
            requestType = RequestType.EXTENSION_MULTIPLE
        )
    )

    override fun createBinding(): ActivityPermissionDemoBinding =
        ActivityPermissionDemoBinding.inflate(layoutInflater)

    override fun setupViews() {
        setupToolbar()
        val white = ContextCompat.getColor(this, android.R.color.white)
        setStatusBarColor(white, lightIcons = false)
        setupRecyclerView()
    }

    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        binding.toolbar.setupSimple(
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            title = "权限请求示例",
            titleTextColor = white,
            backgroundColor = ContextCompat.getColor(this, R.color.blue),
            onLeftClick = { finish() }
        )
    }

    private fun setupRecyclerView() {
        adapter = PermissionDemoAdapter(permissionItems) { item ->
            requestPermission(item)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    /**
     * 根据不同的请求类型执行权限请求
     */
    private fun requestPermission(item: PermissionItem) {
        when (item.requestType) {
            RequestType.EXTENSION_SINGLE -> {
                // 使用扩展函数，单个权限
                requestPermission(item.permissions[0]) { grantedList, deniedList ->
                    handlePermissionResult(item.title, grantedList, deniedList)
                }
            }

            RequestType.EXTENSION_MULTIPLE -> {
                // 使用扩展函数，多个权限（vararg）
                requestPermission(*item.permissions.toTypedArray()) { grantedList, deniedList ->
                    handlePermissionResult(item.title, grantedList, deniedList)
                }
            }

            RequestType.EXTENSION_LIST -> {
                // 使用扩展函数，列表形式
                requestPermission(item.permissions) { grantedList, deniedList ->
                    handlePermissionResult(item.title, grantedList, deniedList)
                }
            }

            RequestType.HELPER_CHAIN -> {
                // 使用 PermissionHelper 链式调用
                PermissionHelper.with(this)
                    .permission(item.permissions)
                    .interceptor(PermissionInterceptor.INSTANCE)
                    .description(PermissionDescription())
                    .request { grantedList, deniedList ->
                        handlePermissionResult(item.title, grantedList, deniedList)
                    }
            }

            RequestType.CUSTOM_INTERCEPTOR -> {
                // 使用自定义拦截器和描述
                PermissionHelper.with(this)
                    .permission(item.permissions)
                    .interceptor(PermissionInterceptor.INSTANCE)
                    .description(PermissionDescription())
                    .request { grantedList, deniedList ->
                        handlePermissionResult(item.title, grantedList, deniedList)
                    }
            }
        }
    }

    /**
     * 处理权限请求结果
     */
    private fun handlePermissionResult(
        title: String,
        grantedList: List<IPermission>,
        deniedList: List<IPermission>
    ) {
        if (deniedList.isEmpty()) {
            showSuccess("「$title」权限已全部授予")
        } else {
            val grantedNames = grantedList.joinToString(", ") { it.getPermissionName() }
            val deniedNames = deniedList.joinToString(", ") { it.getPermissionName() }
            showWarning(
                "「$title」权限请求结果：\n" +
                        "已授予：$grantedNames\n" +
                        "已拒绝：$deniedNames"
            )
        }
    }

    override fun setupObservers() {
        // 暂无数据观察
    }

    /**
     * 请求类型枚举
     */
    enum class RequestType {
        EXTENSION_SINGLE,      // 扩展函数 - 单个权限
        EXTENSION_MULTIPLE,    // 扩展函数 - 多个权限（vararg）
        EXTENSION_LIST,        // 扩展函数 - 列表形式
        HELPER_CHAIN,         // PermissionHelper - 链式调用
        CUSTOM_INTERCEPTOR     // 自定义拦截器和描述
    }

    /**
     * 权限项数据类
     */
    data class PermissionItem(
        val title: String,
        val description: String,
        val permissions: List<IPermission>,
        val requestType: RequestType
    )
}

