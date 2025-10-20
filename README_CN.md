# FusionKit AutoRegister

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/)
[![KSP](https://img.shields.io/badge/KSP-2.0.21--1.0.28-green.svg)](https://github.com/google/ksp)
[![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一个基于 Kotlin 符号处理 (KSP) 的强大自动注册框架，专为多模块 Android 项目设计。在编译时自动发现和注册服务实现。

## 📁 项目结构

```
FusionKit/
├── FusionKit-AutoRegister-Processor/     # KSP 注解处理器
│   ├── src/main/java/.../processor/      # 核心处理器实现
│   ├── src/main/resources/META-INF/       # KSP 服务注册
│   └── README.md                          # 处理器文档
├── app/                                   # 示例 Android 应用
│   ├── src/main/java/.../test/           # 测试服务实现
│   ├── src/test/java/.../                # 单元测试
│   └── README.md                          # 使用示例
├── README.md                              # 此文件
└── LICENSE                                # MIT 许可证
```

## 🎯 框架功能

- **编译时服务发现** - 自动查找带注解的类
- **零运行时开销** - 无反射，纯编译时代码生成
- **多接口支持** - 为一个实现注册多个接口
- **基于优先级的选择** - 自动选择最佳服务
- **环境控制** - 为 DEBUG/RELEASE 提供不同实现
- **基于类型的分组** - 按业务类型组织服务

## ✨ 特性

- 🚀 **编译时代码生成** - 零运行时开销
- 🎯 **多接口支持** - 为一个实现注册多个接口
- 📊 **基于优先级的选择** - 自动最佳服务选择
- 🏷️ **基于类型的分组** - 按业务类型组织服务
- 🌍 **环境控制** - 为 DEBUG/RELEASE 提供不同实现
- 🔄 **对象和类支持** - 支持 Kotlin 对象和类
- 📱 **Android 优化** - 专为 Android 项目设计

## 🚀 快速开始

### 1. 配置 KSP

在应用模块的 `build.gradle.kts` 中：

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

### 2. 定义服务接口

创建你的服务接口：

```kotlin
interface UserService {
    fun getUserInfo(): String
    fun getUserId(): Int
}
```

### 3. 实现并注册

实现你的服务并注册它：

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

### 4. 使用生成的服务

框架会自动生成 `UserServiceProviders` 类：

```kotlin
// 获取所有实现
val allServices = UserServiceProviders.all

// 按名称获取
val primaryService = UserServiceProviders.get("primary")

// 获取最佳服务（最高优先级）
val bestService = UserServiceProviders.getBest()

// 按类型获取
val coreServices = UserServiceProviders.getAll("core")
```

## 📖 详细用法

### 注解参数

`@AutoRegister` 注解支持以下参数：

| 参数 | 类型 | 必需 | 描述 |
|------|------|------|------|
| `value` | `Array<KClass<*>>` | ✅ | 要注册的接口列表 |
| `name` | `String` | ❌ | 唯一标识符（默认：类简单名） |
| `type` | `String` | ❌ | 业务分组（默认：接口全限定名） |
| `priority` | `Int` | ❌ | 优先级（越高越好，默认：0） |
| `enabledIn` | `Array<BuildType>` | ❌ | 环境控制（默认：ALL） |
| `isObject` | `Boolean` | ❌ | 是否为 Kotlin object（默认：false） |

### 高级示例

#### 多接口注册

```kotlin
@AutoRegister(
    value = [UserService::class, AuthService::class],
    name = "comprehensive",
    type = "auth",
    priority = 200
)
class ComprehensiveAuthService : UserService, AuthService {
    // 实现
}
```

#### 环境特定实现

```kotlin
@AutoRegister(
    value = [DatabaseService::class],
    name = "debug",
    type = "database",
    priority = 100,
    enabledIn = [BuildType.DEBUG]
)
class MockDatabaseService : DatabaseService {
    // 用于调试的模拟实现
}

@AutoRegister(
    value = [DatabaseService::class],
    name = "production",
    type = "database",
    priority = 50,
    enabledIn = [BuildType.RELEASE]
)
class RealDatabaseService : DatabaseService {
    // 用于生产的真实实现
}
```

#### Kotlin 对象注册

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
        // 实现
    }
}
```

## 🏗️ 生成的代码

框架为每个接口生成一个 `{InterfaceName}Providers` 类：

```kotlin
public object UserServiceProviders {
    // 按优先级排序的所有实现
    public val all: List<UserService> = listOf(
        HighPriorityService(),
        MediumPriorityService(),
        LowPriorityService()
    )

    // 按名称映射
    public val byName: Map<String, UserService> = mapOf(
        "primary" to PrimaryService(),
        "secondary" to SecondaryService()
    )

    // 按类型映射
    public val byType: Map<String, List<UserService>> = mapOf(
        "core" to listOf(CoreService()),
        "auth" to listOf(AuthService())
    )

    // 按名称获取
    public fun get(name: String): UserService? = byName[name]

    // 获取最佳服务（最高优先级）
    public fun getBest(type: String? = null): UserService? = 
        if (type != null) byType[type]?.firstOrNull() else all.firstOrNull()

    // 获取所有服务
    public fun getAll(type: String? = null): List<UserService> = 
        type?.let { byType[it] } ?: all
}
```

## 🔧 配置

### 环境控制

控制不同构建类型中启用哪些实现：

```kotlin
// 在你的 build.gradle.kts 中
ksp {
    arg("auto.register.env", "DEBUG") // 或 "RELEASE"
}
```

### 构建类型

- `BuildType.DEBUG` - 仅在调试构建中启用
- `BuildType.RELEASE` - 仅在发布构建中启用
- `BuildType.ALL` - 在所有构建中启用（默认）

## 🧪 测试

框架提供全面的测试支持：

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

## 🆚 与 AutoService 的对比

| 功能 | AutoService | FusionKit-AutoRegister |
|------|-------------|------------------------|
| **性能** | 运行时反射 | 编译时生成 |
| **配置** | 基础 | 丰富（优先级、类型、环境） |
| **查询方法** | 单一 | 多种（按名称、类型、优先级） |
| **环境控制** | ❌ | ✅ |
| **优先级支持** | ❌ | ✅ |
| **类型分组** | ❌ | ✅ |
| **学习曲线** | 低 | 中等 |
| **使用场景** | 简单项目 | 复杂多模块项目 |

## 📋 要求

- **Kotlin** 2.0.21+
- **KSP** 2.0.21-1.0.28+
- **Android Gradle Plugin** 8.12.3+
- **Gradle** 8.13+

## 🤝 贡献

欢迎贡献！请随时提交 Pull Request。

## 📄 许可证

本项目基于 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 👨‍💻 作者

**Redamancy** - *初始工作* - [GitHub](https://github.com/redamancy)

---

为 Android 社区用 ❤️ 制作
