package com.horizon.fusionkit.autoregister.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * FusionKit 组合符号处理器提供者
 * 
 * 同时处理：
 * - @AutoRegister 注解（自动注册）
 * - @HotReload 注解（热更新）
 * - @HotReloadConfig 注解（热更新配置）
 */
class FusionKitSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return FusionKitSymbolProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            options = environment.options
        )
    }
}