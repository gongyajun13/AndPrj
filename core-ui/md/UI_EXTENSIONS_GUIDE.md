# Core-UI 模块扩展功能使用指南

## 📦 新增功能概览

core-ui 模块已扩展，新增了以下功能：

### 1. 状态视图 (StateLayout) ⭐ 推荐

通用的状态容器组件，用于统一管理加载、空、错误和内容视图的切换。配合 `bindListState` 扩展函数，一行代码即可完成状态绑定。

**快速开始**：
```kotlin
binding.stateLayout.bindListState(
    owner = this,
    stateFlow = viewModel.uiState
) { users ->
    adapter.submitList(users)
}
```

详细使用请参考 [状态视图使用指南](#8-状态视图-statelayout)。

### 2. View 扩展函数 (ViewExtensions.kt)

#### 动画相关
```kotlin
// 显示/隐藏动画
view.showWithAnimation(duration = 300)
view.hideWithAnimation(duration = 300) { /* 动画结束回调 */ }

// 淡入淡出
view.fadeIn(duration = 300)
view.fadeOut(duration = 300)

// 滑动动画
view.slideIn(duration = 300, fromBottom = true)
view.slideOut(duration = 300, toBottom = true)

// 设置透明度（带动画）
view.setAlpha(0.5f, duration = 300)

// 设置缩放（带动画）
view.setScale(0.8f, 0.8f, duration = 300)

// 设置旋转（带动画）
view.setRotation(45f, duration = 300)

// 设置平移（带动画）
view.setTranslation(x = 10f, y = 10f, duration = 300)
```

#### 点击防抖
```kotlin
// 防抖点击（时间间隔方式）
view.setOnClickListenerDebounced(debounceTime = 500) { view ->
    // 处理点击
}

// 防抖点击（协程方式）
view.setOnClickListenerDebounced(scope, debounceTime = 500) { view ->
    // 处理点击
}

// 简化写法
view.click(debounceTime = 500) { view ->
    // 处理点击
}

// 长按
view.longClick { view ->
    // 处理长按
    true
}
```

#### 可见性控制
```kotlin
// 带动画的可见性切换
view.setVisibleWithAnimation(visible = true, duration = 300)

// 设置尺寸
view.setWidth(100)
view.setHeight(100)
view.setSize(100, 100)

// 设置边距
view.setMargins(left = 16, top = 16, right = 16, bottom = 16)

// 设置内边距
view.setPadding(16)

// 设置背景透明度
view.setBackgroundAlpha(0.5f)

// 启用/禁用（带透明度变化）
view.setEnabled(enabled = false, alphaWhenDisabled = 0.5f)
```

#### 位置和可见性检查
```kotlin
// 获取 View 在屏幕中的位置
val (x, y) = view.getLocationOnScreen()

// 检查 View 是否在屏幕中可见
val isVisible = view.isVisibleOnScreen()
```

### 3. RecyclerView 扩展 (RecyclerViewExtensions.kt)

#### LayoutManager 设置
```kotlin
// 垂直布局
recyclerView.setVerticalLayoutManager()

// 水平布局
recyclerView.setHorizontalLayoutManager()

// Grid 布局
recyclerView.setGridLayoutManager(spanCount = 2)

// 瀑布流布局
recyclerView.setStaggeredGridLayoutManager(spanCount = 2)
```

#### 分割线
```kotlin
// 垂直分割线
recyclerView.addVerticalDivider()

// 水平分割线
recyclerView.addHorizontalDivider()
```

#### 滚动控制
```kotlin
// 滚动到顶部
recyclerView.scrollToTop(smooth = true)

// 滚动到底部
recyclerView.scrollToBottom(smooth = true)

// 检查是否滚动到底部
val isAtBottom = recyclerView.isScrolledToBottom(threshold = 5)

// 检查是否滚动到顶部
val isAtTop = recyclerView.isScrolledToTop(threshold = 5)
```

#### ViewGroup 扩展
```kotlin
// 获取 LayoutInflater
val inflater = viewGroup.inflater()

// 直接 inflate layout
val view = viewGroup.inflate(R.layout.item_layout, attachToRoot = false)
```

### 4. Dialog 和 Snackbar 扩展 (DialogExtensions.kt)

#### AlertDialog
```kotlin
// 简单对话框
context.showAlertDialog(
    title = "标题",
    message = "消息内容",
    positiveText = "确定",
    negativeText = "取消",
    onPositive = { /* 确定回调 */ },
    onNegative = { /* 取消回调 */ }
)

// 确认对话框
context.showConfirmDialog(
    title = "确认",
    message = "确定要执行此操作吗？",
    onConfirm = { /* 确认回调 */ }
)

// 列表对话框
context.showListDialog(
    title = "选择",
    items = arrayOf("选项1", "选项2", "选项3"),
    onItemClick = { index, item -> /* 处理点击 */ }
)

// 单选对话框
context.showSingleChoiceDialog(
    title = "单选",
    items = arrayOf("选项1", "选项2"),
    selectedIndex = 0,
    onItemSelected = { index, item -> /* 处理选择 */ }
)

// 多选对话框
context.showMultiChoiceDialog(
    title = "多选",
    items = arrayOf("选项1", "选项2", "选项3"),
    checkedItems = booleanArrayOf(true, false, false),
    onConfirm = { selectedIndices -> /* 处理确认 */ }
)
```

#### Snackbar
```kotlin
// 普通消息
view.showSnackbar("消息内容")

// 成功消息
view.showSuccessSnackbar("操作成功")

// 错误消息
view.showErrorSnackbar("操作失败")

// 警告消息
view.showWarningSnackbar("警告信息")

// 带操作按钮
view.showSnackbar(
    message = "消息内容",
    actionText = "撤销",
    action = { /* 撤销操作 */ }
)

// Fragment 中使用
fragment.showSnackbar("消息内容")
```

### 5. BaseActivity 和 BaseFragment (base/)

#### BaseActivity
```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    
    override fun createBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }
    
    override fun setupViews() {
        // 设置视图
    }
    
    override fun setupObservers() {
        // 设置观察者
        viewModel.uiState.collectOnLifecycle { state ->
            // 处理状态
        }
    }
    
    override fun setupListeners() {
        // 设置监听器
    }
    
    // 显示消息
    showSuccess("操作成功")
    showError("操作失败")
    showWarning("警告信息")
    showMessage("普通消息")
    
    // 全屏和沉浸式
    setFullScreen()
    setImmersiveStatusBar()
}
```

#### BaseFragment
```kotlin
class MyFragment : BaseFragment<FragmentMyBinding>() {
    
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMyBinding {
        return FragmentMyBinding.inflate(inflater, container, false)
    }
    
    override fun setupViews() {
        // 设置视图
    }
    
    override fun setupObservers() {
        // 设置观察者
        viewModel.uiState.collectOnLifecycle { state ->
            // 处理状态
        }
    }
    
    // 显示消息
    showSuccess("操作成功")
    showError("操作失败")
}
```

### 6. ViewBinding 扩展 (ViewBindingExtensions.kt)

```kotlin
// Activity 中使用
class MainActivity : AppCompatActivity() {
    private val binding = binding.root.inflateBinding<ActivityMainBinding>()
}

// Fragment 中使用
class MyFragment : Fragment() {
    private val binding = inflateBinding<FragmentMyBinding>(layoutInflater, container)
}

// 从 View 绑定
val binding = view.bindView<ItemUserBinding>()
```

### 7. BaseAdapter (adapter/BaseAdapter.kt)

```kotlin
class UserAdapter : BaseAdapter<User, ItemUserBinding>(
    diffCallback = object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
) {
    override fun createBinding(parent: ViewGroup, viewType: Int): ItemUserBinding {
        return ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }
    
    override fun bind(binding: ItemUserBinding, item: User, position: Int) {
        binding.tvUserName.text = item.name
        binding.tvUserEmail.text = item.email
    }
    
    override fun onItemClick(binding: ItemUserBinding, item: User, position: Int) {
        // 处理点击
    }
}
```

### 9. 键盘扩展 (KeyboardExtensions.kt)

```kotlin
// 显示键盘
editText.showKeyboard()

// 隐藏键盘
editText.hideKeyboard()
view.hideKeyboard()
activity.hideKeyboard()
fragment.hideKeyboard()

// 切换键盘
editText.toggleKeyboard()

// 检查键盘是否显示
val isVisible = view.isKeyboardVisible()

// EditText 扩展
editText.setTextAndMoveCursor("文本")
editText.clear()
val text = editText.getTextTrimmed()
val isEmpty = editText.isEmpty()
val isNotEmpty = editText.isNotEmpty()
```

### 8. 状态视图 (StateLayout)

`StateLayout` 是一个通用的状态容器组件，用于在同一个区域内切换显示：**加载视图**、**空视图**、**错误视图**和**内容视图**。它完全封装了状态切换逻辑，让页面代码更简洁。

#### 基本使用

##### 1. 在 XML 中包裹内容视图

```xml
<com.jun.core.ui.widget.StateLayout
    android:id="@+id/stateLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- 这里是你的内容视图（如 RecyclerView、ScrollView 等） -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvUsers"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</com.jun.core.ui.widget.StateLayout>
```

##### 2. 在代码中绑定 UiState（推荐方式）

**列表场景 - 使用 `bindListState`（最简单）**

```kotlin
class MainActivity : AppCompatActivity() {
    private val viewModel: UserViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ...
        
        setupRecyclerView()
        observeUiState()
    }
    
    private fun observeUiState() {
        // 一行代码完成状态绑定 + 数据提交
        binding.stateLayout.bindListState(
            owner = this,
            stateFlow = viewModel.uiState
        ) { users ->
            userAdapter.submitList(users)
        }
        
        // 可选：设置错误重试
        binding.stateLayout.setOnRetryClickListener {
            viewModel.refresh()
        }
    }
}
```

**通用场景 - 使用 `bindState`**

```kotlin
binding.stateLayout.bindState(
    owner = this,
    stateFlow = viewModel.uiState,
    onSuccess = { data ->
        // 处理成功数据
        updateUI(data)
    },
    onError = { message, throwable ->
        // 可选：自定义错误处理（默认已显示错误视图）
        showSnackbar("错误: $message")
    }
)
```

##### 3. 手动控制状态（不推荐，但支持）

```kotlin
// 显示加载
binding.stateLayout.showLoading()

// 显示空视图
binding.stateLayout.showEmpty()

// 显示错误视图
binding.stateLayout.showError()
binding.stateLayout.setErrorMessage("自定义错误信息")

// 显示内容视图
binding.stateLayout.showContent()

// 根据 UiState 渲染（自动判断）
binding.stateLayout.renderState(viewModel.uiState.value)
```

#### 自定义状态视图

##### 自定义加载视图

```kotlin
// 方式1：通过 View 对象
val customLoadingView = LayoutInflater.from(context)
    .inflate(R.layout.custom_loading, null, false)
binding.stateLayout.setLoadingView(customLoadingView)

// 方式2：通过布局 ID
binding.stateLayout.setLoadingView(R.layout.custom_loading)
```

##### 自定义空视图

```kotlin
binding.stateLayout.setEmptyView(R.layout.custom_empty)
// 或
binding.stateLayout.setEmptyView(customEmptyView)
```

##### 自定义错误视图

```kotlin
binding.stateLayout.setErrorView(R.layout.custom_error)
// 注意：自定义错误视图需要包含 id 为 tvErrorMessage 的 TextView
// 和 id 为 btnRetry 的 Button（如果使用默认重试功能）
```

#### 错误重试处理

```kotlin
// 设置重试点击回调
binding.stateLayout.setOnRetryClickListener {
    viewModel.refresh()  // 或 viewModel.loadUsers()
}

// 自定义错误信息
binding.stateLayout.setErrorMessage("网络连接失败，请检查网络设置")
// 或使用字符串资源
binding.stateLayout.setErrorMessage(R.string.error_network)
```

#### 完整示例

**Activity 示例**

```kotlin
@AndroidEntryPoint
class UserListActivity : AppCompatActivity() {
    private val viewModel: UserViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        observeUiState()
    }
    
    private fun setupRecyclerView() {
        userAdapter = UserAdapter()
        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(this@UserListActivity)
            adapter = userAdapter
        }
    }
    
    private fun observeUiState() {
        // 绑定状态 + 自动处理列表数据
        binding.stateLayout.bindListState(
            owner = this,
            stateFlow = viewModel.uiState
        ) { users ->
            userAdapter.submitList(users)
        }
        
        // 设置重试
        binding.stateLayout.setOnRetryClickListener {
            viewModel.refresh()
        }
    }
}
```

**Fragment 示例**

```kotlin
@AndroidEntryPoint
class UserListFragment : Fragment() {
    private val viewModel: UserViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeUiState()
    }
    
    private fun observeUiState() {
        // 使用 viewLifecycleOwner（Fragment 必须）
        binding.stateLayout.bindListState(
            owner = viewLifecycleOwner,
            stateFlow = viewModel.uiState
        ) { users ->
            userAdapter.submitList(users)
        }
        
        binding.stateLayout.setOnRetryClickListener {
            viewModel.refresh()
        }
    }
}
```

#### 状态视图工作原理

1. **初始状态**：默认显示内容视图（如果有子 View）
2. **Loading 状态**：显示加载视图，隐藏内容和错误视图
3. **Success 状态**：
   - 如果数据是 `Collection` 且为空 → 显示空视图
   - 否则 → 显示内容视图
4. **Error 状态**：显示错误视图，自动设置错误信息
5. **Empty 状态**：显示空视图

#### 注意事项

1. **生命周期管理**：`bindState` 和 `bindListState` 会自动处理生命周期，无需手动取消订阅
2. **Fragment 使用**：Fragment 中必须使用 `viewLifecycleOwner`，不能使用 `this`
3. **内容视图**：StateLayout 的子 View 会被自动识别为"内容视图"，状态视图会覆盖在内容视图之上
4. **自定义视图**：自定义状态视图时，确保布局符合 Material Design 规范
5. **错误视图 ID**：如果使用默认重试功能，自定义错误视图需要包含 `tvErrorMessage` 和 `btnRetry` 这两个 ID

## 📊 功能统计

### 新增文件（10 个）
1. ✅ `extension/ViewExtensions.kt` - View 扩展函数（动画、点击防抖、可见性等）
2. ✅ `extension/RecyclerViewExtensions.kt` - RecyclerView 扩展
3. ✅ `extension/DialogExtensions.kt` - Dialog 和 Snackbar 扩展
4. ✅ `extension/KeyboardExtensions.kt` - 键盘相关扩展
5. ✅ `extension/ViewBindingExtensions.kt` - ViewBinding 扩展
6. ✅ `extension/StateLayoutExtensions.kt` - StateLayout 绑定扩展函数
7. ✅ `widget/StateLayout.kt` - 通用状态视图容器
8. ✅ `base/BaseActivity.kt` - BaseActivity 基类
9. ✅ `base/BaseFragment.kt` - BaseFragment 基类
10. ✅ `adapter/BaseAdapter.kt` - BaseAdapter 基类

### 布局资源（3 个）
1. ✅ `res/layout/coreui_view_state_loading.xml` - 默认加载视图
2. ✅ `res/layout/coreui_view_state_empty.xml` - 默认空视图
3. ✅ `res/layout/coreui_view_state_error.xml` - 默认错误视图

### 原有文件（5 个）
1. ✅ `state/UiState.kt`
2. ✅ `viewmodel/BaseViewModel.kt`
3. ✅ `viewmodel/PagingViewModel.kt`
4. ✅ `event/SingleLiveEvent.kt`
5. ✅ `extension/ImageExtensions.kt`

### 总计
- **15 个 Kotlin 文件**（从 5 个增加到 15 个）
- **3 个布局资源文件**
- **功能覆盖**：View、RecyclerView、Dialog、Snackbar、键盘、ViewBinding、状态视图、BaseActivity、BaseFragment、BaseAdapter

## 🎯 使用建议

1. **状态视图**：优先使用 `StateLayout + bindListState` 处理列表页面的加载/空/错误状态，一行代码完成状态绑定
2. **View 动画**：使用 `showWithAnimation`、`fadeIn`、`slideIn` 等提供流畅的 UI 体验
3. **点击防抖**：使用 `click()` 或 `setOnClickListenerDebounced()` 防止重复点击
4. **RecyclerView**：使用扩展函数快速设置 LayoutManager 和分割线
5. **Dialog/Snackbar**：使用扩展函数简化对话框和消息提示的创建
6. **BaseActivity/BaseFragment**：继承基类获得统一的消息提示和生命周期管理
7. **BaseAdapter**：使用 BaseAdapter 简化 RecyclerView Adapter 的创建

## ✨ 总结

core-ui 模块现在提供了**完整的 UI 开发工具集**：
- ✅ 通用状态视图容器（StateLayout）
- ✅ 丰富的 View 扩展函数
- ✅ RecyclerView 工具
- ✅ Dialog 和 Snackbar 扩展
- ✅ 键盘管理
- ✅ BaseActivity 和 BaseFragment 基类
- ✅ BaseAdapter 基类
- ✅ ViewBinding 扩展

**core-ui 模块已扩展完成！** 🎉


