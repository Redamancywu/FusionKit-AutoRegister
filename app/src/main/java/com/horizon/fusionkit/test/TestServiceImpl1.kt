package com.horizon.fusionkit.test

import com.horizon.fusionkit.autoregister.processor.AutoRegister
import com.horizon.fusionkit.autoregister.processor.BuildType

/**
 * 测试服务实现1 - 高优先级
 * 作者：Redamancy
 * 时间：2025-01-27
 */
@AutoRegister(
    value = [TestService::class],
    name = "primary",
    type = "core",
    priority = 100,
    enabledIn = [BuildType.ALL]
)
class TestServiceImpl1 : TestService {
    override fun getName(): String = "PrimaryService"
    override fun getPriority(): Int = 100
    override fun Test(): String {
  return "测试服务实现1"
    }
}
