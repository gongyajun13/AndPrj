# Android 适配方案指南

> 更新时间：2024-2025
> 基于项目：minSdk 24, targetSdk 36

---

## 📱 一、屏幕适配方案

### 1.1 主流方案对比

| 方案 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **SmallestWidth 限定符** | 官方推荐，稳定可靠 | 需要维护多套资源 | ⭐⭐⭐⭐⭐ |
| **今日头条方案** | 动态修改 density | 可能影响第三方库 | ⭐⭐⭐ |
| **AutoSize** | 简单易用 | 已停止维护 | ⭐⭐ |
| **ConstraintLayout + Guideline** | 响应式布局 | 需要重新设计布局 | ⭐⭐⭐⭐ |

### 1.2 推荐方案：SmallestWidth 限定符（官方方案）

**原理**：根据屏幕最小宽度（sw）提供不同资源

**实现步骤**：

1. **创建不同尺寸的资源目录**
```
res/
├── values/
│   └── dimens.xml          # 默认尺寸（手机）
├── values-sw600dp/
│   └── dimens.xml          # 平板（7寸）
├── values-sw720dp/
│   └── dimens.xml          # 平板（10寸）
└── values-sw840dp/
    └── dimens.xml          # 大屏设备
```

2. **定义尺寸资源**
```xml
<!-- values/dimens.xml -->
<resources>
    <dimen name="text_size_normal">14sp</dimen>
    <dimen name="padding_normal">16dp</dimen>
</resources>

<!-- values-sw600dp/dimens.xml -->
<resources>
    <dimen name="text_size_normal">16sp</dimen>
    <dimen name="padding_normal">24dp</dimen>
</resources>
```

3. **在布局中使用**
```xml
<TextView
    android:textSize="@dimen/text_size_normal"
    android:padding="@dimen/padding_normal" />
```

**优点**：
- ✅ 官方推荐，稳定可靠
- ✅ 适配效果好
- ✅ 不影响代码逻辑

**项目建议**：已在 `ContextExtensions.kt` 中提供了 `isTablet()` 方法，可以结合使用。

---

## 🔐 二、权限适配方案

### 2.1 当前项目方案

**已集成**：`XXPermissions` + `PermissionHelper`

**特点**：
- ✅ 统一的权限请求接口
- ✅ 自动处理权限描述和拦截
- ✅ 支持所有 Android 版本

### 2.2 权限适配要点

#### Android 6.0+ (API 23+)
- 运行时权限请求
- 已通过 `XXPermissions` 处理 ✅

#### Android 8.0+ (API 26+)
- 通知渠道（NotificationChannel）
- 后台位置限制

#### Android 10+ (API 29+)
- 分区存储（Scoped Storage）
- 已配置 `maxSdkVersion="29"` ✅

#### Android 11+ (API 30+)
- 包可见性（Package Visibility）
- 所有文件访问权限（MANAGE_EXTERNAL_STORAGE）

#### Android 12+ (API 31+)
- 模糊位置权限
- 精确位置权限分离

#### Android 13+ (API 33+)
- 细粒度媒体权限（READ_MEDIA_IMAGES/VIDEO/AUDIO）
- 通知权限（POST_NOTIFICATIONS）
- 已配置 ✅

#### Android 14+ (API 34+)
- 部分照片访问权限
- 健康数据权限

---

## 💾 三、存储适配方案

### 3.1 分区存储（Scoped Storage）

**Android 10+ 强制启用分区存储**

**适配方案**：

1. **使用 MediaStore API**
```kotlin
// 保存图片到公共目录
val values = ContentValues().apply {
    put(MediaStore.Images.Media.DISPLAY_NAME, "image.jpg")
    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
}

val uri = contentResolver.insert(
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    values
)
```

2. **使用应用专属目录**
```kotlin
// 应用专属外部存储（卸载时删除）
val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "image.jpg")

// 应用专属内部存储
val file = File(context.filesDir, "data.txt")
```

3. **FileProvider 配置**（已配置 ✅）
- 用于应用间文件共享
- Android 7.0+ 必需

### 3.2 存储权限适配

**已配置的权限**：
```xml
<!-- Android 13 以下 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />

<!-- Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
```

---

## 🔔 四、通知适配方案

### 4.1 Android 8.0+ 通知渠道

**必需配置**：
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "重要通知",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "重要消息通知"
        enableVibration(true)
    }
    notificationManager.createNotificationChannel(channel)
}
```

### 4.2 Android 13+ 通知权限

**已配置**：
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**请求方式**：
```kotlin
// 使用项目中的权限请求
requestPermission(PermissionLists.getPostNotificationsPermission()) { ... }
```

---

## 🌓 五、Edge-to-Edge 适配（全面屏适配）

### 5.1 Android 15+ Edge-to-Edge

**适配要点**：

1. **设置窗口属性**
```kotlin
WindowCompat.setDecorFitsSystemWindows(window, false)
```

2. **处理 WindowInsets**
```kotlin
ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    v.setPadding(
        systemBars.left,
        systemBars.top,
        systemBars.right,
        systemBars.bottom
    )
    insets
}
```

3. **状态栏和导航栏颜色**
```kotlin
// 设置状态栏颜色
WindowCompat.setStatusBarColor(window, Color.TRANSPARENT)
WindowCompat.setNavigationBarColor(window, Color.TRANSPARENT)

// 设置状态栏图标颜色（浅色/深色）
WindowCompat.getInsetsController(window, view).apply {
    isAppearanceLightStatusBars = true  // 深色图标
    isAppearanceLightNavigationBars = true
}
```

**项目建议**：`CenterToolbar` 已处理状态栏高度，可在此基础上扩展。

---

## 🌙 六、深色模式适配

### 6.1 资源适配

**创建深色资源**：
```
res/
├── values/
│   └── colors.xml          # 浅色模式
└── values-night/
    └── colors.xml          # 深色模式
```

**定义颜色**：
```xml
<!-- values/colors.xml -->
<color name="background">#FFFFFF</color>
<color name="text_primary">#000000</color>

<!-- values-night/colors.xml -->
<color name="background">#121212</color>
<color name="text_primary">#FFFFFF</color>
```

### 6.2 代码适配

```kotlin
// 检查当前是否为深色模式
val isDarkMode = (resources.configuration.uiMode and 
    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

// 监听深色模式变化
override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    val isDarkMode = (newConfig.uiMode and 
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    // 更新 UI
}
```

---

## 📐 七、折叠屏适配

### 7.1 检测折叠屏

```kotlin
// 使用 Jetpack WindowManager
implementation("androidx.window:window:1.2.0")

val windowInfoTracker = WindowInfoTracker.getOrCreate(this)
windowInfoTracker.currentWindowLayoutInfo(this)
    .collect { layoutInfo ->
        val foldingFeature = layoutInfo.displayFeatures
            .filterIsInstance<FoldingFeature>()
            .firstOrNull()
        
        if (foldingFeature != null) {
            val isTableTop = foldingFeature.state == FoldingFeature.State.HALF_OPENED
            val orientation = foldingFeature.orientation
            // 调整布局
        }
    }
```

### 7.2 布局适配

- 使用 `ConstraintLayout` 的 `Guideline` 和 `Barrier`
- 使用 `MotionLayout` 实现流畅的布局切换
- 响应式布局设计

---

## 🪟 八、多窗口适配

### 8.1 分屏模式

**配置**：
```xml
<activity
    android:name=".MainActivity"
    android:resizeableActivity="true"
    android:supportsPictureInPicture="true" />
```

**处理配置变化**：
```kotlin
override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // 重新计算布局
    updateLayoutForMultiWindow()
}
```

### 8.2 画中画（PiP）

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    enterPictureInPictureMode(
        PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
    )
}
```

---

## 🔧 九、系统版本适配最佳实践

### 9.1 版本检查

```kotlin
// 使用 Build.VERSION.SDK_INT
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    // Android 13+ 代码
} else {
    // 旧版本代码
}

// 使用版本检查扩展函数（推荐）
if (isAtLeastT()) {
    // Android 13+ 代码
}
```

### 9.2 使用 AndroidX 兼容库

**已使用的兼容库**：
- ✅ `androidx.core:core-ktx` - 核心扩展
- ✅ `androidx.appcompat:appcompat` - 向后兼容
- ✅ `androidx.activity:activity-ktx` - Activity 扩展
- ✅ `androidx.fragment:fragment-ktx` - Fragment 扩展

### 9.3 使用 WindowCompat

```kotlin
// 替代 Window 方法
WindowCompat.setDecorFitsSystemWindows(window, false)
WindowCompat.setStatusBarColor(window, color)
WindowCompat.getInsetsController(window, view)
```

---

## 📊 十、项目当前适配状态

### ✅ 已适配

1. **权限适配**
   - ✅ 运行时权限（XXPermissions）
   - ✅ 分区存储权限
   - ✅ 通知权限（Android 13+）
   - ✅ 媒体权限（Android 13+）

2. **存储适配**
   - ✅ FileProvider 配置
   - ✅ 分区存储权限声明

3. **基础适配**
   - ✅ 屏幕尺寸检测（isTablet）
   - ✅ dp/sp/px 转换工具
   - ✅ 状态栏/导航栏高度获取

### 🔄 建议补充

1. **Edge-to-Edge 适配**
   - 在 BaseActivity 中添加统一处理
   - 完善 WindowInsets 处理

2. **深色模式适配**
   - 创建深色资源文件
   - 添加深色模式检测工具

3. **折叠屏适配**
   - 集成 WindowManager
   - 添加折叠屏检测

4. **屏幕适配**
   - 使用 SmallestWidth 限定符
   - 创建多套 dimens 资源

---

## 🛠️ 十一、推荐工具和库

### 11.1 官方工具

- **Android Studio Layout Inspector** - 布局检查
- **Device Manager** - 多设备测试
- **Lint** - 代码检查

### 11.2 第三方库

- **XXPermissions** - 权限请求（已集成 ✅）
- **DeviceCompat** - 设备兼容（已集成 ✅）
- **WindowManager** - 窗口管理（Jetpack）
- **Material Design Components** - Material 组件（已集成 ✅）

---

## 📝 十二、适配检查清单

### 开发阶段

- [ ] 在多个设备上测试（不同尺寸、不同系统版本）
- [ ] 测试横竖屏切换
- [ ] 测试深色模式
- [ ] 测试权限拒绝场景
- [ ] 测试存储访问
- [ ] 测试通知显示

### 发布前

- [ ] 检查所有权限声明
- [ ] 检查 targetSdk 兼容性
- [ ] 检查 ProGuard 规则
- [ ] 测试 Edge-to-Edge 显示
- [ ] 测试折叠屏（如有）

---

## 🎯 总结

### 核心适配原则

1. **使用 AndroidX 兼容库** - 自动处理大部分兼容性问题
2. **版本检查** - 使用 `Build.VERSION.SDK_INT` 或扩展函数
3. **资源限定符** - 使用 SmallestWidth 等限定符适配不同屏幕
4. **权限适配** - 使用统一的权限请求框架
5. **测试覆盖** - 在真实设备上测试

### 项目优势

- ✅ 已集成完善的权限请求框架
- ✅ 已配置 FileProvider
- ✅ 已提供基础适配工具
- ✅ 使用最新 AndroidX 库

### 下一步建议

1. 完善 Edge-to-Edge 适配
2. 添加深色模式支持
3. 使用 SmallestWidth 进行屏幕适配
4. 考虑折叠屏适配（如需要）

---

**参考资源**：
- [Android 官方适配指南](https://developer.android.com/guide)
- [Material Design 适配指南](https://material.io/design)
- [AndroidX 库文档](https://developer.android.com/jetpack/androidx)

