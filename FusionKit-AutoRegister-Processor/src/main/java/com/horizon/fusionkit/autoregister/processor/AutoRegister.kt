package com.horizon.fusionkit.autoregister.processor

import kotlin.reflect.KClass

/**
 * 多模块服务自动注册注解。
 *
 * - [value]: 必填，要注册的接口列表
 * - [name]: 唯一标识（默认为类简单名）
 * - [type]: 业务分组（默认为接口全限定名）
 * - [priority]: 优先级（越大越优先，默认 0）
 * - [enabledIn]: 启用环境（DEBUG/RELEASE/ALL）
 * - [isObject]: 是否为 Kotlin object（默认 false）
 * - [configFile]: 配置文件路径（配置化服务）
 * - [configKey]: 配置键名（配置化服务）
 * - [enableLifecycle]: 启用生命周期管理（默认 false）
 * - [enableMetrics]: 启用性能监控（默认 false）
 * - [pluginId]: 插件ID（插件系统）
 * - [pluginVersion]: 插件版本（插件系统）
 */
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class AutoRegister(
    val value: Array<KClass<*>>,
    val name: String = "",
    val type: String = "",
    val priority: Int = 0,
    val enabledIn: Array<BuildType> = [BuildType.ALL],
    val isObject: Boolean = false,
    val configFile: String = "",
    val configKey: String = "",
    val enableLifecycle: Boolean = false,
    val enableMetrics: Boolean = false,
    val pluginId: String = "",
    val pluginVersion: String = ""
)

enum class BuildType {
    DEBUG, RELEASE, ALL
}