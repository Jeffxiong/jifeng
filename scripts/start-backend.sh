#!/bin/bash

# 积分系统后端服务启动脚本
# 功能：按顺序启动所有后端微服务，包括自检功能

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
LOG_DIR="/tmp/points-system"

# 服务配置
SERVICES=(
    "auth-service:8081"
    "points-service:8082"
    "product-service:8083"
    "api-gateway:8080"
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
            if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port" > /dev/null 2>&1 || [ "$service_name" = "api-gateway" ]; then
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

# 停止已运行的服务
stop_existing_services() {
    print_info "检查并停止已运行的服务..."
    
    for service_config in "${SERVICES[@]}"; do
        IFS=':' read -r service_name port <<< "$service_config"
        if check_port $port; then
            print_warning "发现端口 $port 被占用，正在停止相关服务..."
            lsof -ti :$port | xargs kill -9 2>/dev/null || true
            sleep 1
        fi
    done
    
    # 停止所有Spring Boot进程
    pkill -f "spring-boot:run" 2>/dev/null || true
    sleep 2
}

# 编译公共模块
build_common() {
    print_info "编译公共模块 (common)..."
    cd "$BACKEND_DIR/common"
    if mvn clean install -q > "$LOG_DIR/common-build.log" 2>&1; then
        print_success "公共模块编译成功"
    else
        print_error "公共模块编译失败，请查看日志: $LOG_DIR/common-build.log"
        return 1
    fi
}

# 启动单个服务
start_service() {
    local service_name=$1
    local port=$2
    local service_dir="$BACKEND_DIR/$service_name"
    local log_file="$LOG_DIR/${service_name}.log"
    
    print_info "启动服务: $service_name (端口: $port)"
    
    # 检查服务目录是否存在
    if [ ! -d "$service_dir" ]; then
        print_error "服务目录不存在: $service_dir"
        return 1
    fi
    
    # 启动服务
    cd "$service_dir"
    nohup mvn spring-boot:run > "$log_file" 2>&1 &
    local pid=$!
    echo $pid > "$LOG_DIR/${service_name}.pid"
    
    print_info "服务 $service_name 正在启动中... (PID: $pid, 日志: $log_file)"
}

# 主函数
main() {
    echo "=========================================="
    echo "  积分系统后端服务启动脚本"
    echo "=========================================="
    echo ""
    
    # 环境检查
    print_info "检查环境..."
    
    # 检查Java
    if ! command -v java &> /dev/null; then
        print_error "未找到 Java，请先安装 JDK 17+"
        exit 1
    fi
    java_version=$(java -version 2>&1 | head -n 1)
    print_success "Java: $java_version"
    
    # 检查Maven
    if ! command -v mvn &> /dev/null; then
        print_error "未找到 Maven，请先安装 Maven 3.6+"
        exit 1
    fi
    mvn_version=$(mvn -version | head -n 1)
    print_success "Maven: $mvn_version"
    
    # 检查MySQL（可选）
    if command -v mysql &> /dev/null; then
        if mysql -u root -proot -e "SELECT 1;" > /dev/null 2>&1; then
            print_success "MySQL 连接正常"
        else
            print_warning "MySQL 连接失败，请检查数据库配置"
        fi
    else
        print_warning "未找到 MySQL 客户端，跳过数据库检查"
    fi
    
    echo ""
    
    # 停止已运行的服务
    stop_existing_services
    
    # 编译公共模块
    build_common
    
    echo ""
    print_info "开始启动后端服务..."
    echo ""
    
    # 启动各个服务
    local failed_services=()
    
    for service_config in "${SERVICES[@]}"; do
        IFS=':' read -r service_name port <<< "$service_config"
        
        start_service "$service_name" "$port"
        
        # 等待服务启动
        if [ "$service_name" = "api-gateway" ]; then
            sleep 10  # API网关需要更多时间
        else
            sleep 5
        fi
        
        # 检查服务状态
        if ! check_service "$service_name" "$port"; then
            failed_services+=("$service_name")
            print_error "服务 $service_name 启动失败，请查看日志: $LOG_DIR/${service_name}.log"
        fi
        
        echo ""
    done
    
    # 最终状态报告
    echo "=========================================="
    echo "  启动完成状态报告"
    echo "=========================================="
    
    for service_config in "${SERVICES[@]}"; do
        IFS=':' read -r service_name port <<< "$service_config"
        if check_port $port; then
            print_success "$service_name - 运行中 (端口: $port)"
        else
            print_error "$service_name - 未运行 (端口: $port)"
        fi
    done
    
    echo ""
    
    if [ ${#failed_services[@]} -eq 0 ]; then
        print_success "所有后端服务启动成功！"
        echo ""
        echo "服务访问地址："
        echo "  - API网关: http://localhost:8080"
        echo "  - 认证服务: http://localhost:8081"
        echo "  - 积分服务: http://localhost:8082"
        echo "  - 产品服务: http://localhost:8083"
        echo ""
        echo "日志文件位置: $LOG_DIR"
        echo ""
        echo "查看日志命令:"
        echo "  tail -f $LOG_DIR/auth-service.log"
        echo "  tail -f $LOG_DIR/points-service.log"
        echo "  tail -f $LOG_DIR/product-service.log"
        echo "  tail -f $LOG_DIR/api-gateway.log"
        return 0
    else
        print_error "以下服务启动失败: ${failed_services[*]}"
        print_info "请查看日志文件排查问题: $LOG_DIR"
        return 1
    fi
}

# 执行主函数
main "$@"

