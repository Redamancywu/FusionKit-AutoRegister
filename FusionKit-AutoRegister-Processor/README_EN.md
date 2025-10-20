# FusionKit AutoRegister Processor

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/)
[![KSP](https://img.shields.io/badge/KSP-2.0.21--1.0.28-green.svg)](https://github.com/google/ksp)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[🇺🇸 English](README_EN.md) | [🇨🇳 中文](README_CN.md)

A Kotlin Symbol Processing (KSP) based annotation processor for automatic service registration in multi-module Android projects.

## 🎯 Overview

This processor automatically discovers classes annotated with `@AutoRegister` and generates provider classes that allow runtime access to service implementations without reflection overhead.

## 🏗️ Architecture

### Core Components

1. **AutoRegister Annotation**
   - Defines service registration metadata

2. **AutoRegisterSymbolProcessor**
   - KSP processor that scans for annotations

3. **Generated Providers**
   - Runtime access classes

### Processing Flow

```
1. Scan for @AutoRegister annotations
2. Parse annotation parameters
3. Group services by interface
4. Generate provider classes
5. Compile-time code generation
```

## 🔧 Implementation Details

### Annotation Processing

The processor supports multiple annotations per class and handles various parameter types:

```kotlin
@AutoRegister(
    value = [MyInterface::class],
    name = "customName",
    type = "businessType", 
    priority = 100,
    enabledIn = [BuildType.DEBUG],
    isObject = false
)
class MyServiceImpl : MyInterface
```

### Code Generation

The processor generates optimized Kotlin code using KotlinPoet:

```kotlin
// Generated code structure
object MyInterfaceProviders {
    val all: List<MyInterface> = listOf(...)
    val byName: Map<String, MyInterface> = mapOf(...)
    val byType: Map<String, List<MyInterface>> = mapOf(...)
    
    fun get(name: String): MyInterface? = byName[name]
    fun getBest(type: String? = null): MyInterface? = ...
    fun getAll(type: String? = null): List<MyInterface> = ...
}
```

## 🚀 Usage

### Integration

Add to your project's `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

dependencies {
    ksp(project(":FusionKit-AutoRegister-Processor"))
    implementation(project(":FusionKit-AutoRegister-Processor"))
}
```

### Service Registration

```kotlin
// Define interface
interface DataService {
    fun getData(): String
}

// Register implementation
@AutoRegister(
    value = [DataService::class],
    name = "primary",
    type = "data",
    priority = 100
)
class PrimaryDataService : DataService {
    override fun getData(): String = "Primary Data"
}

// Use generated provider
val service = DataServiceProviders.get("primary")
```

## 🔍 Advanced Features

### Multi-Interface Registration

```kotlin
@AutoRegister(
    value = [ServiceA::class, ServiceB::class],
    name = "multi",
    type = "utility",
    priority = 200
)
class MultiService : ServiceA, ServiceB {
    // Implementation for both interfaces
}
```

### Environment-Specific Services

```kotlin
// Debug implementation
@AutoRegister(
    value = [NetworkService::class],
    name = "mock",
    type = "network",
    priority = 100,
    enabledIn = [BuildType.DEBUG]
)
class MockNetworkService : NetworkService {
    // Mock implementation
}

// Production implementation
@AutoRegister(
    value = [NetworkService::class],
    name = "real",
    type = "network", 
    priority = 50,
    enabledIn = [BuildType.RELEASE]
)
class RealNetworkService : NetworkService {
    // Real implementation
}
```

## 🧪 Testing

The processor includes comprehensive test coverage:

```kotlin
@Test
fun testServiceDiscovery() {
    val allServices = TestServiceProviders.all
    assertEquals(3, allServices.size)
    
    val primaryService = TestServiceProviders.get("primary")
    assertNotNull(primaryService)
    
    val bestService = TestServiceProviders.getBest()
    assertEquals(200, bestService?.getPriority())
}
```

## 🔧 Configuration

### Build Configuration

```kotlin
ksp {
    arg("auto.register.env", "DEBUG") // Environment control
    arg("auto.register.debug", "true") // Debug mode
}
```

### Gradle Configuration

```kotlin
// In your module's build.gradle.kts
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
    }
}
```

## 📋 Requirements

- **Kotlin** 2.0.21+
- **KSP** 2.0.21-1.0.28+
- **KotlinPoet** 2.1.0+
- **Android Gradle Plugin** 8.12.3+

## 🐛 Troubleshooting

### Common Issues

1. **KSP not running**
   - Ensure KSP plugin is applied

2. **Generated code not found**
   - Check build/generated/ksp directory

3. **Import errors**
   - Manually add imports to generated files

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Redamancy** - *Initial work* - [GitHub](https://github.com/redamancy)

---

Built with ❤️ for the Android community
