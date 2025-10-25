package com.horizon.fusionkit.autoregister.processor

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 集成测试 - 测试各组件之间的协作
 */
class IntegrationTest {

    @BeforeEach
    fun setUp() {
        // 重置所有组件状态
        ConfigServiceManager.clearCache()
        ServiceLifecycleManager.destroy()
        PerformanceMonitor.reset()
        PluginManager.destroyAll()
    }

    @Test
    fun `test complete service lifecycle with performance monitoring`() {
        // 初始化性能监控
        PerformanceMonitor.initialize(MonitorConfig(
            enabled = true,
            enableMetrics = true,
            logLevel = LogLevel.INFO
        ))

        // 创建模拟服务
        val mockService = mockk<LifecycleService>()
        every { mockService.onCreate() } returns Unit
        every { mockService.onDestroy() } returns Unit

        // 注册服务到生命周期管理器
        ServiceLifecycleManager.registerService(mockService)
        assertEquals(1, ServiceLifecycleManager.getServiceCount())

        // 初始化生命周期管理器
        ServiceLifecycleManager.initialize()
        assertTrue(ServiceLifecycleManager.isInitialized())
        verify { mockService.onCreate() }

        // 使用性能监控跟踪服务调用
        val result = PerformanceMonitor.trackCall("TestService") {
            "service result"
        }
        assertEquals("service result", result)

        // 验证性能指标
        val metrics = PerformanceMonitor.getMetrics("TestService")!!
        assertEquals(1, metrics.callCount.get())
        assertTrue(metrics.averageTime >= 0)

        // 销毁服务
        ServiceLifecycleManager.destroy()
        assertFalse(ServiceLifecycleManager.isInitialized())
        verify { mockService.onDestroy() }
    }

    @Test
    fun `test configuration management with lifecycle integration`() {
        // 设置配置
        ConfigServiceManager.setConfig("app.properties", "service.enabled", true)
        ConfigServiceManager.setConfig("app.properties", "service.timeout", 5000)

        // 验证配置读取
        val enabled = ConfigServiceManager.getConfig("app.properties", "service.enabled", false)
        val timeout = ConfigServiceManager.getConfig("app.properties", "service.timeout", 3000)
        
        assertTrue(enabled)
        assertEquals(5000, timeout)

        // 测试配置监听器
        var configChanged = false
        ConfigServiceManager.addConfigListener("app.properties", "service.enabled") {
            configChanged = true
        }

        // 更新配置触发监听器
        ConfigServiceManager.setConfig("app.properties", "service.enabled", false)
        assertTrue(configChanged)

        // 验证配置已更新
        val updatedEnabled = ConfigServiceManager.getConfig("app.properties", "service.enabled", true)
        assertFalse(updatedEnabled)
    }

    @Test
    fun `test plugin system with performance monitoring`() {
        // 初始化插件管理器
        val pluginConfig = PluginConfig(
            enabled = true,
            pluginDir = "test-plugins",
            autoLoad = false,
            logLevel = PluginLogLevel.INFO
        )
        PluginManager.initialize(pluginConfig)

        // 验证插件管理器状态
        val plugins = PluginManager.getAllPlugins()
        assertTrue(plugins.isEmpty()) // 没有实际插件文件

        // 测试插件信息创建
        val pluginInfo = PluginInfo(
            id = "integration-test-plugin",
            name = "Integration Test Plugin",
            version = "1.0.0",
            description = "Plugin for integration testing",
            author = "Test Framework",
            className = "com.test.IntegrationTestPlugin"
        )

        assertEquals("integration-test-plugin", pluginInfo.id)
        assertEquals("Integration Test Plugin", pluginInfo.name)
        assertFalse(pluginInfo.loaded)

        // 使用性能监控跟踪插件相关操作
        PerformanceMonitor.trackCall("PluginOperation") {
            // 模拟插件操作
            Thread.sleep(1) // 确保有可测量的时间
            "plugin operation completed"
        }

        val metrics = PerformanceMonitor.getMetrics("PluginOperation")!!
        assertEquals(1, metrics.callCount.get())
        assertTrue(metrics.averageTime > 0)
    }

    @Test
    fun `test error handling across components`() {
        // 初始化性能监控
        PerformanceMonitor.initialize()

        // 测试错误跟踪
        try {
            PerformanceMonitor.trackCall("ErrorService") {
                throw RuntimeException("Test error")
            }
        } catch (e: RuntimeException) {
            // 预期的异常
        }

        // 验证错误被正确记录
        val metrics = PerformanceMonitor.getMetrics("ErrorService")!!
        assertEquals(1, metrics.callCount.get())
        assertEquals(1, metrics.errorCount.get())
        assertEquals(1.0, metrics.errorRate)

        // 生成性能报告
        val report = PerformanceMonitor.generateReport()
        assertEquals(1, report.totalCalls)
        assertEquals(1, report.totalErrors)
        assertEquals(1, report.serviceCount)
    }

    @Test
    fun `test service entry with all features`() {
        // 测试ServiceEntry数据类的完整功能
        val entry = AutoRegisterSymbolProcessor.ServiceEntry(
            className = "com.example.IntegrationService",
            name = "integration-service",
            type = "integration",
            priority = 200,
            isObject = true,
            configFile = "integration.properties",
            configKey = "integration.service",
            enableLifecycle = true,
            enableMetrics = true,
            pluginId = "integration-plugin",
            pluginVersion = "2.0.0"
        )

        // 验证所有属性
        assertEquals("com.example.IntegrationService", entry.className)
        assertEquals("integration-service", entry.name)
        assertEquals("integration", entry.type)
        assertEquals(200, entry.priority)
        assertTrue(entry.isObject)
        assertEquals("integration.properties", entry.configFile)
        assertEquals("integration.service", entry.configKey)
        assertTrue(entry.enableLifecycle)
        assertTrue(entry.enableMetrics)
        assertEquals("integration-plugin", entry.pluginId)
        assertEquals("2.0.0", entry.pluginVersion)
    }
}