package com.horizon.fusionkit


import com.horizon.fusionkit.test.TestServiceProviders
import org.junit.Test
import org.junit.Assert.*

/**
 * AutoRegister 插件功能测试
 * 作者：Redamancy
 * 时间：2025-01-27
 */
class AutoRegisterTest {

    @Test
    fun testAllServicesRetrieved() {
        // 测试获取所有服务
        val allServices = TestServiceProviders.all
        assertNotNull("所有服务列表不应为空", allServices)
        assertEquals("应该有3个服务实现", 3, allServices.size)
        
        // 验证服务按优先级排序（从高到低）
        val priorities = allServices.map { it.getPriority() }
        assertEquals("服务应按优先级排序", listOf(200, 100, 50), priorities)
    }

    @Test
    fun testGetServiceByName() {
        // 测试按名称获取服务
        val primaryService = TestServiceProviders.get("primary")
        assertNotNull("主服务不应为空", primaryService)
        assertEquals("主服务名称", "PrimaryService", primaryService?.getName())
        assertEquals("主服务优先级", 100, primaryService?.getPriority())

        val secondaryService = TestServiceProviders.get("secondary")
        assertNotNull("次服务不应为空", secondaryService)
        assertEquals("次服务名称", "SecondaryService", secondaryService?.getName())

        val objectService = TestServiceProviders.get("object")
        assertNotNull("对象服务不应为空", objectService)
        assertEquals("对象服务名称", "ObjectService", objectService?.getName())
        assertEquals("对象服务优先级", 200, objectService?.getPriority())
    }

    @Test
    fun testGetServiceByType() {
        // 测试按类型获取服务
        val coreServices = TestServiceProviders.getAll("core")
        assertNotNull("核心服务列表不应为空", coreServices)
        assertEquals("应该有1个核心服务", 1, coreServices.size)
        assertEquals("核心服务名称", "PrimaryService", coreServices.first().getName())

        val backupServices = TestServiceProviders.getAll("backup")
        assertNotNull("备份服务列表不应为空", backupServices)
        assertEquals("应该有1个备份服务", 1, backupServices.size)
        assertEquals("备份服务名称", "SecondaryService", backupServices.first().getName())

        val singletonServices = TestServiceProviders.getAll("singleton")
        assertNotNull("单例服务列表不应为空", singletonServices)
        assertEquals("应该有1个单例服务", 1, singletonServices.size)
        assertEquals("单例服务名称", "ObjectService", singletonServices.first().getName())
    }

    @Test
    fun testGetBestService() {
        // 测试获取最佳服务
        val bestCoreService = TestServiceProviders.getBest("core")
        assertNotNull("最佳核心服务不应为空", bestCoreService)
        assertEquals("最佳核心服务名称", "PrimaryService", bestCoreService?.getName())

        val bestBackupService = TestServiceProviders.getBest("backup")
        assertNotNull("最佳备份服务不应为空", bestBackupService)
        assertEquals("最佳备份服务名称", "SecondaryService", bestBackupService?.getName())

        val bestSingletonService = TestServiceProviders.getBest("singleton")
        assertNotNull("最佳单例服务不应为空", bestSingletonService)
        assertEquals("最佳单例服务名称", "ObjectService", bestSingletonService?.getName())

        // 测试获取全局最佳服务（应该是最优先级的）
        val globalBestService = TestServiceProviders.getBest()
        assertNotNull("全局最佳服务不应为空", globalBestService)
        assertEquals("全局最佳服务名称", "ObjectService", globalBestService?.getName())
        assertEquals("全局最佳服务优先级", 200, globalBestService?.getPriority())
    }

    @Test
    fun testServiceInstances() {
        // 测试服务实例类型
        val allServices = TestServiceProviders.all
        
        // 验证对象服务是单例
        val objectService = allServices.find { it.getName() == "ObjectService" }
        assertNotNull("对象服务应该存在", objectService)
        
        // 验证普通服务实例
        val primaryService = allServices.find { it.getName() == "PrimaryService" }
        assertNotNull("主服务应该存在", primaryService)
        assertTrue("主服务应该是TestServiceImpl1的实例", 
            primaryService is com.horizon.fusionkit.test.TestServiceImpl1)
    }

    @Test
    fun testServiceRegistration() {
        // 测试服务注册完整性
        val allServices = TestServiceProviders.all
        val serviceNames = allServices.map { it.getName() }.toSet()
        
        assertTrue("应该包含PrimaryService", serviceNames.contains("PrimaryService"))
        assertTrue("应该包含SecondaryService", serviceNames.contains("SecondaryService"))
        assertTrue("应该包含ObjectService", serviceNames.contains("ObjectService"))
        
        // 验证服务数量
        assertEquals("应该有3个不同的服务", 3, serviceNames.size)
    }
}
