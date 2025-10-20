#!/bin/bash

echo "🚀 发布 FusionKit AutoRegister 到 GitHub Packages 和 JitPack"

# 检查本地配置文件
if [ ! -f "gradle.properties.local" ]; then
    echo "❌ 错误: 未找到 gradle.properties.local 文件"
    echo "请创建 gradle.properties.local 文件并配置你的 GitHub Token"
    exit 1
fi

# 复制本地配置
echo "📋 使用本地配置..."
cp gradle.properties.local gradle.properties

# 发布到 GitHub Packages
echo "📦 发布到 GitHub Packages..."
./gradlew :FusionKit-AutoRegister-Processor:publishGprPublicationToGitHubPackagesRepository

if [ $? -eq 0 ]; then
    echo "✅ GitHub Packages 发布成功！"
else
    echo "❌ GitHub Packages 发布失败"
    exit 1
fi

# 恢复模板配置
echo "🔄 恢复模板配置..."
git checkout gradle.properties

echo "🎉 发布完成！"
echo "📋 使用方式："
echo "1. GitHub Packages: 需要认证配置"
echo "2. JitPack: 直接使用 com.github.Redamancywu:FusionKit-AutoRegister:v1.0.0"
