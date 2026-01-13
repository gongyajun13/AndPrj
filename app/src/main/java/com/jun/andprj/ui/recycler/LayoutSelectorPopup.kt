package com.jun.andprj.ui.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.jun.andprj.databinding.PopupLayoutSelectorBinding
import com.jun.core.ui.base.BasePopupWindow

/**
 * 布局选择器 PopupWindow
 */
class LayoutSelectorPopup(
    context: android.content.Context,
    private val onLayoutSelected: (RecyclerLayoutActivity.LayoutType) -> Unit
) : BasePopupWindow<PopupLayoutSelectorBinding>(context) {

    override fun createBinding(inflater: LayoutInflater): PopupLayoutSelectorBinding {
        return PopupLayoutSelectorBinding.inflate(inflater, null, false)
    }

    override fun setupViews() {
        // 设置当前选中状态（可以通过参数传入）
    }

    override fun setupListeners() {
        // 线性布局
        binding.tvLinear.setOnClickListener {
            onLayoutSelected(RecyclerLayoutActivity.LayoutType.LINEAR)
            dismiss()
        }

        // 网格布局
        binding.tvGrid.setOnClickListener {
            onLayoutSelected(RecyclerLayoutActivity.LayoutType.GRID)
            dismiss()
        }

        // 瀑布流布局
        binding.tvStaggered.setOnClickListener {
            onLayoutSelected(RecyclerLayoutActivity.LayoutType.STAGGERED)
            dismiss()
        }
    }

    /**
     * 设置当前选中的布局类型（高亮显示）
     */
    fun setCurrentLayoutType(layoutType: RecyclerLayoutActivity.LayoutType) {
        val selectedColor = ContextCompat.getColor(context, android.R.color.holo_blue_light)
        val normalColor = ContextCompat.getColor(context, android.R.color.transparent)

        // 重置所有项的背景
        binding.tvLinear.setBackgroundColor(normalColor)
        binding.tvGrid.setBackgroundColor(normalColor)
        binding.tvStaggered.setBackgroundColor(normalColor)

        // 设置选中项的背景
        when (layoutType) {
            RecyclerLayoutActivity.LayoutType.LINEAR -> {
                binding.tvLinear.setBackgroundColor(selectedColor)
            }
            RecyclerLayoutActivity.LayoutType.GRID -> {
                binding.tvGrid.setBackgroundColor(selectedColor)
            }
            RecyclerLayoutActivity.LayoutType.STAGGERED -> {
                binding.tvStaggered.setBackgroundColor(selectedColor)
            }
        }
    }
}









