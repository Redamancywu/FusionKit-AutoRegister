# FusionKit AutoRegister 处理器

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/)
[![KSP](https://img.shields.io/badge/KSP-2.0.21--1.0.28-green.svg)](https://github.com/google/ksp)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[🇺🇸 English](README_EN.md) | [🇨🇳 中文](README_CN.md)

一个基于 Kotlin 符号处理 (KSP) 的注解处理器，用于多模块 Android 项目中的自动服务注册。

## 🎯 概述

此处理器自动发现使用 `@AutoRegister` 注解的类，并生成提供者类，允许在运行时访问服务实现，而无需反射开销。

## 🏗️ 架构

### 核心组件

1. **AutoRegister 注解**
   - 定义服务注册元数据

2. **AutoRegisterSymbolProcessor**
   - 扫描注解的 KSP 处理器

3. **生成的提供者**
   - 运行时访问类

### 处理流程

```
1. 扫描 @AutoRegister 注解
2. 解析注解参数
3. 按接口分组服务
4. 生成提供者类
5. 编译时代码生成
```

## 🔧 实现细节

### 注解处理

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

### 代码生成

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

## 🚀 用法

### 集成

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

### 服务注册

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

## 🔍 高级功能

### 多接口注册

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

### 环境特定服务

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

## 🧪 测试

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

## 🔧 配置

### 构建配置

```kotlin
ksp {
    arg("auto.register.env", "DEBUG") // 环境控制
    arg("auto.register.debug", "true") // 调试模式
}
```

### Gradle 配置

```kotlin
// 在你的模块 build.gradle.kts 中
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
    }
}
```

## 📋 要求

- **Kotlin** 2.0.21+
- **KSP** 2.0.21-1.0.28+
- **KotlinPoet** 2.1.0+
- **Android Gradle Plugin** 8.12.3+

## 🐛 故障排除

### 常见问题

1. **KSP 未运行**
   - 确保 KSP 插件已应用

2. **找不到生成的代码**
   - 检查 build/generated/ksp 目录

3. **导入错误**
   - 手动添加导入到生成的文件

## 🤝 贡献

欢迎贡献！请随时提交 Pull Request。

## 📄 许可证

本项目基于 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 👨‍💻 作者

**Redamancy** - *初始工作* - [GitHub](https://github.com/redamancy)

---

为 Android 社区用 ❤️ 构建
