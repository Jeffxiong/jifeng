#!/bin/bash

# 积分系统前端服务启动脚本
# 功能：启动管理后台和用户前端，包括自检功能

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

# 前端服务配置
FRONTEND_SERVICES=(
    "admin-frontend:5174:管理后台"
    "jifeng-front:5173:用户前端"
)

# 创建日志目录
mkdir -p "$LOG_DIR"

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

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
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

# 检查服务是否启动成功
check_service() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=0
    
    print_info "检查服务 $service_name (端口 $port) 是否启动..."
    
    while [ $attempt -lt $max_attempts ]; do
        if check_port $port; then
            # 尝试HTTP请求验证服务是否真正可用
            if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port" | grep -q "200\|404\|403"; then
                print_success "服务 $service_name 已成功启动 (端口 $port)"
                return 0
            fi
        fi
        attempt=$((attempt + 1))
        sleep 2
    done
    
    print_error "服务 $service_name 启动超时或失败"
    return 1
}

# 停止已运行的前端服务
stop_existing_services() {
    print_info "检查并停止已运行的前端服务..."
    
    for service_config in "${FRONTEND_SERVICES[@]}"; do
        IFS=':' read -r service_name port description <<< "$service_config"
        if check_port $port; then
            print_warning "发现端口 $port 被占用，正在停止相关服务..."
            lsof -ti :$port | xargs kill -9 2>/dev/null || true
            sleep 1
        fi
    done
    
    # 停止所有vite进程
    pkill -f "vite" 2>/dev/null || true
    sleep 2
}

# 检查并安装依赖
check_dependencies() {
    local service_dir=$1
    local service_name=$2
    
    print_info "检查 $service_name 的依赖..."
    
    if [ ! -d "$service_dir/node_modules" ]; then
        print_warning "$service_name 的依赖未安装，正在安装..."
        cd "$service_dir"
        if npm install > "$LOG_DIR/${service_name}-install.log" 2>&1; then
            print_success "$service_name 依赖安装成功"
        else
            print_error "$service_name 依赖安装失败，请查看日志: $LOG_DIR/${service_name}-install.log"
            return 1
        fi
    else
        print_success "$service_name 依赖已存在"
    fi
}

# 启动单个前端服务
start_service() {
    local service_name=$1
    local port=$2
    local description=$3
    local service_dir="$PROJECT_ROOT/$service_name"
    local log_file="$LOG_DIR/${service_name}.log"
    
    print_info "启动服务: $description ($service_name) - 端口: $port"
    
    # 检查服务目录是否存在
    if [ ! -d "$service_dir" ]; then
        print_error "服务目录不存在: $service_dir"
        return 1
    fi
    
    # 检查并安装依赖
    if ! check_dependencies "$service_dir" "$service_name"; then
        return 1
    fi
    
    # 启动服务
    cd "$service_dir"
    nohup npm run dev > "$log_file" 2>&1 &
    local pid=$!
    echo $pid > "$LOG_DIR/${service_name}.pid"
    
    print_info "服务 $description 正在启动中... (PID: $pid, 日志: $log_file)"
}

# 主函数
main() {
    local service_to_start=${1:-"all"}  # 默认启动所有服务
    
    echo "=========================================="
    echo "  积分系统前端服务启动脚本"
    echo "=========================================="
    echo ""
    
    # 环境检查
    print_info "检查环境..."
    
    # 检查Node.js
    if ! command -v node &> /dev/null; then
        print_error "未找到 Node.js，请先安装 Node.js 18+"
        exit 1
    fi
    node_version=$(node -v)
    print_success "Node.js: $node_version"
    
    # 检查npm
    if ! command -v npm &> /dev/null; then
        print_error "未找到 npm，请先安装 npm"
        exit 1
    fi
    npm_version=$(npm -v)
    print_success "npm: $npm_version"
    
    echo ""
    
    # 停止已运行的服务
    stop_existing_services
    
    echo ""
    print_info "开始启动前端服务..."
    echo ""
    
    # 启动各个服务
    local failed_services=()
    
    for service_config in "${FRONTEND_SERVICES[@]}"; do
        IFS=':' read -r service_name port description <<< "$service_config"
        
        # 如果指定了特定服务，只启动该服务
        if [ "$service_to_start" != "all" ] && [ "$service_to_start" != "$service_name" ]; then
            continue
        fi
        
        start_service "$service_name" "$port" "$description"
        
        # 等待服务启动
        sleep 8
        
        # 检查服务状态
        if ! check_service "$service_name" "$port"; then
            failed_services+=("$service_name")
            print_error "服务 $description 启动失败，请查看日志: $LOG_DIR/${service_name}.log"
        fi
        
        echo ""
    done
    
    # 最终状态报告
    echo "=========================================="
    echo "  启动完成状态报告"
    echo "=========================================="
    
    for service_config in "${FRONTEND_SERVICES[@]}"; do
        IFS=':' read -r service_name port description <<< "$service_config"
        if [ "$service_to_start" != "all" ] && [ "$service_to_start" != "$service_name" ]; then
            continue
        fi
        
        if check_port $port; then
            print_success "$description - 运行中 (端口: $port)"
        else
            print_error "$description - 未运行 (端口: $port)"
        fi
    done
    
    echo ""
    
    if [ ${#failed_services[@]} -eq 0 ]; then
        print_success "所有前端服务启动成功！"
        echo ""
        echo "服务访问地址："
        for service_config in "${FRONTEND_SERVICES[@]}"; do
            IFS=':' read -r service_name port description <<< "$service_config"
            if [ "$service_to_start" = "all" ] || [ "$service_to_start" = "$service_name" ]; then
                echo "  - $description: http://localhost:$port"
            fi
        done
        echo ""
        echo "日志文件位置: $LOG_DIR"
        echo ""
        echo "查看日志命令:"
        for service_config in "${FRONTEND_SERVICES[@]}"; do
            IFS=':' read -r service_name port description <<< "$service_config"
            if [ "$service_to_start" = "all" ] || [ "$service_to_start" = "$service_name" ]; then
                echo "  tail -f $LOG_DIR/${service_name}.log"
            fi
        done
        return 0
    else
        print_error "以下服务启动失败: ${failed_services[*]}"
        print_info "请查看日志文件排查问题: $LOG_DIR"
        return 1
    fi
}

# 执行主函数
main "$@"

