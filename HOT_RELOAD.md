# FusionKit 热更新使用指南

本指南详细介绍 FusionKit 的热更新（Hot Reload）功能：如何启用、如何在代码中使用注解、如何在运行时控制与监控、以及常见问题与最佳实践。

## 功能概览

- 文件系统监听：基于 NIO `WatchService` 监听源码或资源文件变更。
- 动态类重载：运行时替换服务实现，无需重启应用。
- 依赖顺序管理：按照声明的依赖顺序进行重载，保障一致性。
- 回滚机制：重载失败自动回滚到旧版本，支持重试策略与失败钩子。
- 事件通知与监控：重载开始/完成事件回调与统计指标。

## 前置条件

- `Kotlin` >= `2.0.21`
- `KSP` 插件 `2.0.21-1.0.28`
- 依赖：`com.redamancy.fusionkit:autoregister-processor:1.0.3-beta`

## 安装与启用

在应用模块 `build.gradle.kts`：

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

dependencies {
    // KSP 注解处理器
    ksp("com.redamancy.fusionkit:autoregister-processor:1.0.3-beta")
    implementation("com.redamancy.fusionkit:autoregister-processor:1.0.3-beta")
}

// 可选：通过 KSP 参数启用热更新（建议仅在 Debug）
ksp {
    arg("fusionkit.hotreload.enabled", "true")
    arg("fusionkit.hotreload.disableInProduction", "true")
}
```

也可通过以下方式启用或配置（优先级由低到高）：

- 默认配置文件：`fusionkit-hotreload.properties`
- 用户配置文件：`fusionkit-hotreload-user.properties`
- JVM 系统属性：`-Dfusionkit.hotreload.enabled=true` 等
- 环境变量：`FUSIONKIT_HOTRELOAD_ENABLED=true` 等

支持的配置键（`HotReloadConfigManager.Keys`）：

- `fusionkit.hotreload.enabled` 是否启用热更新，默认 `false`
- `fusionkit.hotreload.watchInterval` 监听间隔毫秒，默认 `1000`
- `fusionkit.hotreload.enableGlobalRollback` 全局回滚，默认 `true`
- `fusionkit.hotreload.globalReloadTimeout` 全局重载超时，默认 `10000`
- `fusionkit.hotreload.maxConcurrentReloads` 最大并发重载数，默认 `5`
- `fusionkit.hotreload.enableEventNotification` 事件通知，默认 `true`
- `fusionkit.hotreload.reloadHistorySize` 历史记录数量，默认 `10`
- `fusionkit.hotreload.disableInProduction` 生产环境禁用，默认 `true`
- `fusionkit.hotreload.enableLogging` 启用日志，默认 `true`

## 注解用法

### `@HotReload`

用于标记可热更新的服务实现类：

```kotlin
@HotReload(
    value = [UserService::class],
    name = "user-service",
    watchPaths = ["src/main/kotlin/com/example/UserServiceImpl.kt"],
    reloadStrategy = ReloadStrategy.IMMEDIATE,
    enableRollback = true,
    maxRetries = 3,
    reloadTimeout = 5000L,
    enabledIn = [BuildType.DEBUG],
    dependencies = [],
    beforeReload = "onBeforeReload",       // fun onBeforeReload(): Boolean
    afterReload = "onAfterReload",         // fun onAfterReload(success: Boolean, oldInstance: Any?, newInstance: Any?)
    onReloadFailed = "onReloadFailed",     // fun onReloadFailed(error: Throwable, retryCount: Int): Boolean
    enableMetrics = false,
    enableLogging = true
)
class UserServiceImpl : UserService {
    fun onBeforeReload(): Boolean = true
    fun onAfterReload(success: Boolean, oldInstance: Any?, newInstance: Any?) { /* ... */ }
    fun onReloadFailed(error: Throwable, retryCount: Int): Boolean = retryCount < 2
}
```

重载策略（`ReloadStrategy`）：

- `IMMEDIATE` 检测到文件变化后立即重载
- `DELAYED` 延迟一定间隔再重载，避免频繁触发
- `MANUAL` 仅通过 API 手动触发
- `BATCH` 收集一段时间的变化后批量重载

### `@HotReloadConfig`

用于配置全局热更新设置（建议放在 Application 或配置类上）：

```kotlin
@HotReloadConfig(
    enabled = true,
    watchInterval = 1000L,
    enableGlobalRollback = true,
    globalReloadTimeout = 10000L,
    maxConcurrentReloads = 5,
    enableEventNotification = true,
    reloadHistorySize = 10,
    disableInProduction = true
)
class AppConfig
```

### 排除重载：`@HotReloadExclude`

用于标记不参与热更新的类或方法：

```kotlin
@HotReloadExclude(reason = "稳定类，无需热更新")
class StableHelper
```

## 运行时 API

热更新引擎（`HotReloadEngine`）提供核心运行时能力：

```kotlin
val configManager = HotReloadConfigManager.getInstance().apply { initialize() }
val engine = HotReloadEngine.getInstance()

// 初始化（如果配置未启用会直接返回）
engine.initialize(configManager.getHotReloadConfiguration())

// 手动触发重载
engine.triggerReload("user-service")

// 批量重载
engine.triggerBatchReload(listOf("user-service", "order-service"))

// 注册监听器
engine.addEventListener { event ->
    when (event) {
        is HotReloadEvent.EngineStarted -> println("Engine started")
        is HotReloadEvent.ReloadCompleted -> println("Reload finished: ${event.success}")
        else -> {}
    }
}

// 统计数据
val stats = engine.getReloadStatistics()
val history = engine.getReloadHistory()

// 关闭引擎
engine.shutdown()
```

你也可以通过 `HotReloadController` 管理启用/禁用与服务注册状态：

```kotlin
HotReloadController.initialize(configManager.getHotReloadConfiguration())
HotReloadController.enableGlobal()
HotReloadController.disableGlobal()
```

提示：注解处理器会生成 `HotReloadManager` 和配置类，用于在应用启动时自动注册带注解的服务及其监听路径，无需手动逐个注册。

## 最佳实践

- 仅在 `Debug` 环境启用热更新；生产环境保持禁用。
- 将 `watchPaths` 指向实际会变更的源码/资源路径，减少无效监听。
- 为关键服务实现 `before/after/failed` 钩子，保障可观测性与可靠性。
- 使用 `BATCH` 或 `DELAYED` 策略降低频繁重载的开销。
- 配合 `ServiceLifecycleManager` 在重载后统一执行 `onPause/onResume` 流程。

## 常见问题

- 看不到重载日志？请确认 `fusionkit.hotreload.enableLogging=true`，且日志级别未被宿主屏蔽。
- 重载未生效？检查是否在生产环境（默认禁用），或 `@HotReloadConfig.enabled` 是否为 `true`。
- Hook 方法签名不匹配？请严格遵循注释中的方法签名，否则会被忽略。

## 变更记录

- 1.0.3-beta：改进日志可靠性（`Logger.error` 始终输出），完善文档与使用示例。