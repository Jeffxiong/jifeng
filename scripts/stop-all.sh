#!/bin/bash

# 积分系统服务停止脚本
# 功能：停止所有后端和前端服务

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="/tmp/points-system"

# 服务配置
BACKEND_SERVICES=(
    "auth-service:8081"
    "points-service:8082"
    "product-service:8083"
    "api-gateway:8080"
)

FRONTEND_SERVICES=(
    "admin-frontend:5174"
    "jifeng-front:5173"
)

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# 检查端口是否被占用
check_port() {
    local port=$1
    if lsof -ti :$port > /dev/null 2>&1; then
        return 0  # 端口被占用
    else
        return 1  # 端口空闲
    fi
}

# 停止指定端口的服务
stop_port() {
    local port=$1
    local service_name=$2
    
    if check_port $port; then
        print_info "停止服务 $service_name (端口 $port)..."
        lsof -ti :$port | xargs kill -9 2>/dev/null || true
        sleep 1
        if ! check_port $port; then
            print_success "服务 $service_name 已停止"
        else
            print_warning "服务 $service_name 停止失败，可能需要手动处理"
        fi
    else
        print_info "服务 $service_name (端口 $port) 未运行"
    fi
}

# 主函数
main() {
    local service_type=${1:-"all"}  # all, backend, frontend
    
    echo "=========================================="
    echo "  积分系统服务停止脚本"
    echo "=========================================="
    echo ""
    
    # 停止后端服务
    if [ "$service_type" = "all" ] || [ "$service_type" = "backend" ]; then
        print_info "停止后端服务..."
        echo ""
        
        for service_config in "${BACKEND_SERVICES[@]}"; do
            IFS=':' read -r service_name port <<< "$service_config"
            stop_port "$port" "$service_name"
        done
        
        # 停止所有Spring Boot进程
        print_info "清理Spring Boot进程..."
        pkill -f "spring-boot:run" 2>/dev/null || true
        sleep 2
        
        echo ""
    fi
    
    # 停止前端服务
    if [ "$service_type" = "all" ] || [ "$service_type" = "frontend" ]; then
        print_info "停止前端服务..."
        echo ""
        
        for service_config in "${FRONTEND_SERVICES[@]}"; do
            IFS=':' read -r service_name port <<< "$service_config"
            stop_port "$port" "$service_name"
        done
        
        # 停止所有vite进程
        print_info "清理Vite进程..."
        pkill -f "vite" 2>/dev/null || true
        sleep 2
        
        echo ""
    fi
    
    # 清理PID文件
    if [ -d "$LOG_DIR" ]; then
        print_info "清理PID文件..."
        rm -f "$LOG_DIR"/*.pid 2>/dev/null || true
    fi
    
    echo "=========================================="
    print_success "所有服务已停止"
    echo "=========================================="
}

# 执行主函数
main "$@"



