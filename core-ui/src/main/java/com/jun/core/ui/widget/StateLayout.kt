package com.jun.core.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.jun.core.ui.R
import com.jun.core.ui.state.UiState
import kotlin.reflect.KMutableProperty0

/**
 * 通用状态布局容器
 *
 * 负责在同一个区域内切换以下几种视图：
 * - 加载视图
 * - 空视图
 * - 错误视图
 * - 内容视图（外部添加的子 View）
 *
 * 典型使用方式：
 * - 在 XML 中包裹实际内容
 * - 在代码中调用 [renderState] 绑定到 [UiState]
 */
class StateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var loadingView: View? = null
    private var emptyView: View? = null
    private var errorView: View? = null

    private val contentViews = mutableListOf<View>()

    private var onRetryClick: (() -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        initDefaultViews()
    }

    /**
     * 初始化默认的加载 / 空 / 错误视图
     * 这些布局完全可通过代码或自定义 XML 替换
     */
    private fun initDefaultViews() {
        val inflater = LayoutInflater.from(context)

        // 默认加载视图
        loadingView = inflater.inflate(R.layout.coreui_view_state_loading, this, false)

        // 默认空视图
        emptyView = inflater.inflate(R.layout.coreui_view_state_empty, this, false)

        // 默认错误视图
        errorView = inflater.inflate(R.layout.coreui_view_state_error, this, false)
        errorView?.findViewById<View?>(R.id.btnRetry)?.setOnClickListener {
            onRetryClick?.invoke()
        }

        // 将状态视图放在内容视图之上，保持覆盖效果
        loadingView?.let { addView(it) }
        emptyView?.let { addView(it) }
        errorView?.let { addView(it) }

        // 初始状态：只显示内容区，由外部决定是否显示 Loading
        showContent()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        // 收集作为“内容视图”的子 View（排除我们内部添加的状态视图）
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child != loadingView && child != emptyView && child != errorView) {
                contentViews.add(child)
            }
        }
    }

    /**
     * 根据 [UiState] 渲染状态
     *
     * - Initial：如果当前有内容则保持内容，否则显示空视图
     * - Loading：显示加载视图
     * - Success：
     *   - 若 data 是空集合 → 显示空视图
     *   - 否则 → 显示内容视图
     * - Error：显示错误视图
     * - Empty：显示空视图
     */
    fun <T> renderState(state: UiState<T>) {
        when (state) {
            is UiState.Initial -> {
                // 默认初始视图走“空”分支，避免白屏
                showEmpty()
            }
            is UiState.Loading -> showLoading()
            is UiState.Success -> {
                val data = state.data
                if (data is Collection<*> && data.isEmpty()) {
                    showEmpty()
                } else {
                    showContent()
                }
            }
            is UiState.Error -> {
                setErrorMessage(state.message)
                showError()
            }
            is UiState.Empty -> showEmpty()
        }
    }

    /**
     * 设置重试点击回调（仅在错误视图中生效）
     */
    fun setOnRetryClickListener(listener: (() -> Unit)?) {
        onRetryClick = listener
    }

    /**
     * 手动显示加载视图
     */
    fun showLoading() {
        setVisibleView(loadingView)
    }

    /**
     * 手动显示空视图
     */
    fun showEmpty() {
        setVisibleView(emptyView)
    }

    /**
     * 手动显示错误视图
     */
    fun showError() {
        setVisibleView(errorView)
    }

    /**
     * 手动显示内容视图
     */
    fun showContent() {
        loadingView?.visibility = View.GONE
        emptyView?.visibility = View.GONE
        errorView?.visibility = View.GONE
        contentViews.forEach { it.visibility = View.VISIBLE }
    }

    /**
     * 自定义加载视图
     */
    fun setLoadingView(view: View) {
        replaceView(::loadingView, view)
    }

    /**
     * 自定义加载视图（通过布局 ID）
     */
    fun setLoadingView(@LayoutRes layoutRes: Int) {
        val view = LayoutInflater.from(context).inflate(layoutRes, this, false)
        setLoadingView(view)
    }

    /**
     * 自定义空视图
     */
    fun setEmptyView(view: View) {
        replaceView(::emptyView, view)
    }

    /**
     * 自定义空视图（通过布局 ID）
     */
    fun setEmptyView(@LayoutRes layoutRes: Int) {
        val view = LayoutInflater.from(context).inflate(layoutRes, this, false)
        setEmptyView(view)
    }

    /**
     * 自定义错误视图
     */
    fun setErrorView(view: View) {
        replaceView(::errorView, view)
    }

    /**
     * 自定义错误视图（通过布局 ID）
     */
    fun setErrorView(@LayoutRes layoutRes: Int) {
        val view = LayoutInflater.from(context).inflate(layoutRes, this, false)
        setErrorView(view)
    }

    /**
     * 设置错误信息到默认错误布局中
     */
    fun setErrorMessage(message: CharSequence?) {
        val tvError = errorView?.findViewById<TextView?>(R.id.tvErrorMessage)
        tvError?.text = message ?: context.getString(R.string.coreui_state_default_error)
    }

    /**
     * 设置错误信息（使用 string 资源）
     */
    fun setErrorMessage(@StringRes resId: Int) {
        setErrorMessage(context.getString(resId))
    }

    private fun setVisibleView(target: View?) {
        contentViews.forEach { it.visibility = View.GONE }
        loadingView?.visibility = if (target == loadingView) View.VISIBLE else View.GONE
        emptyView?.visibility = if (target == emptyView) View.VISIBLE else View.GONE
        errorView?.visibility = if (target == errorView) View.VISIBLE else View.GONE
    }

    private fun replaceView(holder: KMutableProperty0<View?>, newView: View) {
        val oldView = holder.get()
        if (oldView != null) {
            removeView(oldView)
        }
        holder.set(newView)
        addView(newView)
        newView.visibility = View.GONE
    }
}


