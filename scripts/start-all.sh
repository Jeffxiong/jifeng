#!/bin/bash

# 积分系统完整启动脚本
# 功能：按顺序启动所有后端和前端服务，包括完整自检

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPTS_DIR="$PROJECT_ROOT/scripts"

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_header() {
    echo -e "${CYAN}$1${NC}"
}

# 主函数
main() {
    echo "=========================================="
    echo "  积分系统完整启动脚本"
    echo "=========================================="
    echo ""
    
    # 检查脚本是否存在
    if [ ! -f "$SCRIPTS_DIR/start-backend.sh" ]; then
        echo "错误: 找不到 start-backend.sh 脚本"
        exit 1
    fi
    
    if [ ! -f "$SCRIPTS_DIR/start-frontend.sh" ]; then
        echo "错误: 找不到 start-frontend.sh 脚本"
        exit 1
    fi
    
    # 给脚本添加执行权限
    chmod +x "$SCRIPTS_DIR"/*.sh 2>/dev/null || true
    
    # 第一步：启动后端服务
    print_header "第一步：启动后端服务"
    echo ""
    
    if "$SCRIPTS_DIR/start-backend.sh"; then
        print_success "后端服务启动完成"
    else
        echo "后端服务启动失败，请检查日志"
        exit 1
    fi
    
    echo ""
    echo "----------------------------------------"
    echo ""
    
    # 等待一下，确保后端服务完全启动
    print_info "等待后端服务完全启动..."
    sleep 5
    
    # 第二步：启动前端服务
    print_header "第二步：启动前端服务"
    echo ""
    
    if "$SCRIPTS_DIR/start-frontend.sh"; then
        print_success "前端服务启动完成"
    else
        echo "前端服务启动失败，请检查日志"
        exit 1
    fi
    
    echo ""
    echo "=========================================="
    print_success "所有服务启动完成！"
    echo "=========================================="
    echo ""
    echo "服务访问地址："
    echo ""
    echo "后端服务："
    echo "  - API网关: http://localhost:8080"
    echo "  - 认证服务: http://localhost:8081"
    echo "  - 积分服务: http://localhost:8082"
    echo "  - 产品服务: http://localhost:8083"
    echo ""
    echo "前端服务："
    echo "  - 管理后台: http://localhost:5174"
    echo "  - 用户前端: http://localhost:5173"
    echo ""
    echo "常用命令："
    echo "  - 检查服务状态: ./scripts/check-services.sh"
    echo "  - 停止所有服务: ./scripts/stop-all.sh"
    echo "  - 查看日志: tail -f /tmp/points-system/*.log"
    echo ""
}

# 执行主函数
main "$@"




