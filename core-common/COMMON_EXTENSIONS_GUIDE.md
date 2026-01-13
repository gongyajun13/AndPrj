# Core-Common 模块扩展功能使用指南

## 📦 新增功能概览

core-common 模块已扩展，新增了以下功能：

### 1. String 扩展函数 (StringExtensions.kt)

#### 空值处理
```kotlin
// 检查是否为空或空白
val isEmpty = string.isNullOrBlank()
val isNotEmpty = string.isNotNullOrBlank()

// 返回默认值
val result = string.orDefault("默认值")
val result2 = string.orDefaultIfBlank("默认值")
```

#### 字符串操作
```kotlin
// 截断字符串
val truncated = "很长的字符串".truncate(maxLength = 10)

// 首字母大小写
val capitalized = "hello".capitalizeFirst() // "Hello"
val decapitalized = "Hello".decapitalizeFirst() // "hello"

// 移除空白字符
val noWhitespace = "a b c".removeWhitespace() // "abc"

// 移除指定字符
val removed = "abc123".remove('1', '2') // "abc3"

// 提取数字/字母
val numbers = "abc123".extractNumbers() // "123"
val letters = "abc123".extractLetters() // "abc"
val alphanumeric = "a1b2c3!".extractAlphanumeric() // "a1b2c3"
```

#### 格式验证
```kotlin
// 邮箱验证
val isEmail = "user@example.com".isEmail()

// URL 验证
val isUrl = "https://example.com".isUrl()

// 手机号验证
val isPhone = "13800138000".isPhoneNumber()

// 身份证号验证
val isIdCard = "110101199001011234".isIdCard()
```

#### 数据脱敏
```kotlin
// 隐藏手机号中间4位
val masked = "13800138000".maskPhone() // "138****8000"

// 隐藏邮箱用户名
val masked = "user@example.com".maskEmail() // "us***@example.com"
```

#### 类型转换（安全）
```kotlin
// 安全转换为数字类型
val int = "123".toIntOrZero()
val long = "123".toLongOrZero()
val double = "123.45".toDoubleOrZero()
val float = "123.45".toFloatOrZero()
val boolean = "true".toBooleanOrFalse()
```

#### 字符串操作
```kotlin
// 重复字符串
val repeated = "abc".repeat(3) // "abcabcabc"

// 插入字符串
val inserted = "hello".insert(5, " world") // "hello world"

// 移除指定范围
val removed = "hello".removeRange(2, 4) // "heo"
```

### 2. Collection 扩展函数 (CollectionExtensions.kt)

#### 安全访问
```kotlin
// 安全获取元素
val item = list.getOrNull(5) // 如果索引越界返回 null
val item2 = list.getOrDefault(5, "默认值") // 如果索引越界返回默认值

// 安全获取首尾元素
val first = list.firstOrNull()
val last = list.lastOrNull()
```

#### 空值处理
```kotlin
// 检查是否不为空
val isNotEmpty = list.isNotNullOrEmpty()

// 返回空列表（如果为 null）
val safeList = list.orEmpty()
```

#### 列表操作
```kotlin
// 转换为带索引的 Pair 列表
val indexed = list.withIndexPairs() // List<Pair<Int, T>>

// 按指定大小分割
val chunks = list.chunked(3) // List<List<T>>

// 移除重复元素（保持顺序）
val distinct = list.distinct()
```

#### Map 操作
```kotlin
// 安全获取值
val value = map.getOrNull(key)
val value2 = map.getOrDefault(key, "默认值")

// 空值处理
val isNotEmpty = map.isNotNullOrEmpty()
val safeMap = map.orEmpty()
```

#### 列表转换
```kotlin
// 合并两个列表为 Pair 列表
val zipped = list1.zip(list2) // List<Pair<T, R>>

// 转换为 Map（使用索引作为键）
val map = list.toMapWithIndex() // Map<Int, T>

// 转换为 Map（使用指定函数生成键）
val map2 = list.toMap { it.id } // Map<K, T>
```

### 3. Context 扩展函数 (ContextExtensions.kt)

#### 资源访问
```kotlin
// 安全获取字符串资源
val text = context.getStringSafe(R.string.app_name)

// 获取颜色资源
val color = context.getColorCompat(R.color.primary)

// 获取尺寸资源（像素值）
val dimen = context.getDimenPx(R.dimen.margin_16)
```

#### 尺寸转换
```kotlin
// dp 转 px
val px = context.dpToPx(16f)

// px 转 dp
val dp = context.pxToDp(48f)

// sp 转 px
val px = context.spToPx(14f)

// px 转 sp
val sp = context.pxToSp(28f)
```

#### 屏幕信息
```kotlin
// 获取屏幕尺寸
val width = context.getScreenWidth()
val height = context.getScreenHeight()
val density = context.getScreenDensity()

// 获取状态栏和导航栏高度
val statusBarHeight = context.getStatusBarHeight()
val navBarHeight = context.getNavigationBarHeight()
```

#### 设备检测
```kotlin
// 检查设备类型
val isTablet = context.isTablet()
val isLandscape = context.isLandscape()
val isPortrait = context.isPortrait()
```

#### View 扩展
```kotlin
// View 中也可以使用尺寸转换
val px = view.dpToPx(16f)
val dp = view.pxToDp(48f)
val sp = view.spToPx(14f)
```

### 4. Number 扩展函数 (NumberExtensions.kt)

#### 格式化数字
```kotlin
// 添加千分位分隔符
val formatted = 1234567.formatWithCommas() // "1,234,567"

// 格式化小数（保留指定位数）
val formatted = 1234.567.formatWithCommas(2) // "1,234.57"
```

#### 格式化文件大小
```kotlin
// 格式化文件大小
val size = 1024L.formatFileSize() // "1.00 KB"
val size2 = 1048576L.formatFileSize() // "1.00 MB"
```

#### 格式化货币
```kotlin
// 格式化货币
val currency = 1234.56.formatCurrency() // "¥1,234.56"
val currency2 = 1234.56.formatCurrency("$", 2) // "$1,234.56"
```

#### 格式化百分比
```kotlin
// 格式化百分比
val percent = 50.formatPercent() // "50%"
val percent2 = 0.5.formatPercent(2) // "50.00%"
```

#### 范围限制
```kotlin
// 限制在指定范围内
val clamped = 150.coerceIn(0, 100) // 100

// 检查是否在范围内
val inRange = 50.isInRange(0, 100) // true
```

#### 单位转换
```kotlin
// 转换为带单位的字符串（K, M, B）
val withUnit = 1500.formatWithUnit() // "1.50K"
val withUnit2 = 1500000.formatWithUnit() // "1.50M"
```

### 5. File 工具类 (FileUtils.kt)

#### 文件操作
```kotlin
// 检查文件是否存在
val exists = FileUtils.exists("/path/to/file.txt")

// 创建目录
val created = FileUtils.createDirectory("/path/to/dir")

// 删除文件或目录
val deleted = FileUtils.delete("/path/to/file.txt")

// 获取文件大小
val size = FileUtils.getFileSize("/path/to/file.txt")
```

#### 文件信息
```kotlin
// 获取文件扩展名
val ext = FileUtils.getFileExtension("/path/to/file.txt") // "txt"

// 获取文件名（不含扩展名）
val name = FileUtils.getFileNameWithoutExtension("/path/to/file.txt") // "file"
```

#### 文件读写
```kotlin
// 读取文件内容
val content = FileUtils.readFileAsString("/path/to/file.txt")

// 写入文件内容
val written = FileUtils.writeStringToFile("/path/to/file.txt", "内容")

// 复制文件
val copied = FileUtils.copyFile("/path/to/source.txt", "/path/to/dest.txt")
```

#### 文件列表
```kotlin
// 获取目录下的所有文件
val files = FileUtils.listFiles("/path/to/dir", recursive = false)

// 递归获取所有文件
val allFiles = FileUtils.listFiles("/path/to/dir", recursive = true)
```

#### File 扩展函数
```kotlin
val file = File("/path/to/file.txt")

// 获取扩展名
val ext = file.getExtension() // "txt"

// 获取文件名（不含扩展名）
val name = file.getNameWithoutExtension() // "file"

// 格式化文件大小
val size = file.formatSize() // "1.00 KB"
```

## 📊 功能统计

### 新增文件（5 个）
1. ✅ `extension/StringExtensions.kt` - String 扩展函数（50+ 个方法）
2. ✅ `extension/CollectionExtensions.kt` - Collection 扩展函数（20+ 个方法）
3. ✅ `extension/ContextExtensions.kt` - Context 扩展函数（20+ 个方法）
4. ✅ `extension/NumberExtensions.kt` - Number 扩展函数（20+ 个方法）
5. ✅ `util/FileUtils.kt` - 文件工具类（15+ 个方法）

### 原有文件（12 个）
1. ✅ `result/AppResult.kt`
2. ✅ `error/AppError.kt`
3. ✅ `extension/CoroutineExtensions.kt`
4. ✅ `extension/ResultExtensions.kt`
5. ✅ `extension/ViewExtensions.kt`
6. ✅ `config/AppConfig.kt`
7. ✅ `network/NetworkMonitor.kt`
8. ✅ `paging/PagingState.kt`
9. ✅ `util/CacheManager.kt`
10. ✅ `util/DateUtils.kt`
11. ✅ `util/ResourceProvider.kt`
12. ✅ `util/Validator.kt`

### 总计
- **17 个文件**（从 12 个增加到 17 个）
- **扩展函数**：100+ 个
- **功能覆盖**：String、Collection、Context、Number、File、Date、Validator、Cache、Network、Paging

## 🎯 使用建议

1. **String 扩展**：使用 `isNullOrBlank()`、`orDefault()` 等简化空值处理
2. **Collection 扩展**：使用 `getOrNull()`、`getOrDefault()` 等安全访问元素
3. **Context 扩展**：使用 `dpToPx()`、`getScreenWidth()` 等简化尺寸转换和屏幕信息获取
4. **Number 扩展**：使用 `formatWithCommas()`、`formatFileSize()` 等格式化数字
5. **File 工具**：使用 `FileUtils` 简化文件操作

## ✨ 总结

core-common 模块现在提供了**完整的通用工具集**：
- ✅ 丰富的 String 扩展函数（验证、格式化、转换等）
- ✅ Collection 安全访问和转换
- ✅ Context 资源访问和尺寸转换
- ✅ Number 格式化和单位转换
- ✅ File 文件操作工具
- ✅ 原有的 Date、Validator、Cache、Network 等工具

**core-common 模块已扩展完成！** 🎉


