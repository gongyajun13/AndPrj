package com.jun.core.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.jun.core.ui.base.callback.ActivityCallback
import com.jun.core.ui.base.utils.BackPressHandler
import com.jun.core.ui.base.utils.KeyboardManager
import com.jun.core.ui.base.utils.MessageNotifier
import com.jun.core.ui.base.utils.StatusBarManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * BaseActivity 基类
 * 提供通用的 Activity 功能
 * 
 * 功能包括：
 * - ViewBinding 支持
 * - 消息提示（Snackbar）
 * - 软键盘管理
 * - 状态栏和导航栏配置
 * - 返回键处理
 * - Flow 收集（自动处理生命周期）
 * - 加载指示器管理
 * 
 * 使用示例：
 * ```kotlin
 * class MyActivity : BaseActivity<ActivityMainBinding>(), ActivityCallback {
 *     override fun createBinding() = ActivityMainBinding.inflate(layoutInflater)
 *     
 *     override fun setupViews() {
 *         statusBarManager.setStatusBarColor(Color.WHITE, lightIcons = true)
 *     }
 * }
 * ```
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity(), ActivityCallback {
    
    protected lateinit var binding: VB
        private set
    
    protected open val enableBackPressHandling: Boolean = true
    protected open val hideKeyboardOnBackPress: Boolean = true
    
    protected var savedState: Bundle? = null
        private set
    
    protected val statusBarManager: StatusBarManager by lazy {
        StatusBarManager(window)
    }
    
    private var backPressHandler: BackPressHandler? = null
    
    protected abstract fun createBinding(): VB
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedState = savedInstanceState
        
        binding = createBinding()
        setContentView(binding.root)
        
        if (enableBackPressHandling) {
            backPressHandler = BackPressHandler(
                onBackPress = { handleBackPress() },
                hideKeyboardOnBackPress = hideKeyboardOnBackPress
            )
            backPressHandler?.setupForActivity(this, onBackPressedDispatcher)
        }
        
        setupViews()
        setupObservers()
        setupListeners()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        backPressHandler?.remove()
    }
    
    @Deprecated("使用 OnBackPressedCallback 替代", ReplaceWith("onBackPressedCallback"))
    override fun onBackPressed() {
        if (hideKeyboardOnBackPress) {
            KeyboardManager.hideKeyboard(this)
        }
        super.onBackPressed()
    }
    
    override fun handleBackPress() {
        finish()
    }
    
    // ==================== 消息提示 ====================
    
    protected fun showError(message: String) {
        MessageNotifier.showError(binding.root, message)
    }
    
    protected fun showSuccess(message: String) {
        MessageNotifier.showSuccess(binding.root, message)
    }
    
    protected fun showWarning(message: String) {
        MessageNotifier.showWarning(binding.root, message)
    }
    
    protected fun showMessage(message: String) {
        MessageNotifier.showMessage(binding.root, message)
    }
    
    // ==================== 软键盘管理 ====================
    
    protected fun hideKeyboard() {
        KeyboardManager.hideKeyboard(this)
    }
    
    protected fun isKeyboardVisible(): Boolean {
        return KeyboardManager.isKeyboardVisible(binding.root)
    }
    
    // ==================== 状态栏和导航栏配置 ====================
    
    protected fun setFullScreen() = statusBarManager.setFullScreen()
    protected fun exitFullScreen() = statusBarManager.exitFullScreen()
    protected fun setImmersiveStatusBar(
        lightStatusBar: Boolean = false,
        lightNavigationBar: Boolean = false
    ) = statusBarManager.setImmersiveStatusBar(lightStatusBar, lightNavigationBar)
    
    @Suppress("DEPRECATION")
    protected fun setStatusBarColor(color: Int, lightIcons: Boolean = false) {
        statusBarManager.setStatusBarColor(color, lightIcons)
    }
    
    @Suppress("DEPRECATION")
    protected fun setNavigationBarColor(color: Int, lightIcons: Boolean = false) {
        statusBarManager.setNavigationBarColor(color, lightIcons)
    }
    
    protected fun setSystemBarsColor(
        statusBarColor: Int,
        navigationBarColor: Int,
        lightStatusBar: Boolean = false,
        lightNavigationBar: Boolean = false
    ) = statusBarManager.setSystemBarsColor(statusBarColor, navigationBarColor, lightStatusBar, lightNavigationBar)
    
    protected fun hideStatusBar() = statusBarManager.hideStatusBar()
    protected fun showStatusBar() = statusBarManager.showStatusBar()
    protected fun hideNavigationBar() = statusBarManager.hideNavigationBar()
    protected fun showNavigationBar() = statusBarManager.showNavigationBar()
    protected fun setKeepScreenOn(keepOn: Boolean = true) = statusBarManager.setKeepScreenOn(keepOn)
    
    // ==================== Flow 收集 ====================
    
    protected fun <T> Flow<T>.collectOnLifecycle(
        lifecycleOwner: LifecycleOwner = this@BaseActivity,
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
    
    // ==================== 工具方法 ====================
    
    protected fun isFirstCreate(): Boolean = savedState == null
    protected fun isConfigChange(): Boolean = savedState != null
    
    protected fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
    
    protected fun getNavigationBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
}
