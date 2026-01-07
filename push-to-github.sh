#!/bin/bash

# 推送代码到 GitHub 的脚本

echo "📋 准备推送代码到 GitHub..."
echo ""

cd "$(dirname "$0")"

echo "📋 当前状态："
echo "   - 本地文件：$(git ls-files | wc -l | tr -d ' ') 个"
echo "   - 远程文件：$(git ls-tree -r --name-only origin/main 2>/dev/null | wc -l | tr -d ' ') 个"
echo ""

echo "📋 开始推送..."
echo "💡 如果提示输入用户名和密码："
echo "   用户名：Jeffxiong"
echo "   密码：使用 Personal Access Token（不是 GitHub 密码）"
echo ""

git push -u origin main

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 代码推送成功！"
    echo "🌐 查看代码：https://github.com/Jeffxiong/jifeng"
else
    echo ""
    echo "❌ 推送失败"
    echo ""
    echo "💡 如果提示需要身份验证："
    echo "   1. 访问：https://github.com/settings/tokens"
    echo "   2. 生成新的 Personal Access Token（classic）"
    echo "   3. 勾选 'repo' 权限"
    echo "   4. 复制 token，在提示输入密码时粘贴"
    echo ""
    echo "   或者使用强制推送："
    echo "   git push -u origin main --force"
fi

