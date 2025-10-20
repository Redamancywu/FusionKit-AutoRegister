# FusionKit AutoRegister Processor

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/)
[![KSP](https://img.shields.io/badge/KSP-2.0.21--1.0.28-green.svg)](https://github.com/google/ksp)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A Kotlin Symbol Processing (KSP) based annotation processor for automatic service registration in multi-module Android projects.

一个基于 Kotlin 符号处理 (KSP) 的注解处理器，用于多模块 Android 项目中的自动服务注册。

## 🎯 Overview / 概述

**English:**
This processor automatically discovers classes annotated with `@AutoRegister` and generates provider classes that allow runtime access to service implementations without reflection overhead.

**中文:**
此处理器自动发现使用 `@AutoRegister` 注解的类，并生成提供者类，允许在运行时访问服务实现，而无需反射开销。

## 🏗️ Architecture / 架构

### Core Components / 核心组件

**English:**

1. **AutoRegister Annotation** / **AutoRegister 注解**
   - Defines service registration metadata
   - 定义服务注册元数据

2. **AutoRegisterSymbolProcessor** / **AutoRegisterSymbolProcessor**
   - KSP processor that scans for annotations
   - 扫描注解的 KSP 处理器

3. **Generated Providers** / **生成的提供者**
   - Runtime access classes
   - 运行时访问类

**中文:**

1. **AutoRegister 注解**
   - 定义服务注册元数据

2. **AutoRegisterSymbolProcessor**
   - 扫描注解的 KSP 处理器

3. **生成的提供者**
   - 运行时访问类

### Processing Flow / 处理流程

```
1. Scan for @AutoRegister annotations
   扫描 @AutoRegister 注解
   
2. Parse annotation parameters
   解析注解参数
   
3. Group services by interface
   按接口分组服务
   
4. Generate provider classes
   生成提供者类
   
5. Compile-time code generation
   编译时代码生成
```

## 🔧 Implementation Details / 实现细节

### Annotation Processing / 注解处理

**English:**
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

**中文:**
处理器支持每个类的多个注解并处理各种参数类型：

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

### Code Generation / 代码生成

**English:**
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

**中文:**
处理器使用 KotlinPoet 生成优化的 Kotlin 代码：

```kotlin
// 生成的代码结构
object MyInterfaceProviders {
    val all: List<MyInterface> = listOf(...)
    val byName: Map<String, MyInterface> = mapOf(...)
    val byType: Map<String, List<MyInterface>> = mapOf(...)
    
    fun get(name: String): MyInterface? = byName[name]
    fun getBest(type: String? = null): MyInterface? = ...
    fun getAll(type: String? = null): List<MyInterface> = ...
}
```

## 🚀 Usage / 用法

### Integration / 集成

**English:**
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

**中文:**
添加到你的项目 `build.gradle.kts` 中：

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

dependencies {
    ksp(project(":FusionKit-AutoRegister-Processor"))
    implementation(project(":FusionKit-AutoRegister-Processor"))
}
```

### Service Registration / 服务注册

**English:**
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

**中文:**
```kotlin
// 定义接口
interface DataService {
    fun getData(): String
}

// 注册实现
@AutoRegister(
    value = [DataService::class],
    name = "primary",
    type = "data",
    priority = 100
)
class PrimaryDataService : DataService {
    override fun getData(): String = "Primary Data"
}

// 使用生成的提供者
val service = DataServiceProviders.get("primary")
```

## 🔍 Advanced Features / 高级功能

### Multi-Interface Registration / 多接口注册

**English:**
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

**中文:**
```kotlin
@AutoRegister(
    value = [ServiceA::class, ServiceB::class],
    name = "multi",
    type = "utility",
    priority = 200
)
class MultiService : ServiceA, ServiceB {
    // 两个接口的实现
}
```

### Environment-Specific Services / 环境特定服务

**English:**
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

**中文:**
```kotlin
// 调试实现
@AutoRegister(
    value = [NetworkService::class],
    name = "mock",
    type = "network",
    priority = 100,
    enabledIn = [BuildType.DEBUG]
)
class MockNetworkService : NetworkService {
    // 模拟实现
}

// 生产实现
@AutoRegister(
    value = [NetworkService::class],
    name = "real",
    type = "network", 
    priority = 50,
    enabledIn = [BuildType.RELEASE]
)
class RealNetworkService : NetworkService {
    // 真实实现
}
```

## 🧪 Testing / 测试

**English:**
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

**中文:**
处理器包含全面的测试覆盖：

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

## 🔧 Configuration / 配置

### Build Configuration / 构建配置

**English:**
```kotlin
ksp {
    arg("auto.register.env", "DEBUG") // Environment control
    arg("auto.register.debug", "true") // Debug mode
}
```

**中文:**
```kotlin
ksp {
    arg("auto.register.env", "DEBUG") // 环境控制
    arg("auto.register.debug", "true") // 调试模式
}
```

### Gradle Configuration / Gradle 配置

**English:**
```kotlin
// In your module's build.gradle.kts
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
    }
}
```

**中文:**
```kotlin
// 在你的模块 build.gradle.kts 中
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
    }
}
```

## 📋 Requirements / 要求

- **Kotlin** 2.0.21+
- **KSP** 2.0.21-1.0.28+
- **KotlinPoet** 2.1.0+
- **Android Gradle Plugin** 8.12.3+

## 🐛 Troubleshooting / 故障排除

### Common Issues / 常见问题

**English:**

1. **KSP not running** / **KSP 未运行**
   - Ensure KSP plugin is applied
   - 确保 KSP 插件已应用

2. **Generated code not found** / **找不到生成的代码**
   - Check build/generated/ksp directory
   - 检查 build/generated/ksp 目录

3. **Import errors** / **导入错误**
   - Manually add imports to generated files
   - 手动添加导入到生成的文件

**中文:**

1. **KSP 未运行**
   - 确保 KSP 插件已应用

2. **找不到生成的代码**
   - 检查 build/generated/ksp 目录

3. **导入错误**
   - 手动添加导入到生成的文件

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

**English:** Built with ❤️ for the Android community

**中文:** 为 Android 社区用 ❤️ 构建
