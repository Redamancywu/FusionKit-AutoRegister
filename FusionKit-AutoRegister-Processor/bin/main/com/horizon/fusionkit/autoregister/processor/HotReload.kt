package com.horizon.fusionkit.autoregister.processor

import kotlin.reflect.KClass

/**
 * 热更新注解 - 支持运行时动态重载服务实现
 * 
 * 功能特性：
 * - 文件监听：监听源文件变化，自动触发重载
 * - 动态重载：运行时替换服务实现，无需重启应用
 * - 依赖管理：自动处理服务间的依赖关系
 * - 回滚机制：重载失败时自动回滚到上一个版本
 * - 生命周期钩子：支持重载前后的自定义处理
 * 
 * 使用示例：
 * ```kotlin
 * @HotReload(
 *     value = [UserService::class],
 *     watchPaths = ["src/main/kotlin/com/example/UserServiceImpl.kt"],
 *     reloadStrategy = ReloadStrategy.IMMEDIATE,
 *     enableRollback = true
 * )
 * class UserServiceImpl : UserService {
 *     // 实现代码
 * }
 * ```
 * 
 * @param value 要热更新的接口列表（必填）
 * @param name 服务唯一标识（默认为类简单名）
 * @param watchPaths 监听的文件路径列表（相对于项目根目录）
 * @param reloadStrategy 重载策略（立即重载/延迟重载/手动重载）
 * @param enableRollback 是否启用回滚机制（默认true）
 * @param maxRetries 重载失败时的最大重试次数（默认3）
 * @param reloadTimeout 重载超时时间（毫秒，默认5000）
 * @param enabledIn 启用环境（默认仅在DEBUG环境启用）
 * @param dependencies 依赖的其他热更新服务列表
 * @param beforeReload 重载前执行的钩子方法名
 * @param afterReload 重载后执行的钩子方法名
 * @param onReloadFailed 重载失败时执行的钩子方法名
 * @param enableMetrics 是否启用重载性能监控（默认false）
 * @param enableLogging 是否启用详细日志记录（默认true）
 */
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class HotReload(
    /**
     * 要热更新的接口列表（必填）
     */
    val value: Array<KClass<*>>,
    
    /**
     * 服务唯一标识（默认为类简单名）
     */
    val name: String = "",
    
    /**
     * 监听的文件路径列表（相对于项目根目录）
     * 如果为空，则自动监听当前类文件
     */
    val watchPaths: Array<String> = [],
    
    /**
     * 重载策略
     */
    val reloadStrategy: ReloadStrategy = ReloadStrategy.IMMEDIATE,
    
    /**
     * 是否启用回滚机制（默认true）
     */
    val enableRollback: Boolean = true,
    
    /**
     * 重载失败时的最大重试次数（默认3）
     */
    val maxRetries: Int = 3,
    
    /**
     * 重载超时时间（毫秒，默认5000）
     */
    val reloadTimeout: Long = 5000L,
    
    /**
     * 启用环境（默认仅在DEBUG环境启用）
     */
    val enabledIn: Array<BuildType> = [BuildType.DEBUG],
    
    /**
     * 依赖的其他热更新服务列表
     * 这些服务会在当前服务重载前先重载
     */
    val dependencies: Array<KClass<*>> = [],
    
    /**
     * 重载前执行的钩子方法名
     * 方法签名：fun methodName(): Boolean
     * 返回false将取消重载
     */
    val beforeReload: String = "",
    
    /**
     * 重载后执行的钩子方法名
     * 方法签名：fun methodName(success: Boolean, oldInstance: Any?, newInstance: Any?)
     */
    val afterReload: String = "",
    
    /**
     * 重载失败时执行的钩子方法名
     * 方法签名：fun methodName(error: Throwable, retryCount: Int): Boolean
     * 返回true将进行重试
     */
    val onReloadFailed: String = "",
    
    /**
     * 是否启用重载性能监控（默认false）
     */
    val enableMetrics: Boolean = false,
    
    /**
     * 是否启用详细日志记录（默认true）
     */
    val enableLogging: Boolean = true
)

/**
 * 重载策略枚举
 */
enum class ReloadStrategy {
    /**
     * 立即重载 - 检测到文件变化后立即重载
     */
    IMMEDIATE,
    
    /**
     * 延迟重载 - 文件变化后等待一段时间再重载（避免频繁重载）
     */
    DELAYED,
    
    /**
     * 手动重载 - 仅通过API手动触发重载
     */
    MANUAL,
    
    /**
     * 批量重载 - 收集一段时间内的所有变化，然后批量重载
     */
    BATCH
}

/**
 * 热更新配置注解 - 用于配置全局热更新设置
 * 
 * 使用示例：
 * ```kotlin
 * @HotReloadConfig(
 *     enabled = true,
 *     watchInterval = 1000,
 *     enableGlobalRollback = true
 * )
 * class Application
 * ```
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class HotReloadConfig(
    /**
     * 是否启用热更新（默认false，需要显式启用）
     */
    val enabled: Boolean = false,
    
    /**
     * 文件监听间隔（毫秒，默认1000）
     */
    val watchInterval: Long = 1000L,
    
    /**
     * 是否启用全局回滚机制（默认true）
     */
    val enableGlobalRollback: Boolean = true,
    
    /**
     * 全局重载超时时间（毫秒，默认10000）
     */
    val globalReloadTimeout: Long = 10000L,
    
    /**
     * 最大并发重载数量（默认5）
     */
    val maxConcurrentReloads: Int = 5,
    
    /**
     * 是否启用重载事件通知（默认true）
     */
    val enableEventNotification: Boolean = true,
    
    /**
     * 重载历史记录保留数量（默认10）
     */
    val reloadHistorySize: Int = 10,
    
    /**
     * 是否在生产环境禁用热更新（默认true）
     */
    val disableInProduction: Boolean = true
)

/**
 * 热更新排除注解 - 标记不参与热更新的类或方法
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class HotReloadExclude(
    /**
     * 排除原因说明
     */
    val reason: String = ""
)