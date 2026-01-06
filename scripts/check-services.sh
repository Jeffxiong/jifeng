#!/bin/bash

# 积分系统服务状态检查脚本
# 功能：检查所有服务的运行状态和健康情况

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 服务配置
BACKEND_SERVICES=(
    "auth-service:8081:认证服务"
    "points-service:8082:积分服务"
    "product-service:8083:产品服务"
    "api-gateway:8080:API网关"
)

FRONTEND_SERVICES=(
    "admin-frontend:5174:管理后台"
    "jifeng-front:5173:用户前端"
)

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

print_header() {
    echo -e "${CYAN}$1${NC}"
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

# 检查HTTP服务健康状态
check_http_health() {
    local port=$1
    local service_name=$2
    
    # 尝试连接
    local http_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "http://localhost:$port" 2>/dev/null || echo "000")
    
    if [ "$http_code" = "000" ]; then
        return 1  # 连接失败
    elif [ "$http_code" -ge 200 ] && [ "$http_code" -lt 500 ]; then
        return 0  # 服务正常
    else
        return 2  # 服务异常
    fi
}

# 检查后端服务
check_backend_service() {
    local service_name=$1
    local port=$2
    local description=$3
    
    printf "%-20s %-15s" "$description" "端口 $port"
    
    if check_port $port; then
        # 检查HTTP健康状态
        if check_http_health $port "$service_name"; then
            print_success "运行中"
            
            # 尝试获取进程信息
            local pid=$(lsof -ti :$port 2>/dev/null | head -1)
            if [ -n "$pid" ]; then
                local cpu=$(ps -p $pid -o %cpu= 2>/dev/null | tr -d ' ' || echo "N/A")
                local mem=$(ps -p $pid -o %mem= 2>/dev/null | tr -d ' ' || echo "N/A")
                echo -e "  (PID: $pid, CPU: ${cpu}%, MEM: ${mem}%)"
            else
                echo ""
            fi
        else
            print_warning "端口占用但服务异常"
            echo ""
        fi
    else
        print_error "未运行"
        echo ""
    fi
}

# 检查前端服务
check_frontend_service() {
    local service_name=$1
    local port=$2
    local description=$3
    
    printf "%-20s %-15s" "$description" "端口 $port"
    
    if check_port $port; then
        if check_http_health $port "$service_name"; then
            print_success "运行中"
            
            # 尝试获取进程信息
            local pid=$(lsof -ti :$port 2>/dev/null | head -1)
            if [ -n "$pid" ]; then
                local cpu=$(ps -p $pid -o %cpu= 2>/dev/null | tr -d ' ' || echo "N/A")
                local mem=$(ps -p $pid -o %mem= 2>/dev/null | tr -d ' ' || echo "N/A")
                echo -e "  (PID: $pid, CPU: ${cpu}%, MEM: ${mem}%)"
            else
                echo ""
            fi
        else
            print_warning "端口占用但服务异常"
            echo ""
        fi
    else
        print_error "未运行"
        echo ""
    fi
}

# 检查数据库连接
check_database() {
    print_header "\n数据库连接检查:"
    
    if command -v mysql &> /dev/null; then
        if mysql -u root -proot -e "SELECT 1;" > /dev/null 2>&1; then
            print_success "MySQL 连接正常"
            
            # 检查数据库是否存在
            if mysql -u root -proot -e "USE points_system;" > /dev/null 2>&1; then
                print_success "数据库 points_system 存在"
                
                # 检查表数量
                local table_count=$(mysql -u root -proot -e "USE points_system; SHOW TABLES;" 2>/dev/null | wc -l | tr -d ' ')
                table_count=$((table_count - 1))  # 减去标题行
                print_info "数据库表数量: $table_count"
            else
                print_error "数据库 points_system 不存在"
            fi
        else
            print_error "MySQL 连接失败"
        fi
    else
        print_warning "未找到 MySQL 客户端，跳过数据库检查"
    fi
}

# 检查API网关路由
check_api_gateway() {
    print_header "\nAPI网关路由检查:"
    
    if check_port 8080; then
        # 测试登录接口
        local response=$(curl -s -X POST http://localhost:8080/api/auth/login \
            -H "Content-Type: application/json" \
            -d '{"username":"test","password":"123456"}' \
            --connect-timeout 3 2>/dev/null || echo "")
        
        if [ -n "$response" ] && echo "$response" | grep -q "token\|登录成功"; then
            print_success "API网关路由正常"
        else
            print_warning "API网关路由可能异常"
        fi
    else
        print_error "API网关未运行"
    fi
}

# 主函数
main() {
    echo "=========================================="
    echo "  积分系统服务状态检查"
    echo "=========================================="
    echo ""
    
    # 检查后端服务
    print_header "后端服务状态:"
    echo "服务名称              端口状态        状态"
    echo "--------------------------------------------"
    
    for service_config in "${BACKEND_SERVICES[@]}"; do
        IFS=':' read -r service_name port description <<< "$service_config"
        check_backend_service "$service_name" "$port" "$description"
    done
    
    # 检查前端服务
    print_header "\n前端服务状态:"
    echo "服务名称              端口状态        状态"
    echo "--------------------------------------------"
    
    for service_config in "${FRONTEND_SERVICES[@]}"; do
        IFS=':' read -r service_name port description <<< "$service_config"
        check_frontend_service "$service_name" "$port" "$description"
    done
    
    # 检查数据库
    check_database
    
    # 检查API网关
    check_api_gateway
    
    # 总结
    echo ""
    echo "=========================================="
    print_info "检查完成"
    echo "=========================================="
    echo ""
    echo "快速操作:"
    echo "  启动所有服务: ./scripts/start-all.sh"
    echo "  停止所有服务: ./scripts/stop-all.sh"
    echo "  查看日志: tail -f /tmp/points-system/*.log"
}

# 执行主函数
main "$@"


