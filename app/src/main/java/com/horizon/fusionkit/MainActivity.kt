package com.horizon.fusionkit

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.horizon.fusionkit.test.TestServiceProviders

/**
 * 主Activity - 演示AutoRegister插件使用
 * 作者：Redamancy
 * 时间：2025-01-27
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "AutoRegisterDemo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 演示AutoRegister插件的使用
        demonstrateAutoRegister()
    }
    
    private fun demonstrateAutoRegister() {
        Log.d(TAG, "=== AutoRegister 插件演示 ===")
        
        // 1. 获取所有服务
        val allServices = TestServiceProviders.all
        val testService = TestServiceProviders.get("primary")
        Log.d(TAG, "所有服务数量: ${allServices.size}")
        allServices.forEach { service ->
            Log.d(TAG, "服务: ${service.getName()}, 优先级: ${service.getPriority()}")
            Log.d(TAG,"测试内容："+service.Test())
        }
          Log.d(TAG,"通过指定的服务获取的 服务为: ${testService?.getName()}, 优先级: ${testService?.getPriority()} ，测试内容：${testService?.Test()}")


        
        // 2. 按名称获取服务
        val primaryService = TestServiceProviders.get("primary")
        Log.d(TAG, "主服务: ${primaryService?.getName()}")
        
        val objectService = TestServiceProviders.get("object")
        Log.d(TAG, "对象服务: ${objectService?.getName()}")
        
        // 3. 按类型获取服务
        val coreServices = TestServiceProviders.getAll("core")
        Log.d(TAG, "核心服务数量: ${coreServices.size}")
        
        val singletonServices = TestServiceProviders.getAll("singleton")
        Log.d(TAG, "单例服务数量: ${singletonServices.size}")
        
        // 4. 获取最佳服务
        val bestService = TestServiceProviders.getBest()
        Log.d(TAG, "最佳服务: ${bestService?.getName()}, 优先级: ${bestService?.getPriority()}")
        
        val bestCoreService = TestServiceProviders.getBest("core")
        Log.d(TAG, "最佳核心服务: ${bestCoreService?.getName()}")
        
        Log.d(TAG, "=== AutoRegister 插件演示完成 ===")
    }
}
