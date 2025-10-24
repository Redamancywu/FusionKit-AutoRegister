#!/bin/bash

# 作者：Redamancy
# 时间：2025年1月15日
# GitHub Token权限检查脚本

echo "🔍 检查GitHub Token权限..."

# 设置Token
TOKEN="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"

echo "📋 检查Token基本信息..."
curl -H "Authorization: token $TOKEN" \
     -H "Accept: application/vnd.github.v3+json" \
     https://api.github.com/user

echo ""
echo "📦 检查GitHub Packages权限..."
curl -H "Authorization: token $TOKEN" \
     -H "Accept: application/vnd.github.v3+json" \
     https://api.github.com/user/packages

echo ""
echo "🏗️ 检查仓库权限..."
curl -H "Authorization: token $TOKEN" \
     -H "Accept: application/vnd.github.v3+json" \
     https://api.github.com/repos/Redamancywu/FusionKit-AutoRegister

echo ""
echo "📊 检查包管理权限..."
curl -H "Authorization: token $TOKEN" \
     -H "Accept: application/vnd.github.v3+json" \
     https://api.github.com/orgs/Redamancywu/packages

echo ""
echo "✅ Token权限检查完成！"
echo ""
echo "🔧 如果出现403错误，请检查："
echo "1. Token是否包含 'write:packages' 权限"
echo "2. Token是否包含 'read:packages' 权限"
echo "3. Token是否包含 'repo' 权限"
echo "4. Token是否已过期"
echo ""
echo "🔗 更新Token权限："
echo "https://github.com/settings/tokens"
