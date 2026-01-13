package com.jun.andprj.ui.notify

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jun.andprj.R
import com.jun.andprj.databinding.ActivityNotifyDemoBinding
import com.jun.core.common.util.notify.SnackbarUiNotifier
import com.jun.core.common.util.notify.ToastUiNotifier
import com.jun.core.ui.base.BaseActivity
import com.jun.core.common.util.notify.UiNotifierManager

/**
 * 消息提示示例 Activity
 * 展示 8 种提示方式：Snackbar 和 Toast 的 4 种类型（INFO, SUCCESS, ERROR, WARNING）
 */
class NotifyDemoActivity : BaseActivity<ActivityNotifyDemoBinding>() {

    private lateinit var adapter: NotifyDemoAdapter

    // 8 种提示方式的数据
    private val notifyItems = listOf(
        NotifyItem("Snackbar - 信息提示", NotifyType.SNACKBAR_INFO, "这是一条信息提示消息"),
        NotifyItem("Snackbar - 成功提示", NotifyType.SNACKBAR_SUCCESS, "操作成功完成！"),
        NotifyItem("Snackbar - 错误提示", NotifyType.SNACKBAR_ERROR, "操作失败，请重试"),
        NotifyItem("Snackbar - 警告提示", NotifyType.SNACKBAR_WARNING, "请注意：这是一个警告"),
        NotifyItem("Toast - 信息提示", NotifyType.TOAST_INFO, "这是一条信息提示消息"),
        NotifyItem("Toast - 成功提示", NotifyType.TOAST_SUCCESS, "操作成功完成！"),
        NotifyItem("Toast - 错误提示", NotifyType.TOAST_ERROR, "操作失败，请重试"),
        NotifyItem("Toast - 警告提示", NotifyType.TOAST_WARNING, "请注意：这是一个警告")
    )

    override fun createBinding(): ActivityNotifyDemoBinding =
        ActivityNotifyDemoBinding.inflate(layoutInflater)

    override fun setupViews() {
        setupToolbar()
        // 状态栏白底黑字
        val white = ContextCompat.getColor(this, android.R.color.white)
        setStatusBarColor(white, lightIcons = false)
        setupRecyclerView()
    }

    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        binding.toolbar.setupSimple(
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            title = "消息提示示例",
            titleTextColor = white,
            backgroundColor = ContextCompat.getColor(this, R.color.blue),
            onLeftClick = { finish() }
        )
    }

    private fun setupRecyclerView() {
        adapter = NotifyDemoAdapter(notifyItems) { item ->
            showNotify(item)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    /**
     * 显示对应的提示
     */
    private fun showNotify(item: NotifyItem) {
        val anchor = binding.root
        val originalDelegate = UiNotifierManager.delegate
        
        try {
            // 根据类型切换实现
            val notifier = when {
                item.type.name.startsWith("SNACKBAR") -> SnackbarUiNotifier()
                item.type.name.startsWith("TOAST") -> ToastUiNotifier()
                else -> originalDelegate
            }
            
            UiNotifierManager.delegate = notifier
            
            // 调用对应的方法
            when (item.type) {
                NotifyType.SNACKBAR_INFO,
                NotifyType.TOAST_INFO -> UiNotifierManager.info(anchor, item.message)
                
                NotifyType.SNACKBAR_SUCCESS,
                NotifyType.TOAST_SUCCESS -> UiNotifierManager.success(anchor, item.message)
                
                NotifyType.SNACKBAR_ERROR,
                NotifyType.TOAST_ERROR -> UiNotifierManager.error(anchor, item.message)
                
                NotifyType.SNACKBAR_WARNING,
                NotifyType.TOAST_WARNING -> UiNotifierManager.warning(anchor, item.message)
            }
        } finally {
            // 恢复原始实现
            UiNotifierManager.delegate = originalDelegate
        }
    }

    override fun setupObservers() {
        // 暂无数据观察
    }

    /**
     * 提示类型枚举
     */
    enum class NotifyType {
        SNACKBAR_INFO,
        SNACKBAR_SUCCESS,
        SNACKBAR_ERROR,
        SNACKBAR_WARNING,
        TOAST_INFO,
        TOAST_SUCCESS,
        TOAST_ERROR,
        TOAST_WARNING
    }

    /**
     * 提示项数据类
     */
    data class NotifyItem(
        val title: String,
        val type: NotifyType,
        val message: String
    )
}

