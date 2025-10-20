# 🚀 JitPack 使用说明

## 📦 如何添加依赖

### 1. 添加 JitPack 仓库

在项目的 `settings.gradle.kts` 中添加：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. 添加依赖

在 `build.gradle.kts` 中添加：

```kotlin
dependencies {
    // KSP 处理器
    ksp("com.github.Redamancywu:FusionKit-AutoRegister:v1.0.0")
    
    // 注解依赖
    implementation("com.github.Redamancywu:FusionKit-AutoRegister:v1.0.0")
}
```

### 3. 配置 KSP

确保在 `build.gradle.kts` 中配置了 KSP 插件：

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}
```

## 🎯 使用示例

```kotlin
// 1. 定义服务接口
interface UserService {
    fun getUser(id: String): User
}

// 2. 实现服务
@AutoRegister(
    interfaceClass = UserService::class,
    type = "user",
    priority = 1,
    buildType = BuildType.ALL
)
class UserServiceImpl : UserService {
    override fun getUser(id: String): User {
        // 实现逻辑
        return User(id)
    }
}

// 3. 使用服务
val userService = ServiceRegistry.getService<UserService>("user")
val user = userService.getUser("123")
```

## 📋 版本说明

- **v1.0.0**: 初始版本，支持基本的自动注册功能
- **最新版本**: 使用 `v1.0.0` 或 `main-SNAPSHOT` 获取最新代码

## 🔗 相关链接

- **GitHub 仓库**: https://github.com/Redamancywu/FusionKit-AutoRegister
- **JitPack 构建**: https://jitpack.io/#Redamancywu/FusionKit-AutoRegister
- **文档**: 查看 README.md 获取详细使用说明
