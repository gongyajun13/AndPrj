package com.jun.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * BaseFragment 基类
 * 提供通用的 Fragment 功能
 * 
 * 功能包括：
 * - ViewBinding 支持（自动处理生命周期）
 * - 消息提示（Snackbar）
 * - 软键盘管理
 * - 返回键处理
 * - Flow 收集（自动处理生命周期）
 * - 加载指示器管理
 * - 懒加载支持
 * - ViewPager2 支持
 * - 工具方法
 * 
 * 使用示例：
 * ```kotlin
 * class MyFragment : BaseFragment<FragmentMainBinding>(), FragmentCallback {
 *     override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) =
 *         FragmentMainBinding.inflate(inflater, container, false)
 *     
 *     override fun loadData() {
 *         // 懒加载数据
 *     }
 * }
 * ```
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment(), FragmentCallback {
    
    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("Binding is not available. Fragment view has been destroyed.")
    
    protected open val enableBackPressHandling: Boolean = true
    protected open val hideKeyboardOnBackPress: Boolean = true
    protected open val enableLazyLoad: Boolean = true
    
    protected val isInViewPager: Boolean get() = visibilityManager.isInViewPager
    protected val isUserVisible: Boolean get() = visibilityManager.isUserVisible
    
    private val visibilityManager: FragmentVisibilityManager by lazy {
        FragmentVisibilityManager(this, enableLazyLoad)
    }
    
    private var backPressHandler: BackPressHandler? = null
    
    protected var savedState: Bundle? = null
        private set
    
    protected abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedState = savedInstanceState
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = createBinding(inflater, container)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        visibilityManager.checkViewPager()
        
        if (enableBackPressHandling) {
            backPressHandler = BackPressHandler(
                onBackPress = { handleBackPress() },
                hideKeyboardOnBackPress = hideKeyboardOnBackPress
            )
            backPressHandler?.setupForFragment(this, viewLifecycleOwner)
        }
        
        setupViews()
        
        if (!enableLazyLoad) {
            setupObservers()
            setupListeners()
            loadData()
        }
    }
    
    override fun onResume() {
        super.onResume()
        visibilityManager.handleVisibilityChange(
            onVisible = { onVisible() },
            onInvisible = { onInvisible() },
            onLoadData = { loadData() },
            setupObservers = { setupObservers() },
            setupListeners = { setupListeners() }
        )
    }
    
    override fun onPause() {
        super.onPause()
        if (visibilityManager.isUserVisible) {
            onInvisible()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        backPressHandler?.remove()
        _binding = null
    }
    
    protected fun isDataLoaded(): Boolean = visibilityManager.isDataLoaded
    
    protected fun resetLazyLoad() {
        visibilityManager.resetLazyLoad { loadData() }
    }
    
    // ==================== 消息提示 ====================
    
    protected fun showError(message: String) {
        MessageNotifier.showError(view ?: binding.root, message)
    }
    
    protected fun showSuccess(message: String) {
        MessageNotifier.showSuccess(view ?: binding.root, message)
    }
    
    protected fun showWarning(message: String) {
        MessageNotifier.showWarning(view ?: binding.root, message)
    }
    
    protected fun showMessage(message: String) {
        MessageNotifier.showMessage(view ?: binding.root, message)
    }
    
    // ==================== 软键盘管理 ====================
    
    protected fun hideKeyboard() {
        KeyboardManager.hideKeyboard(this)
    }
    
    protected fun isKeyboardVisible(): Boolean {
        return view?.let { KeyboardManager.isKeyboardVisible(it) } ?: false
    }
    
    // ==================== Flow 收集 ====================
    
    protected fun <T> Flow<T>.collectOnLifecycle(
        lifecycleOwner: LifecycleOwner = viewLifecycleOwner,
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
        action: suspend (T) -> Unit
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(minActiveState) {
                collect(action)
            }
        }
    }
    
    protected fun <T> Flow<T>.collectOnResumed(action: suspend (T) -> Unit) {
        collectOnLifecycle(minActiveState = Lifecycle.State.RESUMED, action = action)
    }
    
    protected fun <T> Flow<T>.collectOnStarted(action: suspend (T) -> Unit) {
        collectOnLifecycle(minActiveState = Lifecycle.State.STARTED, action = action)
    }
    
    protected fun <T> Flow<T>.collectOnCreated(action: suspend (T) -> Unit) {
        collectOnLifecycle(minActiveState = Lifecycle.State.CREATED, action = action)
    }
    
    protected fun <T> Flow<T>.collectOnVisible(action: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { value ->
                    if (visibilityManager.isUserVisible) {
                        action(value)
                    }
                }
            }
        }
    }
    
    // ==================== 工具方法 ====================
    
    protected fun isFirstCreate(): Boolean = savedState == null
    protected fun isConfigChange(): Boolean = savedState != null
    
    protected fun requireAppCompatActivity(): AppCompatActivity {
        return requireActivity() as? AppCompatActivity
            ?: throw IllegalStateException("Activity must be AppCompatActivity")
    }
    
    protected fun isFragmentAdded(): Boolean = isAdded && activity != null
    protected fun isFragmentVisible(): Boolean = isAdded && isVisible && isResumed
    
    protected fun safeExecute(action: () -> Unit) {
        if (isFragmentAdded()) action()
    }
    
    protected fun safeExecuteIfVisible(action: () -> Unit) {
        if (isFragmentVisible()) action()
    }
    
    protected fun safeExecuteIfUserVisible(action: () -> Unit) {
        if (visibilityManager.isUserVisible) action()
    }
    
    protected fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
    
    protected fun getNavigationBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
    
    protected fun View.postDelayedIfVisible(delayMillis: Long, action: () -> Unit) {
        if (isFragmentVisible()) {
            postDelayed({
                if (isFragmentVisible()) action()
            }, delayMillis)
        }
    }
    
    protected fun View.postDelayedIfAdded(delayMillis: Long, action: () -> Unit) {
        if (isFragmentAdded()) {
            postDelayed({
                if (isFragmentAdded()) action()
            }, delayMillis)
        }
    }
    
    protected fun View.postDelayedIfUserVisible(delayMillis: Long, action: () -> Unit) {
        if (visibilityManager.isUserVisible) {
            postDelayed({
                if (visibilityManager.isUserVisible) action()
            }, delayMillis)
        }
    }
}

/**
 * ViewPager2 中的 Fragment 基类
 * 专门为 ViewPager2 优化的 Fragment
 */
abstract class ViewPagerFragment<VB : ViewBinding> : BaseFragment<VB>() {
    
    override val enableLazyLoad: Boolean = true
    
    protected open fun onPageVisible() {}
    protected open fun onPageInvisible() {}
    
    override fun onVisible() {
        super.onVisible()
        onPageVisible()
    }
    
    override fun onInvisible() {
        super.onInvisible()
        onPageInvisible()
    }
}
