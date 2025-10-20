# FusionKit AutoRegister

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/)
[![KSP](https://img.shields.io/badge/KSP-2.0.21--1.0.28-green.svg)](https://github.com/google/ksp)
[![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A powerful Kotlin Symbol Processing (KSP) based auto-registration framework for multi-module Android projects. Automatically discover and register service implementations at compile time.

## 📁 Project Structure

```
FusionKit/
├── FusionKit-AutoRegister-Processor/     # KSP Annotation Processor
│   ├── src/main/java/.../processor/      # Core processor implementation
│   ├── src/main/resources/META-INF/       # KSP service registration
│   └── README.md                          # Processor documentation
├── app/                                   # Example Android Application
│   ├── src/main/java/.../test/           # Test service implementations
│   ├── src/test/java/.../                # Unit tests
│   └── README.md                          # Usage examples
├── README.md                              # This file
└── LICENSE                                # MIT License
```

## 🎯 What This Framework Does

- **Compile-time Service Discovery** - Automatically finds annotated classes
- **Zero Runtime Overhead** - No reflection, pure compile-time code generation
- **Multi-interface Support** - Register one implementation for multiple interfaces
- **Priority-based Selection** - Automatic best service selection
- **Environment Control** - Different implementations for DEBUG/RELEASE
- **Type-based Grouping** - Organize services by business type

## ✨ Features

- 🚀 **Compile-time Code Generation** - Zero runtime overhead
- 🎯 **Multi-interface Support** - Register one implementation for multiple interfaces
- 📊 **Priority-based Selection** - Automatic best service selection
- 🏷️ **Type-based Grouping** - Organize services by business type
- 🌍 **Environment Control** - Different implementations for DEBUG/RELEASE
- 🔄 **Object & Class Support** - Support both Kotlin objects and classes
- 📱 **Android Optimized** - Designed specifically for Android projects

## 🚀 Quick Start

### 1. Configure KSP

In your app module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Redamancywu/FusionKit-AutoRegister")
    }
}

dependencies {
    ksp("com.redamancy.fusionkit:autoregister-processor:1.0.0")
    implementation("com.redamancy.fusionkit:autoregister-processor:1.0.0")
}
```

### 2. Define Service Interface

Create your service interface:

```kotlin
interface UserService {
    fun getUserInfo(): String
    fun getUserId(): Int
}
```

### 3. Implement and Register

Implement your service and register it:

```kotlin
@AutoRegister(
    value = [UserService::class],
    name = "primary",
    type = "core",
    priority = 100,
    enabledIn = [BuildType.ALL]
)
class UserServiceImpl : UserService {
    override fun getUserInfo(): String = "John Doe"
    override fun getUserId(): Int = 123
}
```

### 4. Use Generated Services

The framework automatically generates a `UserServiceProviders` class:

```kotlin
// Get all implementations
val allServices = UserServiceProviders.all

// Get by name
val primaryService = UserServiceProviders.get("primary")

// Get best service (highest priority)
val bestService = UserServiceProviders.getBest()

// Get by type
val coreServices = UserServiceProviders.getAll("core")
```

## 📖 Detailed Usage

### Annotation Parameters

The `@AutoRegister` annotation supports the following parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `value` | `Array<KClass<*>>` | ✅ | Interfaces to register for |
| `name` | `String` | ❌ | Unique identifier (default: class simple name) |
| `type` | `String` | ❌ | Business grouping (default: interface full name) |
| `priority` | `Int` | ❌ | Priority level (higher = better, default: 0) |
| `enabledIn` | `Array<BuildType>` | ❌ | Environment control (default: ALL) |
| `isObject` | `Boolean` | ❌ | Whether it's a Kotlin object (default: false) |

### Advanced Examples

#### Multiple Interface Registration

```kotlin
@AutoRegister(
    value = [UserService::class, AuthService::class],
    name = "comprehensive",
    type = "auth",
    priority = 200
)
class ComprehensiveAuthService : UserService, AuthService {
    // Implementation
}
```

#### Environment-specific Implementation

```kotlin
@AutoRegister(
    value = [DatabaseService::class],
    name = "debug",
    type = "database",
    priority = 100,
    enabledIn = [BuildType.DEBUG]
)
class MockDatabaseService : DatabaseService {
    // Mock implementation for debugging
}

@AutoRegister(
    value = [DatabaseService::class],
    name = "production",
    type = "database",
    priority = 50,
    enabledIn = [BuildType.RELEASE]
)
class RealDatabaseService : DatabaseService {
    // Real implementation for production
}
```

#### Kotlin Object Registration

```kotlin
@AutoRegister(
    value = [SingletonService::class],
    name = "singleton",
    type = "utility",
    priority = 300,
    isObject = true
)
object SingletonServiceImpl : SingletonService {
    override fun doSomething() {
        // Implementation
    }
}
```

## 🏗️ Generated Code

The framework generates a `{InterfaceName}Providers` class for each interface:

```kotlin
public object UserServiceProviders {
    // All implementations sorted by priority
    public val all: List<UserService> = listOf(
        HighPriorityService(),
        MediumPriorityService(),
        LowPriorityService()
    )

    // Map by name
    public val byName: Map<String, UserService> = mapOf(
        "primary" to PrimaryService(),
        "secondary" to SecondaryService()
    )

    // Map by type
    public val byType: Map<String, List<UserService>> = mapOf(
        "core" to listOf(CoreService()),
        "auth" to listOf(AuthService())
    )

    // Get by name
    public fun get(name: String): UserService? = byName[name]

    // Get best service (highest priority)
    public fun getBest(type: String? = null): UserService? = 
        if (type != null) byType[type]?.firstOrNull() else all.firstOrNull()

    // Get all services
    public fun getAll(type: String? = null): List<UserService> = 
        type?.let { byType[it] } ?: all
}
```

## 🔧 Configuration

### Environment Control

Control which implementations are enabled in different build types:

```kotlin
// In your build.gradle.kts
ksp {
    arg("auto.register.env", "DEBUG") // or "RELEASE"
}
```

### Build Types

- `BuildType.DEBUG` - Only enabled in debug builds
- `BuildType.RELEASE` - Only enabled in release builds  
- `BuildType.ALL` - Enabled in all builds (default)

## 🧪 Testing

The framework provides comprehensive testing support:

```kotlin
@Test
fun testServiceRegistration() {
    val allServices = UserServiceProviders.all
    assertTrue(allServices.isNotEmpty())
    
    val primaryService = UserServiceProviders.get("primary")
    assertNotNull(primaryService)
    
    val bestService = UserServiceProviders.getBest()
    assertEquals(300, bestService?.getPriority())
}
```

## 🆚 Comparison with AutoService

| Feature | AutoService | FusionKit-AutoRegister |
|---------|-------------|------------------------|
| **Performance** | Runtime reflection | Compile-time generation |
| **Configuration** | Basic | Rich (priority, type, environment) |
| **Query Methods** | Single | Multiple (by name, type, priority) |
| **Environment Control** | ❌ | ✅ |
| **Priority Support** | ❌ | ✅ |
| **Type Grouping** | ❌ | ✅ |
| **Learning Curve** | Low | Medium |
| **Use Case** | Simple projects | Complex multi-module projects |

## 📋 Requirements

- **Kotlin** 2.0.21+
- **KSP** 2.0.21-1.0.28+
- **Android Gradle Plugin** 8.12.3+
- **Gradle** 8.13+

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Redamancy** - *Initial work* - [GitHub](https://github.com/redamancy)

---

Made with ❤️ for the Android community
