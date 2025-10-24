#!/bin/bash

# 作者：Redamancy
# 时间：2025年1月15日
# SDK发布脚本，用于发布1.0.1-beta和1.0.2-beta版本到GitHub Packages

echo "🚀 开始发布FusionKit AutoRegister SDK到GitHub Packages..."

# 检查当前分支
current_branch=$(git branch --show-current)
echo "当前分支: $current_branch"

# 发布1.0.1-beta版本
echo "📦 发布1.0.1-beta版本..."
git checkout feature/1.0.1-beta
if [ $? -eq 0 ]; then
    echo "✅ 切换到feature/1.0.1-beta分支成功"
    
    # 更新版本号
    sed -i '' 's/version = "1.0.0"/version = "1.0.1-beta"/' FusionKit-AutoRegister-Processor/build.gradle.kts
    
    # 清理并构建
    echo "🧹 清理项目..."
    ./gradlew clean
    
    echo "🔨 构建项目..."
    ./gradlew :FusionKit-AutoRegister-Processor:build
    
    echo "📤 发布1.0.1-beta到GitHub Packages..."
    ./gradlew :FusionKit-AutoRegister-Processor:publishGprPublicationToGitHubPackagesRepository
    
    if [ $? -eq 0 ]; then
        echo "✅ 1.0.1-beta版本发布成功！"
    else
        echo "❌ 1.0.1-beta版本发布失败！"
    fi
else
    echo "❌ 切换到feature/1.0.1-beta分支失败！"
fi

echo ""
echo "📦 发布1.0.2-beta版本..."
git checkout feature/1.0.2-beta
if [ $? -eq 0 ]; then
    echo "✅ 切换到feature/1.0.2-beta分支成功"
    
    # 更新版本号
    sed -i '' 's/version = "1.0.0"/version = "1.0.2-beta"/' FusionKit-AutoRegister-Processor/build.gradle.kts
    
    # 清理并构建
    echo "🧹 清理项目..."
    ./gradlew clean
    
    echo "🔨 构建项目..."
    ./gradlew :FusionKit-AutoRegister-Processor:build
    
    echo "📤 发布1.0.2-beta到GitHub Packages..."
    ./gradlew :FusionKit-AutoRegister-Processor:publishGprPublicationToGitHubPackagesRepository
    
    if [ $? -eq 0 ]; then
        echo "✅ 1.0.2-beta版本发布成功！"
    else
        echo "❌ 1.0.2-beta版本发布失败！"
    fi
else
    echo "❌ 切换到feature/1.0.2-beta分支失败！"
fi

# 切换回原分支
echo "🔄 切换回原分支: $current_branch"
git checkout $current_branch

echo ""
echo "🎉 SDK发布完成！"
echo "📋 发布总结："
echo "  - 1.0.1-beta: 基础功能版本"
echo "  - 1.0.2-beta: 增强测试框架和高级功能版本"
echo ""
echo "🔗 GitHub Packages链接："
echo "  https://github.com/Redamancywu/FusionKit-AutoRegister/packages"
