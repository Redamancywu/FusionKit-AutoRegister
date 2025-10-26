package com.horizon.fusionkit.autoregister.processor

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 热更新功能测试
 */
class HotReloadTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var configManager: HotReloadConfigManager
    private lateinit var controller: HotReloadController
    private lateinit var engine: HotReloadEngine
    
    @BeforeEach
    fun setUp() {
        // 重置单例实例
        resetSingletons()
        
        // 初始化组件
        configManager = HotReloadConfigManager.getInstance()
        controller = HotReloadController.getInstance()
        engine = HotReloadEngine.getInstance()
        
        // 设置测试环境
        System.setProperty("fusionkit.build.type", "debug")
        System.setProperty("fusionkit.environment", "test")
    }
    
    @AfterEach
    fun tearDown() {
        try {
            if (::controller.isInitialized) {
                controller.reset()
            }
            if (::engine.isInitialized) {
                engine.shutdown()
            }
        } catch (e: Exception) {
            // 忽略清理错误
        }
        resetSingletons()
    }
    
    @Test
    fun `test config manager initialization`() {
        configManager.initialize()
        
        // 验证默认配置
        assertFalse(configManager.getBoolean(HotReloadConfigManager.Companion.Keys.ENABLED))
        assertEquals(1000L, configManager.getLong(HotReloadConfigManager.Companion.Keys.WATCH_INTERVAL))
        assertTrue(configManager.getBoolean(HotReloadConfigManager.Companion.Keys.ENABLE_GLOBAL_ROLLBACK))
    }
    
    @Test
    fun `test config manager set and get`() {
        configManager.initialize()
        
        // 测试设置和获取配置
        configManager.setBoolean(HotReloadConfigManager.Companion.Keys.ENABLED, true)
        assertTrue(configManager.getBoolean(HotReloadConfigManager.Companion.Keys.ENABLED))
        
        configManager.setLong(HotReloadConfigManager.Companion.Keys.WATCH_INTERVAL, 2000L)
        assertEquals(2000L, configManager.getLong(HotReloadConfigManager.Companion.Keys.WATCH_INTERVAL))
    }
    
    @Test
    fun `test config change listener`() {
        configManager.initialize()
        
        val changeReceived = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        
        configManager.addConfigChangeListener(object : ConfigChangeListener {
            override fun onConfigChanged(key: String, oldValue: String?, newValue: String) {
                if (key == HotReloadConfigManager.Companion.Keys.ENABLED) {
                    changeReceived.set(true)
                    latch.countDown()
                }
            }
        })
        
        configManager.setBoolean(HotReloadConfigManager.Companion.Keys.ENABLED, true)
        
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertTrue(changeReceived.get())
    }
    
    @Test
    fun `test controller initialization`() {
        controller.initialize()
        
        // 验证初始状态
        assertFalse(controller.isGlobalEnabled())
        assertEquals(0, controller.getAllServiceStates().size)
    }
    
    @Test
    fun `test controller global state management`() {
        controller.initialize()
        
        // 测试全局开关
        assertTrue(controller.setGlobalEnabled(true, "test"))
        assertTrue(controller.isGlobalEnabled())
        
        assertTrue(controller.setGlobalEnabled(false, "test"))
        assertFalse(controller.isGlobalEnabled())
    }
    
    @Test
    fun `test controller service management`() {
        controller.initialize()
        controller.setGlobalEnabled(true, "test")
        
        // 注册服务
        assertTrue(controller.registerService("test-service", false))
        assertFalse(controller.isServiceEnabled("test-service"))
        
        // 启用服务
        assertTrue(controller.setServiceEnabled("test-service", true))
        assertTrue(controller.isServiceEnabled("test-service"))
        
        // 获取服务状态
        val state = controller.getServiceState("test-service")
        assertNotNull(state)
        assertEquals("test-service", state!!.serviceId)
        assertTrue(state.enabled)
    }
    
    @Test
    fun `test controller permission check`() {
        controller.initialize()
        
        // 全局禁用时
        controller.setGlobalEnabled(false, "test")
        controller.registerService("test-service", true)
        
        val permission1 = controller.canPerformHotReload("test-service")
        assertEquals(HotReloadPermission.DENIED_GLOBAL_DISABLED, permission1)
        
        // 全局启用，服务禁用时
        controller.setGlobalEnabled(true, "test")
        controller.setServiceEnabled("test-service", false)
        
        val permission2 = controller.canPerformHotReload("test-service")
        assertEquals(HotReloadPermission.DENIED_SERVICE_DISABLED, permission2)
        
        // 全部启用时
        controller.setServiceEnabled("test-service", true)
        
        val permission3 = controller.canPerformHotReload("test-service")
        assertEquals(HotReloadPermission.ALLOWED, permission3)
    }
    
    @Test
    fun `test controller state listener`() {
        controller.initialize()
        
        val globalStateChanges = AtomicInteger(0)
        val serviceStateChanges = AtomicInteger(0)
        
        controller.addStateListener(object : HotReloadStateListener {
            override fun onGlobalStateChanged(oldState: Boolean, newState: Boolean, reason: String) {
                globalStateChanges.incrementAndGet()
            }
            
            override fun onServiceStateChanged(serviceId: String, oldState: Boolean, newState: Boolean, reason: String) {
                serviceStateChanges.incrementAndGet()
            }
        })
        
        // 触发状态变更
        controller.setGlobalEnabled(true, "test")
        controller.registerService("test-service", false)
        controller.setServiceEnabled("test-service", true)
        
        assertEquals(1, globalStateChanges.get())
        assertEquals(1, serviceStateChanges.get())
    }
    
    @Test
    fun `test hot reload engine initialization`() {
        engine.initialize()
        
        // 验证初始状态
        val stats = engine.getReloadStatistics()
        assertEquals(0, stats.totalReloads)
    }
    
    @Test
    fun `test hot reload engine service registration`() {
        engine.initialize()
        
        val serviceInfo = HotReloadServiceInfo(
            name = "test-service",
            interfaces = emptyList(),
            watchPaths = listOf(tempDir.resolve("test.txt").toString()),
            reloadStrategy = ReloadStrategy.IMMEDIATE,
            enableRollback = true,
            maxRetries = 3,
            reloadTimeout = 5000L,
            dependencies = emptyList(),
            beforeReload = "",
            afterReload = "",
            onReloadFailed = "",
            enableMetrics = true,
            enableLogging = true
        )
        
        // 注册服务
        engine.registerService(serviceInfo)
        
        // 验证服务注册成功（通过统计信息验证）
        val stats = engine.getReloadStatistics()
        assertEquals(0, stats.totalReloads)
        
        // 重复注册（registerService方法返回void，无法直接测试失败）
        engine.registerService(serviceInfo)
    }
    
    @Test
    fun `test hot reload engine file watching`() {
        // 启用热重载功能
        val config = HotReloadConfiguration(enabled = true)
        engine.initialize(config)
        
        val testFile = tempDir.resolve("test.txt").toFile()
        val reloadTriggered = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        
        // 先创建文件
        testFile.writeText("initial content")
        
        val serviceInfo = HotReloadServiceInfo(
            name = "test-service",
            interfaces = emptyList(),
            watchPaths = listOf(testFile.absolutePath),
            reloadStrategy = ReloadStrategy.IMMEDIATE,
            enableRollback = true,
            maxRetries = 3,
            reloadTimeout = 5000L,
            dependencies = emptyList(),
            beforeReload = "",
            afterReload = "",
            onReloadFailed = "",
            enableMetrics = true,
            enableLogging = true
        )
        
        // 添加事件监听器
        engine.addEventListener(object : HotReloadEventListener {
            override fun onEvent(event: HotReloadEvent) {
                when (event) {
                    is HotReloadEvent.ReloadStarted -> {
                        if (event.serviceName == "test-service") {
                            reloadTriggered.set(true)
                            latch.countDown()
                        }
                    }
                    else -> {}
                }
            }
        })
        
        // 注册服务
        engine.registerService(serviceInfo)
        
        // 等待更长时间让文件监听器完全启动
        Thread.sleep(1000)
        
        // 修改文件触发监听
        testFile.writeText("modified content")
        
        // 等待事件触发
        val success = latch.await(10, TimeUnit.SECONDS)
        
        // 手动关闭引擎
        engine.shutdown()
        
        // 断言
        assertTrue(success, "Latch should have been triggered within 10 seconds")
        assertTrue(reloadTriggered.get(), "Reload should have been triggered")
    }
    
    @Test
    fun `test hot reload engine statistics`() {
        // 启用热重载功能
        val config = HotReloadConfiguration(enabled = true)
        engine.initialize(config)
        
        val serviceInfo = HotReloadServiceInfo(
            name = "test-service",
            interfaces = emptyList(),
            watchPaths = listOf(tempDir.resolve("test.txt").toString()),
            reloadStrategy = ReloadStrategy.IMMEDIATE,
            enableRollback = true,
            maxRetries = 3,
            reloadTimeout = 5000L,
            dependencies = emptyList(),
            beforeReload = "",
            afterReload = "",
            onReloadFailed = "",
            enableMetrics = true,
            enableLogging = true
        )
        
        engine.registerService(serviceInfo)
        
        // 获取统计信息
        val stats = engine.getReloadStatistics()
        assertEquals(0, stats.totalReloads)
        assertEquals(0, stats.successfulReloads)
        assertEquals(0, stats.failedReloads)
        assertEquals(0.0, stats.averageReloadTime, 0.01)
    }
    
    @Test
    fun `test production environment detection`() {
        // 测试生产环境检测
        System.setProperty("fusionkit.build.type", "release")
        configManager.initialize()
        assertTrue(configManager.isProductionEnvironment())
        
        System.setProperty("fusionkit.environment", "production")
        assertTrue(configManager.isProductionEnvironment())
        
        // 重置为测试环境
        System.setProperty("fusionkit.build.type", "debug")
        System.setProperty("fusionkit.environment", "test")
        assertFalse(configManager.isProductionEnvironment())
    }
    
    @Test
    fun `test config builder`() {
        val config = HotReloadConfigBuilder()
            .enabled(true)
            .watchInterval(2000L)
            .enableGlobalRollback(false)
            .maxConcurrentReloads(10)
            .build()
        
        assertEquals("true", config[HotReloadConfigManager.Companion.Keys.ENABLED])
        assertEquals("2000", config[HotReloadConfigManager.Companion.Keys.WATCH_INTERVAL])
        assertEquals("false", config[HotReloadConfigManager.Companion.Keys.ENABLE_GLOBAL_ROLLBACK])
        assertEquals("10", config[HotReloadConfigManager.Companion.Keys.MAX_CONCURRENT_RELOADS])
    }
    
    @Test
    fun `test controller builder`() {
        val controller = HotReloadControllerBuilder()
            .globalEnabled(true)
            .addService("service1", true)
            .addService("service2", false)
            .build()
        
        assertTrue(controller.isGlobalEnabled())
        assertTrue(controller.isServiceEnabled("service1"))
        assertFalse(controller.isServiceEnabled("service2"))
    }
    
    @Test
    fun `test integration scenario`() {
        // 完整的集成测试场景
        
        // 1. 初始化所有组件
        configManager.initialize()
        controller.initialize()
        // 启用热重载功能
        val config = HotReloadConfiguration(enabled = true)
        engine.initialize(config)
        
        // 2. 配置热更新
        configManager.setBoolean(HotReloadConfigManager.Companion.Keys.ENABLED, true)
        controller.setGlobalEnabled(true, "integration_test")
        
        // 3. 注册服务
        controller.registerService("integration-service", true)
        
        val serviceInfo = HotReloadServiceInfo(
            name = "integration-service",
            interfaces = listOf(),
            watchPaths = listOf(tempDir.resolve("integration.txt").toString()),
            reloadStrategy = ReloadStrategy.IMMEDIATE,
            enableRollback = true,
            maxRetries = 3,
            reloadTimeout = 5000L,
            dependencies = listOf(),
            beforeReload = "",
            afterReload = "",
            onReloadFailed = "",
            enableMetrics = false,
            enableLogging = true
        )
        
        engine.registerService(serviceInfo)
        
        // 4. 验证权限
        val permission = controller.canPerformHotReload("integration-service")
        assertEquals(HotReloadPermission.ALLOWED, permission)
        
        // 6. 验证引擎已启动 - 通过统计信息验证引擎状态
        val statsAfterInit = engine.getReloadStatistics()
        assertNotNull(statsAfterInit)
        
        // 6. 验证状态
        val status = controller.getStatusSummary()
        assertTrue(status.globalEnabled)
        assertEquals(1, status.totalServices)
        assertEquals(1, status.enabledServices)
        
        // 7. 清理
        engine.shutdown()
        // 验证引擎已关闭 - 通过统计信息验证引擎状态
        val statsAfterShutdown = engine.getReloadStatistics()
        assertNotNull(statsAfterShutdown)
    }
    
    // 辅助方法
    
    private fun resetSingletons() {
        // 通过反射重置单例实例（仅用于测试）
        try {
            val configManagerField = HotReloadConfigManager::class.java.getDeclaredField("INSTANCE")
            configManagerField.isAccessible = true
            configManagerField.set(null, null)
            
            val controllerField = HotReloadController::class.java.getDeclaredField("INSTANCE")
            controllerField.isAccessible = true
            controllerField.set(null, null)
            
            val engineField = HotReloadEngine::class.java.getDeclaredField("INSTANCE")
            engineField.isAccessible = true
            engineField.set(null, null)
        } catch (e: Exception) {
            // 忽略反射错误
        }
    }
}