# FusionKit AutoRegister - Usage Example

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/)
[![KSP](https://img.shields.io/badge/KSP-2.0.21--1.0.28-green.svg)](https://github.com/google/ksp)
[![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)](https://developer.android.com/)

[🇺🇸 English](README_EN.md) | [🇨🇳 中文](README_CN.md)

A comprehensive example demonstrating how to use the FusionKit AutoRegister framework in an Android application.

## 🎯 Overview

This example app showcases the complete workflow of using the AutoRegister framework, from service definition to runtime usage.

## 🏗️ Project Structure

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

## 🚀 Quick Start

### 1. Service Interface

Define your service interface:

```kotlin
interface TestService {
    fun getName(): String
    fun getPriority(): Int
}
```

### 2. Service Implementations

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

### 3. Generated Code

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

## 📱 Usage in MainActivity

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

## 🧪 Unit Testing

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

## 🎨 UI Layout

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

## 🚀 Running the Example

1. **Build the project**
   ```bash
   ./gradlew build
   ```

2. **Run unit tests**
   ```bash
   ./gradlew test
   ```

3. **Install and run the app**
   ```bash
   ./gradlew installDebug
   ```

4. **Check Logcat output**
   - Look for "AutoRegisterDemo" tag

## 📋 Expected Output

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

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Redamancy** - *Initial work* - [GitHub](https://github.com/redamancy)

---

Example app demonstrating the power of compile-time service registration
