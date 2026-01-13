package com.jun.andprj.ui.recycler

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.jun.andprj.R
import com.jun.andprj.databinding.ActivityRecyclerLayoutBinding
import com.jun.core.ui.base.BaseActivity

/**
 * RecyclerView 布局示例 Activity
 * 支持三种布局切换：线性布局、网格布局、瀑布流布局
 */
class RecyclerLayoutActivity : BaseActivity<ActivityRecyclerLayoutBinding>() {

    private lateinit var adapter: RecyclerLayoutAdapter
    private var layoutSelectorPopup: LayoutSelectorPopup? = null

    // 当前布局类型
    private var currentLayoutType = LayoutType.LINEAR

    // 示例数据
    private val demoItems = mutableListOf<RecyclerItem>().apply {
        repeat(50) { index ->
            add(
                RecyclerItem(
                    title = "Item ${index + 1}",
                    content = "这是第 ${index + 1} 个列表项的内容。".repeat((index % 3) + 1)
                )
            )
        }
    }

    override fun createBinding(): ActivityRecyclerLayoutBinding =
        ActivityRecyclerLayoutBinding.inflate(layoutInflater)

    override fun setupViews() {

        setupToolbar()
        // 状态栏白底黑字
        val white = ContextCompat.getColor(this, android.R.color.white)
        setStatusBarColor(white, lightIcons = true)
        setupRecyclerView()
    }

    private fun setupToolbar() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        binding.toolbar.setupSimple(
            title = "布局示例",
            titleTextColor = white,
            titleTextSizeSp = 18f,
            leftIcon = R.drawable.icon_back,
            leftIconTint = white,
            onLeftClick = { finish() },
            rightText = "切换布局 (线性)",
            rightTextColor = white,
            rightTextSizeSp = 14f,
            backgroundColor = ContextCompat.getColor(this, R.color.purple_200),
            onRightClick = { showLayoutSelector() }
        )
    }

    private fun setupRecyclerView() {
        adapter = RecyclerLayoutAdapter(demoItems)
        binding.recyclerView.adapter = adapter
        applyLayout(currentLayoutType)
    }

    /**
     * 显示布局选择器 PopupWindow
     */
    private fun showLayoutSelector() {
        val rightTextView = binding.toolbar.getRightTextView()
        
        // 创建或复用 PopupWindow
        if (layoutSelectorPopup == null) {
            layoutSelectorPopup = LayoutSelectorPopup(this) { layoutType ->
                switchToLayout(layoutType)
            }
        }
        
        val popup = layoutSelectorPopup!!
        
        // 设置当前选中状态
        popup.setCurrentLayoutType(currentLayoutType)
        
        // 配置并显示 PopupWindow（显示在按钮下方，中心对齐）
        popup.configure {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            outsideTouchable = true
            focusable = true
//            backgroundColor = android.graphics.Color.WHITE
        }.showBelow(
            anchor = rightTextView,
            offsetX = 0,
            offsetY = 8,
            alignToAnchor = false // 中心对齐，看起来更平衡
        )
    }
    
    /**
     * 切换到指定布局类型
     */
    private fun switchToLayout(layoutType: LayoutType) {
        currentLayoutType = layoutType
        applyLayout(currentLayoutType)
        updateToolbarRightText()
    }
    
    /**
     * 切换布局类型（循环切换，保留用于兼容）
     */
    private fun switchLayout() {
        currentLayoutType = when (currentLayoutType) {
            LayoutType.LINEAR -> LayoutType.GRID
            LayoutType.GRID -> LayoutType.STAGGERED
            LayoutType.STAGGERED -> LayoutType.LINEAR
        }
        applyLayout(currentLayoutType)
        updateToolbarRightText()
    }

    /**
     * 应用布局
     */
    private fun applyLayout(layoutType: LayoutType) {
        binding.recyclerView.layoutManager = when (layoutType) {
            LayoutType.LINEAR -> {
                LinearLayoutManager(this)
            }
            LayoutType.GRID -> {
                GridLayoutManager(this, 2)
            }
            LayoutType.STAGGERED -> {
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            }
        }
    }

    /**
     * 更新 Toolbar 右侧文字
     */
    private fun updateToolbarRightText() {
        val layoutName = when (currentLayoutType) {
            LayoutType.LINEAR -> "线性"
            LayoutType.GRID -> "网格"
            LayoutType.STAGGERED -> "瀑布流"
        }
        binding.toolbar.setRightText("切换布局 ($layoutName)")
    }

    override fun setupObservers() {
        // 暂无数据观察
    }

    /**
     * 布局类型枚举
     */
    enum class LayoutType {
        LINEAR,      // 线性布局
        GRID,        // 网格布局
        STAGGERED    // 瀑布流布局
    }

    /**
     * 列表项数据类
     */
    data class RecyclerItem(
        val title: String,
        val content: String
    )
}

