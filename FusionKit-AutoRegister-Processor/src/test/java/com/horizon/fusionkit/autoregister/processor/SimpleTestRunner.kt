package com.horizon.fusionkit.autoregister.processor

/**
 * 作者：Redamancy
 * 时间：2025年1月15日
 * 简单的测试运行器，用于手动运行测试
 */
object SimpleTestRunner {
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("开始运行AutoRegisterSymbolProcessorTest...")
        
        try {
            val test = AutoRegisterSymbolProcessorTest()
            
            // 运行测试方法
            println("运行测试: test service entry creation")
            test.`test service entry creation`()
            println("✅ test service entry creation 通过")
            
            println("运行测试: test service entry sorting by priority")
            test.`test service entry sorting by priority`()
            println("✅ test service entry sorting by priority 通过")
            
            println("运行测试: test companion object constants")
            test.`test companion object constants`()
            println("✅ test companion object constants 通过")
            
            println("运行测试: test build type enum values")
            test.`test build type enum values`()
            println("✅ test build type enum values 通过")
            
            println("运行测试: test auto register annotation default values")
            test.`test auto register annotation default values`()
            println("✅ test auto register annotation default values 通过")
            
            println("\n🎉 所有测试都通过了！")
            
        } catch (e: Exception) {
            println("❌ 测试失败: ${e.message}")
            e.printStackTrace()
        }
    }
}
