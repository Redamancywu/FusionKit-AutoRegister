# FusionKit AutoRegister - Usage Example

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/)
[![KSP](https://img.shields.io/badge/KSP-2.0.21--1.0.28-green.svg)](https://github.com/google/ksp)
[![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)](https://developer.android.com/)

A comprehensive example demonstrating how to use the FusionKit AutoRegister framework in an Android application.

一个综合示例，演示如何在 Android 应用程序中使用 FusionKit AutoRegister 框架。

## 🎯 Overview / 概述

**English:**
This example app showcases the complete workflow of using the AutoRegister framework, from service definition to runtime usage.

**中文:**
此示例应用展示了使用 AutoRegister 框架的完整工作流程，从服务定义到运行时使用。

## 🏗️ Project Structure / 项目结构

```
app/
├── src/main/java/com/horizon/fusionkit/
│   ├── MainActivity.kt                    # Main activity demonstrating usage
│   └── test/                              # Test services
│       ├── TestService.kt                 # Service interface
│       ├── TestServiceImpl1.kt            # Primary implementation
│       ├── TestServiceImpl2.kt            # Secondary implementation
│       └── TestServiceObject.kt           # Object implementation
├── src/test/java/com/horizon/fusionkit/
│   └── AutoRegisterTest.kt               # Unit tests
└── build/generated/ksp/debug/kotlin/
    └── com/horizon/fusionkit/test/
        └── TestServiceProviders.kt        # Generated provider class
```

## 🚀 Quick Start / 快速开始

### 1. Service Interface / 服务接口

**English:**
Define your service interface:

```kotlin
interface TestService {
    fun getName(): String
    fun getPriority(): Int
}
```

**中文:**
定义你的服务接口：

```kotlin
interface TestService {
    fun getName(): String
    fun getPriority(): Int
}
```

### 2. Service Implementations / 服务实现

**English:**
Create multiple implementations with different priorities:

```kotlin
// High priority implementation
@AutoRegister(
    value = [TestService::class],
    name = "primary",
    type = "core",
    priority = 100,
    enabledIn = [BuildType.ALL]
)
class TestServiceImpl1 : TestService {
    override fun getName(): String = "PrimaryService"
    override fun getPriority(): Int = 100
}

// Medium priority implementation
@AutoRegister(
    value = [TestService::class],
    name = "secondary",
    type = "backup",
    priority = 50,
    enabledIn = [BuildType.ALL]
)
class TestServiceImpl2 : TestService {
    override fun getName(): String = "SecondaryService"
    override fun getPriority(): Int = 50
}

// Highest priority object implementation
@AutoRegister(
    value = [TestService::class],
    name = "object",
    type = "singleton",
    priority = 200,
    enabledIn = [BuildType.ALL],
    isObject = true
)
object TestServiceObject : TestService {
    override fun getName(): String = "ObjectService"
    override fun getPriority(): Int = 200
}
```

**中文:**
创建具有不同优先级的多个实现：

```kotlin
// 高优先级实现
@AutoRegister(
    value = [TestService::class],
    name = "primary",
    type = "core",
    priority = 100,
    enabledIn = [BuildType.ALL]
)
class TestServiceImpl1 : TestService {
    override fun getName(): String = "PrimaryService"
    override fun getPriority(): Int = 100
}

// 中等优先级实现
@AutoRegister(
    value = [TestService::class],
    name = "secondary",
    type = "backup",
    priority = 50,
    enabledIn = [BuildType.ALL]
)
class TestServiceImpl2 : TestService {
    override fun getName(): String = "SecondaryService"
    override fun getPriority(): Int = 50
}

// 最高优先级对象实现
@AutoRegister(
    value = [TestService::class],
    name = "object",
    type = "singleton",
    priority = 200,
    enabledIn = [BuildType.ALL],
    isObject = true
)
object TestServiceObject : TestService {
    override fun getName(): String = "ObjectService"
    override fun getPriority(): Int = 200
}
```

### 3. Generated Code / 生成的代码

**English:**
The framework automatically generates `TestServiceProviders.kt`:

```kotlin
public object TestServiceProviders {
    public val all: List<TestService> = listOf(
        TestServiceObject,        // Priority 200
        TestServiceImpl1(),       // Priority 100
        TestServiceImpl2()       // Priority 50
    )

    public val byName: Map<String, TestService> = mapOf(
        "object" to TestServiceObject,
        "primary" to TestServiceImpl1(),
        "secondary" to TestServiceImpl2()
    )

    public val byType: Map<String, List<TestService>> = mapOf(
        "singleton" to listOf(TestServiceObject),
        "core" to listOf(TestServiceImpl1()),
        "backup" to listOf(TestServiceImpl2())
    )

    public fun get(name: String): TestService? = byName[name]
    public fun getBest(type: String? = null): TestService? = 
        if (type != null) byType[type]?.firstOrNull() else all.firstOrNull()
    public fun getAll(type: String? = null): List<TestService> = 
        type?.let { byType[it] } ?: all
}
```

**中文:**
框架自动生成 `TestServiceProviders.kt`：

```kotlin
public object TestServiceProviders {
    public val all: List<TestService> = listOf(
        TestServiceObject,        // 优先级 200
        TestServiceImpl1(),       // 优先级 100
        TestServiceImpl2()       // 优先级 50
    )

    public val byName: Map<String, TestService> = mapOf(
        "object" to TestServiceObject,
        "primary" to TestServiceImpl1(),
        "secondary" to TestServiceImpl2()
    )

    public val byType: Map<String, List<TestService>> = mapOf(
        "singleton" to listOf(TestServiceObject),
        "core" to listOf(TestServiceImpl1()),
        "backup" to listOf(TestServiceImpl2())
    )

    public fun get(name: String): TestService? = byName[name]
    public fun getBest(type: String? = null): TestService? = 
        if (type != null) byType[type]?.firstOrNull() else all.firstOrNull()
    public fun getAll(type: String? = null): List<TestService> = 
        type?.let { byType[it] } ?: all
}
```

## 📱 Usage in MainActivity / 在 MainActivity 中的使用

**English:**
```kotlin
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "AutoRegisterDemo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Demonstrate AutoRegister framework usage
        demonstrateAutoRegister()
    }
    
    private fun demonstrateAutoRegister() {
        Log.d(TAG, "=== AutoRegister Framework Demo ===")
        
        // 1. Get all services
        val allServices = TestServiceProviders.all
        Log.d(TAG, "Total services: ${allServices.size}")
        allServices.forEach { service ->
            Log.d(TAG, "Service: ${service.getName()}, Priority: ${service.getPriority()}")
        }
        
        // 2. Get service by name
        val primaryService = TestServiceProviders.get("primary")
        Log.d(TAG, "Primary service: ${primaryService?.getName()}")
        
        val objectService = TestServiceProviders.get("object")
        Log.d(TAG, "Object service: ${objectService?.getName()}")
        
        // 3. Get services by type
        val coreServices = TestServiceProviders.getAll("core")
        Log.d(TAG, "Core services count: ${coreServices.size}")
        
        val singletonServices = TestServiceProviders.getAll("singleton")
        Log.d(TAG, "Singleton services count: ${singletonServices.size}")
        
        // 4. Get best service
        val bestService = TestServiceProviders.getBest()
        Log.d(TAG, "Best service: ${bestService?.getName()}, Priority: ${bestService?.getPriority()}")
        
        val bestCoreService = TestServiceProviders.getBest("core")
        Log.d(TAG, "Best core service: ${bestCoreService?.getName()}")
        
        Log.d(TAG, "=== AutoRegister Framework Demo Complete ===")
    }
}
```

**中文:**
```kotlin
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "AutoRegisterDemo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 演示 AutoRegister 框架使用
        demonstrateAutoRegister()
    }
    
    private fun demonstrateAutoRegister() {
        Log.d(TAG, "=== AutoRegister 框架演示 ===")
        
        // 1. 获取所有服务
        val allServices = TestServiceProviders.all
        Log.d(TAG, "总服务数: ${allServices.size}")
        allServices.forEach { service ->
            Log.d(TAG, "服务: ${service.getName()}, 优先级: ${service.getPriority()}")
        }
        
        // 2. 按名称获取服务
        val primaryService = TestServiceProviders.get("primary")
        Log.d(TAG, "主服务: ${primaryService?.getName()}")
        
        val objectService = TestServiceProviders.get("object")
        Log.d(TAG, "对象服务: ${objectService?.getName()}")
        
        // 3. 按类型获取服务
        val coreServices = TestServiceProviders.getAll("core")
        Log.d(TAG, "核心服务数量: ${coreServices.size}")
        
        val singletonServices = TestServiceProviders.getAll("singleton")
        Log.d(TAG, "单例服务数量: ${singletonServices.size}")
        
        // 4. 获取最佳服务
        val bestService = TestServiceProviders.getBest()
        Log.d(TAG, "最佳服务: ${bestService?.getName()}, 优先级: ${bestService?.getPriority()}")
        
        val bestCoreService = TestServiceProviders.getBest("core")
        Log.d(TAG, "最佳核心服务: ${bestCoreService?.getName()}")
        
        Log.d(TAG, "=== AutoRegister 框架演示完成 ===")
    }
}
```

## 🧪 Unit Testing / 单元测试

**English:**
Comprehensive unit tests demonstrate framework functionality:

```kotlin
class AutoRegisterTest {

    @Test
    fun testAllServicesRetrieved() {
        val allServices = TestServiceProviders.all
        assertNotNull("All services list should not be null", allServices)
        assertEquals("Should have 3 service implementations", 3, allServices.size)
        
        // Verify services are sorted by priority (highest to lowest)
        val priorities = allServices.map { it.getPriority() }
        assertEquals("Services should be sorted by priority", listOf(200, 100, 50), priorities)
    }

    @Test
    fun testGetServiceByName() {
        val primaryService = TestServiceProviders.get("primary")
        assertNotNull("Primary service should not be null", primaryService)
        assertEquals("Primary service name", "PrimaryService", primaryService?.getName())
        assertEquals("Primary service priority", 100, primaryService?.getPriority())

        val objectService = TestServiceProviders.get("object")
        assertNotNull("Object service should not be null", objectService)
        assertEquals("Object service name", "ObjectService", objectService?.getName())
        assertEquals("Object service priority", 200, objectService?.getPriority())
    }

    @Test
    fun testGetServiceByType() {
        val coreServices = TestServiceProviders.getAll("core")
        assertNotNull("Core services list should not be null", coreServices)
        assertEquals("Should have 1 core service", 1, coreServices.size)
        assertEquals("Core service name", "PrimaryService", coreServices.first().getName())

        val singletonServices = TestServiceProviders.getAll("singleton")
        assertNotNull("Singleton services list should not be null", singletonServices)
        assertEquals("Should have 1 singleton service", 1, singletonServices.size)
        assertEquals("Singleton service name", "ObjectService", singletonServices.first().getName())
    }

    @Test
    fun testGetBestService() {
        val bestService = TestServiceProviders.getBest()
        assertNotNull("Best service should not be null", bestService)
        assertEquals("Best service name", "ObjectService", bestService?.getName())
        assertEquals("Best service priority", 200, bestService?.getPriority())

        val bestCoreService = TestServiceProviders.getBest("core")
        assertNotNull("Best core service should not be null", bestCoreService)
        assertEquals("Best core service name", "PrimaryService", bestCoreService?.getName())
    }
}
```

**中文:**
全面的单元测试演示框架功能：

```kotlin
class AutoRegisterTest {

    @Test
    fun testAllServicesRetrieved() {
        val allServices = TestServiceProviders.all
        assertNotNull("所有服务列表不应为空", allServices)
        assertEquals("应该有3个服务实现", 3, allServices.size)
        
        // 验证服务按优先级排序（从高到低）
        val priorities = allServices.map { it.getPriority() }
        assertEquals("服务应按优先级排序", listOf(200, 100, 50), priorities)
    }

    @Test
    fun testGetServiceByName() {
        val primaryService = TestServiceProviders.get("primary")
        assertNotNull("主服务不应为空", primaryService)
        assertEquals("主服务名称", "PrimaryService", primaryService?.getName())
        assertEquals("主服务优先级", 100, primaryService?.getPriority())

        val objectService = TestServiceProviders.get("object")
        assertNotNull("对象服务不应为空", objectService)
        assertEquals("对象服务名称", "ObjectService", objectService?.getName())
        assertEquals("对象服务优先级", 200, objectService?.getPriority())
    }

    @Test
    fun testGetServiceByType() {
        val coreServices = TestServiceProviders.getAll("core")
        assertNotNull("核心服务列表不应为空", coreServices)
        assertEquals("应该有1个核心服务", 1, coreServices.size)
        assertEquals("核心服务名称", "PrimaryService", coreServices.first().getName())

        val singletonServices = TestServiceProviders.getAll("singleton")
        assertNotNull("单例服务列表不应为空", singletonServices)
        assertEquals("应该有1个单例服务", 1, singletonServices.size)
        assertEquals("单例服务名称", "ObjectService", singletonServices.first().getName())
    }

    @Test
    fun testGetBestService() {
        val bestService = TestServiceProviders.getBest()
        assertNotNull("最佳服务不应为空", bestService)
        assertEquals("最佳服务名称", "ObjectService", bestService?.getName())
        assertEquals("最佳服务优先级", 200, bestService?.getPriority())

        val bestCoreService = TestServiceProviders.getBest("core")
        assertNotNull("最佳核心服务不应为空", bestCoreService)
        assertEquals("最佳核心服务名称", "PrimaryService", bestCoreService?.getName())
    }
}
```

## 🎨 UI Layout / UI 布局

**English:**
Simple layout for demonstration:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="AutoRegister Framework Test"
        android:textSize="18sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp" />

    <TextView
        android:id="@+id/tvResult"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Check Logcat output for framework results"
        android:textSize="14sp" />

</LinearLayout>
```

**中文:**
用于演示的简单布局：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="AutoRegister 框架测试"
        android:textSize="18sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp" />

    <TextView
        android:id="@+id/tvResult"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="查看 Logcat 输出查看框架结果"
        android:textSize="14sp" />

</LinearLayout>
```

## 🚀 Running the Example / 运行示例

**English:**

1. **Build the project** / **构建项目**
   ```bash
   ./gradlew build
   ```

2. **Run unit tests** / **运行单元测试**
   ```bash
   ./gradlew test
   ```

3. **Install and run the app** / **安装并运行应用**
   ```bash
   ./gradlew installDebug
   ```

4. **Check Logcat output** / **检查 Logcat 输出**
   - Look for "AutoRegisterDemo" tag
   - 查找 "AutoRegisterDemo" 标签

**中文:**

1. **构建项目**
   ```bash
   ./gradlew build
   ```

2. **运行单元测试**
   ```bash
   ./gradlew test
   ```

3. **安装并运行应用**
   ```bash
   ./gradlew installDebug
   ```

4. **检查 Logcat 输出**
   - 查找 "AutoRegisterDemo" 标签

## 📋 Expected Output / 预期输出

**English:**
When running the app, you should see the following in Logcat:

```
D/AutoRegisterDemo: === AutoRegister Framework Demo ===
D/AutoRegisterDemo: Total services: 3
D/AutoRegisterDemo: Service: ObjectService, Priority: 200
D/AutoRegisterDemo: Service: PrimaryService, Priority: 100
D/AutoRegisterDemo: Service: SecondaryService, Priority: 50
D/AutoRegisterDemo: Primary service: PrimaryService
D/AutoRegisterDemo: Object service: ObjectService
D/AutoRegisterDemo: Core services count: 1
D/AutoRegisterDemo: Singleton services count: 1
D/AutoRegisterDemo: Best service: ObjectService, Priority: 200
D/AutoRegisterDemo: Best core service: PrimaryService
D/AutoRegisterDemo: === AutoRegister Framework Demo Complete ===
```

**中文:**
运行应用时，你应该在 Logcat 中看到以下内容：

```
D/AutoRegisterDemo: === AutoRegister 框架演示 ===
D/AutoRegisterDemo: 总服务数: 3
D/AutoRegisterDemo: 服务: ObjectService, 优先级: 200
D/AutoRegisterDemo: 服务: PrimaryService, 优先级: 100
D/AutoRegisterDemo: 服务: SecondaryService, 优先级: 50
D/AutoRegisterDemo: 主服务: PrimaryService
D/AutoRegisterDemo: 对象服务: ObjectService
D/AutoRegisterDemo: 核心服务数量: 1
D/AutoRegisterDemo: 单例服务数量: 1
D/AutoRegisterDemo: 最佳服务: ObjectService, 优先级: 200
D/AutoRegisterDemo: 最佳核心服务: PrimaryService
D/AutoRegisterDemo: === AutoRegister 框架演示完成 ===
```

## 🤝 Contributing / 贡献

**English:**
Contributions are welcome! Please feel free to submit a Pull Request.

**中文:**
欢迎贡献！请随时提交 Pull Request。

## 📄 License / 许可证

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

本项目基于 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 👨‍💻 Author / 作者

**Redamancy** - *Initial work* - [GitHub](https://github.com/redamancy)

---

**English:** Example app demonstrating the power of compile-time service registration

**中文:** 演示编译时服务注册功能的示例应用
