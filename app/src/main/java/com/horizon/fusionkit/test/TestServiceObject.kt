package com.horizon.fusionkit.test

import com.horizon.fusionkit.autoregister.processor.AutoRegister
import com.horizon.fusionkit.autoregister.processor.BuildType

/**
 * 测试服务Object实现 - 最高优先级
 * 作者：Redamancy
 * 时间：2025-01-27
 */
@AutoRegister(
    value = [TestService::class],
    name = "object",
    type = "singleton",
    priority = 200,
    enabledIn = [BuildType.ALL],
    isObject = true
)
object TestServiceObject : TestService {
    override fun getName(): String = "ObjectService"
    override fun getPriority(): Int = 200
    override fun Test(): String {
        return "测试服务实现3"
    }
}
