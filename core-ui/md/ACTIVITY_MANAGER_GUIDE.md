# ActivityManager 使用指南

## 📋 概述

`ActivityManager` 是一个全局的 Activity 管理器，用于统一管理应用中的所有 Activity。它提供了 Activity 栈管理、统一的启动方法、退出应用等功能。

---

## 🚀 初始化

在 `Application.onCreate()` 中初始化：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ActivityManager.init(this)
    }
}
```

---

## ✨ 核心功能

### 1. 启动 Activity

#### 使用泛型启动（推荐）

```kotlin
// 基础启动
ActivityManager.startActivity<DetailActivity>(this)

// 带参数启动
ActivityManager.startActivity<DetailActivity>(this) {
    putExtra("key", "value")
    putExtra("id", 123)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
```

#### 使用 Class 启动

```kotlin
ActivityManager.startActivity(this, DetailActivity::class.java) {
    putExtra("key", "value")
}
```

#### 启动并关闭当前 Activity

```kotlin
// 使用泛型
ActivityManager.startActivityAndFinish<DetailActivity>(this) {
    putExtra("key", "value")
}

// 使用 Class
ActivityManager.startActivityAndFinish(this, DetailActivity::class.java) {
    putExtra("key", "value")
}
```

#### 启动并清空任务栈（用于登录等场景）

```kotlin
// 使用泛型
ActivityManager.startActivityAndClearTask<LoginActivity>(this) {
    putExtra("from", "logout")
}

// 使用 Class
ActivityManager.startActivityAndClearTask(this, LoginActivity::class.java) {
    putExtra("from", "logout")
}
```

#### 启动并返回结果

```kotlin
// 使用泛型
ActivityManager.startActivityForResult<SelectImageActivity>(
    activity = this,
    requestCode = REQUEST_CODE_SELECT_IMAGE
) {
    putExtra("maxCount", 9)
}

// 使用 Class
ActivityManager.startActivityForResult(
    activity = this,
    clazz = SelectImageActivity::class.java,
    requestCode = REQUEST_CODE_SELECT_IMAGE
) {
    putExtra("maxCount", 9)
}
```

### 2. 获取当前 Activity

```kotlin
val currentActivity = ActivityManager.getCurrentActivity()
if (currentActivity != null) {
    // 使用当前 Activity
    currentActivity.showError("操作失败")
}
```

### 3. 查找 Activity

```kotlin
// 查找指定类型的 Activity
val detailActivity = ActivityManager.findActivity(DetailActivity::class.java)
if (detailActivity != null) {
    // Activity 存在
}

// 检查 Activity 是否存在
if (ActivityManager.hasActivity(DetailActivity::class.java)) {
    // Activity 存在
}
```

### 4. 关闭 Activity

#### 关闭指定 Activity

```kotlin
// 关闭指定类型的 Activity
ActivityManager.finishActivity(DetailActivity::class.java)
```

#### 关闭除指定 Activity 外的所有 Activity

```kotlin
// 关闭除 MainActivity 外的所有 Activity
ActivityManager.finishAllActivitiesExcept(MainActivity::class.java)
```

#### 关闭所有 Activity

```kotlin
ActivityManager.finishAllActivities()
```

### 5. 退出应用

```kotlin
// 退出应用（关闭所有 Activity 并退出进程）
ActivityManager.exitApp()
```

### 6. 返回到指定 Activity

如果栈中存在该 Activity，则关闭其上的所有 Activity；如果不存在，则启动该 Activity。

```kotlin
// 使用泛型
ActivityManager.backToActivity<MainActivity>(this) {
    putExtra("from", "back")
}

// 使用 Class
ActivityManager.backToActivity(this, MainActivity::class.java) {
    putExtra("from", "back")
}
```

### 7. 获取 Activity 栈信息

```kotlin
// 获取 Activity 栈
val stack = ActivityManager.getActivityStack()
stack.forEach { activity ->
    println("Activity: ${activity.javaClass.simpleName}")
}

// 获取 Activity 栈大小
val size = ActivityManager.getActivityStackSize()
println("当前有 $size 个 Activity")
```

---

## 📝 完整示例

### 示例 1：登录后跳转到主页

```kotlin
class LoginActivity : BaseActivity<ActivityLoginBinding>() {
    
    private fun onLoginSuccess() {
        // 登录成功后，清空任务栈并跳转到主页
        ActivityManager.startActivityAndClearTask<MainActivity>(this) {
            putExtra("from", "login")
        }
    }
}
```

### 示例 2：从详情页返回到列表页

```kotlin
class DetailActivity : BaseActivity<ActivityDetailBinding>() {
    
    private fun goBackToList() {
        // 返回到列表页（如果存在则关闭当前页，不存在则启动）
        ActivityManager.backToActivity<ListActivity>(this)
    }
}
```

### 示例 3：退出登录

```kotlin
class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {
    
    private fun logout() {
        // 关闭除登录页外的所有 Activity
        ActivityManager.finishAllActivitiesExcept(LoginActivity::class.java)
        
        // 或者直接退出应用
        // ActivityManager.exitApp()
    }
}
```

### 示例 4：在非 Activity 中启动 Activity

```kotlin
class MyRepository {
    fun openDetail(context: Context, id: Int) {
        // 在 Repository 或其他非 Activity 类中启动 Activity
        ActivityManager.startActivity<DetailActivity>(context) {
            putExtra("id", id)
        }
    }
}
```

### 示例 5：检查并关闭重复的 Activity

```kotlin
class MainActivity : BaseActivity<ActivityMainBinding>() {
    
    private fun openDetail(id: Int) {
        // 如果详情页已存在，先关闭它
        if (ActivityManager.hasActivity(DetailActivity::class.java)) {
            ActivityManager.finishActivity(DetailActivity::class.java)
        }
        
        // 启动新的详情页
        ActivityManager.startActivity<DetailActivity>(this) {
            putExtra("id", id)
        }
    }
}
```

### 示例 6：在 Fragment 中使用

```kotlin
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    
    private fun openDetail(id: Int) {
        // 在 Fragment 中启动 Activity
        ActivityManager.startActivity<DetailActivity>(requireContext()) {
            putExtra("id", id)
        }
    }
}
```

---

## 🎯 最佳实践

### 1. 统一使用 ActivityManager 启动 Activity

```kotlin
// ✅ 推荐：使用 ActivityManager
ActivityManager.startActivity<DetailActivity>(this) {
    putExtra("id", id)
}

// ❌ 不推荐：直接使用 Intent
val intent = Intent(this, DetailActivity::class.java)
intent.putExtra("id", id)
startActivity(intent)
```

### 2. 登录场景使用清空任务栈

```kotlin
// ✅ 推荐：登录后清空任务栈
ActivityManager.startActivityAndClearTask<MainActivity>(this)

// ❌ 不推荐：普通启动（可能通过返回键回到登录页）
ActivityManager.startActivity<MainActivity>(this)
```

### 3. 退出应用时使用 exitApp

```kotlin
// ✅ 推荐：使用 exitApp
ActivityManager.exitApp()

// ❌ 不推荐：只关闭所有 Activity（可能还有后台任务）
ActivityManager.finishAllActivities()
```

### 4. 返回到指定页面使用 backToActivity

```kotlin
// ✅ 推荐：返回到指定页面
ActivityManager.backToActivity<MainActivity>(this)

// ❌ 不推荐：手动关闭多个 Activity
ActivityManager.finishActivity(DetailActivity::class.java)
ActivityManager.finishActivity(EditActivity::class.java)
// ...
```

### 5. 在非 Activity 中启动时注意 Context 类型

```kotlin
// ✅ 推荐：ActivityManager 会自动处理 Context 类型
ActivityManager.startActivity<DetailActivity>(context) {
    putExtra("id", id)
}

// 如果 context 不是 Activity，会自动添加 FLAG_ACTIVITY_NEW_TASK
```

---

## ⚠️ 注意事项

1. **初始化**：必须在 `Application.onCreate()` 中调用 `ActivityManager.init(application)`
2. **Context 类型**：在非 Activity 中启动时，会自动添加 `FLAG_ACTIVITY_NEW_TASK` 标志
3. **Activity 栈**：Activity 栈是线程安全的，使用 `CopyOnWriteArrayList` 实现
4. **退出应用**：`exitApp()` 会强制退出进程，请谨慎使用
5. **日志**：ActivityManager 会记录详细的日志，方便调试

---

## 🔍 调试技巧

### 查看 Activity 栈

```kotlin
// 打印所有 Activity
ActivityManager.getActivityStack().forEachIndexed { index, activity ->
    Timber.d("Activity[$index]: ${activity.javaClass.simpleName}")
}
```

### 检查当前 Activity

```kotlin
val current = ActivityManager.getCurrentActivity()
Timber.d("当前 Activity: ${current?.javaClass?.simpleName ?: "无"}")
```

---

## ✨ 总结

ActivityManager 提供了：

- ✅ **统一的启动方法**：简化 Activity 启动代码
- ✅ **Activity 栈管理**：自动追踪所有 Activity
- ✅ **退出应用功能**：一键退出应用
- ✅ **查找和关闭**：灵活管理 Activity
- ✅ **返回到指定页面**：便捷的页面导航
- ✅ **线程安全**：使用线程安全的集合
- ✅ **详细日志**：方便调试和排查问题

**建议：在项目中统一使用 ActivityManager 来管理 Activity！**

