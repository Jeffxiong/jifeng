# 积分系统启动脚本使用说明

本目录包含了一套完整的服务启动、停止和检查脚本，帮助您快速管理积分系统的所有服务。

## 脚本列表

| 脚本名称 | 功能说明 |
|---------|---------|
| `start-all.sh` | 启动所有后端和前端服务（推荐使用） |
| `start-backend.sh` | 仅启动后端服务（4个微服务） |
| `start-frontend.sh` | 仅启动前端服务（管理后台和用户前端） |
| `stop-all.sh` | 停止所有服务 |
| `check-services.sh` | 检查所有服务的运行状态和健康情况 |

## 快速开始

### 1. 启动所有服务（推荐）

```bash
# 在项目根目录执行
./scripts/start-all.sh
```

这个脚本会：
- ✅ 自动检查环境（Java、Maven、Node.js、npm）
- ✅ 停止已运行的服务
- ✅ 编译公共模块
- ✅ 按顺序启动所有后端服务
- ✅ 启动所有前端服务
- ✅ 进行服务健康检查
- ✅ 显示服务访问地址

### 2. 仅启动后端服务

```bash
./scripts/start-backend.sh
```

启动的服务：
- 认证服务 (auth-service) - 端口 8081
- 积分服务 (points-service) - 端口 8082
- 产品服务 (product-service) - 端口 8083
- API网关 (api-gateway) - 端口 8080

### 3. 仅启动前端服务

```bash
# 启动所有前端服务
./scripts/start-frontend.sh

# 或启动指定前端服务
./scripts/start-frontend.sh admin-frontend  # 仅启动管理后台
./scripts/start-frontend.sh jifeng-front    # 仅启动用户前端
```

启动的服务：
- 管理后台 (admin-frontend) - 端口 5174
- 用户前端 (jifeng-front) - 端口 5173

### 4. 检查服务状态

```bash
./scripts/check-services.sh
```

这个脚本会显示：
- ✅ 所有服务的运行状态
- ✅ 端口占用情况
- ✅ 进程信息（PID、CPU、内存）
- ✅ 数据库连接状态
- ✅ API网关路由测试

### 5. 停止所有服务

```bash
# 停止所有服务
./scripts/stop-all.sh

# 仅停止后端服务
./scripts/stop-all.sh backend

# 仅停止前端服务
./scripts/stop-all.sh frontend
```

## 脚本功能详解

### start-backend.sh - 后端启动脚本

**功能特性：**
- 🔍 环境检查（Java、Maven、MySQL）
- 🛑 自动停止已运行的服务
- 🔨 自动编译公共模块（common）
- 🚀 按正确顺序启动服务
- ✅ 服务健康检查
- 📊 启动状态报告

**启动顺序：**
1. auth-service (认证服务)
2. points-service (积分服务)
3. product-service (产品服务)
4. api-gateway (API网关)

**日志文件：**
- `/tmp/points-system/auth-service.log`
- `/tmp/points-system/points-service.log`
- `/tmp/points-system/product-service.log`
- `/tmp/points-system/api-gateway.log`
- `/tmp/points-system/common-build.log`

### start-frontend.sh - 前端启动脚本

**功能特性：**
- 🔍 环境检查（Node.js、npm）
- 🛑 自动停止已运行的服务
- 📦 自动检查并安装依赖
- 🚀 启动前端开发服务器
- ✅ 服务健康检查
- 📊 启动状态报告

**日志文件：**
- `/tmp/points-system/admin-frontend.log`
- `/tmp/points-system/jifeng-front.log`
- `/tmp/points-system/admin-frontend-install.log`
- `/tmp/points-system/jifeng-front-install.log`

### check-services.sh - 服务检查脚本

**检查内容：**
- ✅ 后端服务运行状态
- ✅ 前端服务运行状态
- ✅ 端口占用情况
- ✅ 进程资源使用（CPU、内存）
- ✅ 数据库连接状态
- ✅ API网关路由测试

**输出示例：**
```
后端服务状态:
服务名称              端口状态        状态
--------------------------------------------
认证服务           端口 8081      [✓] 运行中  (PID: 12345, CPU: 2.5%, MEM: 3.2%)
积分服务           端口 8082      [✓] 运行中  (PID: 12346, CPU: 1.8%, MEM: 2.9%)
...
```

### stop-all.sh - 服务停止脚本

**功能特性：**
- 🛑 停止指定端口的服务
- 🧹 清理Spring Boot进程
- 🧹 清理Vite进程
- 🗑️ 清理PID文件

## 日志管理

所有服务的日志文件都保存在 `/tmp/points-system/` 目录下。

### 查看实时日志

```bash
# 查看单个服务日志
tail -f /tmp/points-system/auth-service.log
tail -f /tmp/points-system/points-service.log
tail -f /tmp/points-system/product-service.log
tail -f /tmp/points-system/api-gateway.log
tail -f /tmp/points-system/admin-frontend.log
tail -f /tmp/points-system/jifeng-front.log

# 查看所有日志
tail -f /tmp/points-system/*.log
```

### 查看历史日志

```bash
# 查看最近100行日志
tail -n 100 /tmp/points-system/auth-service.log

# 搜索错误日志
grep -i error /tmp/points-system/*.log
```

## 常见问题

### 1. 脚本没有执行权限

```bash
chmod +x scripts/*.sh
```

### 2. 端口被占用

脚本会自动检测并停止占用端口的进程。如果自动停止失败，可以手动停止：

```bash
# 查看端口占用
lsof -i :8080

# 停止指定端口的进程
lsof -ti :8080 | xargs kill -9
```

### 3. 服务启动失败

1. 检查日志文件：`/tmp/points-system/*.log`
2. 检查环境是否正确安装（Java、Maven、Node.js）
3. 检查数据库是否运行
4. 检查配置文件是否正确

### 4. 数据库连接失败

确保：
- MySQL服务正在运行
- 数据库 `points_system` 已创建
- 数据库用户名和密码正确（默认：root/root）
- 已执行初始化脚本：`backend/database/init.sql`

### 5. 前端依赖安装失败

```bash
# 手动安装依赖
cd admin-frontend
npm install

cd ../jifeng-front
npm install
```

## 服务访问地址

启动成功后，可以通过以下地址访问服务：

### 后端服务
- **API网关**: http://localhost:8080
- **认证服务**: http://localhost:8081
- **积分服务**: http://localhost:8082
- **产品服务**: http://localhost:8083

### 前端服务
- **管理后台**: http://localhost:5174
- **用户前端**: http://localhost:5173

### 测试账号
- 用户名: `test`
- 密码: `123456`

## 开发建议

1. **开发环境**：使用 `start-all.sh` 一键启动所有服务
2. **调试单个服务**：使用 `start-backend.sh` 或 `start-frontend.sh` 单独启动
3. **检查问题**：使用 `check-services.sh` 快速诊断服务状态
4. **查看日志**：使用 `tail -f` 实时查看日志输出
5. **停止服务**：使用 `stop-all.sh` 安全停止所有服务

## 脚本维护

如果需要修改脚本：
1. 所有脚本都包含详细的注释
2. 使用统一的颜色输出格式
3. 错误处理完善
4. 日志记录完整

## 注意事项

⚠️ **重要提示：**
- 启动顺序很重要，后端服务必须按顺序启动
- 确保数据库已初始化
- 确保所有环境依赖已安装
- 生产环境请使用更严格的配置和安全措施


