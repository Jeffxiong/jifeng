# 积分系统 (Jifeng)

一个完整的积分管理和兑换系统，采用微服务架构，支持用户积分管理、产品兑换、短信验证等功能。

## 项目简介

本项目是一个基于 Spring Boot 微服务架构的积分系统，包含以下核心功能：

- 用户积分管理和查询
- 产品兑换功能（支持短信验证码验证）
- 产品管理（上下架、库存管理）
- 兑换记录查询
- 管理后台（Vue 3 + Element Plus）

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.0
- **数据库**: MySQL 8.0
- **认证**: JWT (JSON Web Token)
- **架构**: 微服务架构
  - API Gateway (端口: 8080)
  - Auth Service (端口: 8081)
  - Points Service (端口: 8082)
  - Product Service (端口: 8083)
- **构建工具**: Maven

### 前端
- **用户端**: React 18 + Vite + TypeScript + Tailwind CSS
- **管理端**: Vue 3 + TypeScript + Element Plus + Pinia

## 项目结构

```
.
├── backend/                 # 后端微服务
│   ├── api-gateway/        # API 网关
│   ├── auth-service/       # 认证服务
│   ├── points-service/     # 积分服务
│   ├── product-service/    # 产品服务
│   ├── common/             # 公共模块
│   └── database/           # 数据库脚本
├── jifeng-front/          # 用户端前端（React）
├── admin-frontend/        # 管理端前端（Vue）
└── scripts/               # 启动脚本
```

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8.0+
- npm 或 yarn

### 数据库初始化

1. 创建数据库：
```sql
CREATE DATABASE points_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行初始化脚本：
```bash
cd backend/database
mysql -u root -p points_system < init_uuid.sql
```

### 启动后端服务

#### 方式一：使用脚本（推荐）

```bash
# 启动所有服务
./scripts/start-all.sh

# 或分别启动
./scripts/start-backend.sh
```

#### 方式二：手动启动

```bash
# 1. 启动 API Gateway
cd backend/api-gateway
mvn spring-boot:run

# 2. 启动 Auth Service
cd backend/auth-service
mvn spring-boot:run

# 3. 启动 Points Service
cd backend/points-service
mvn spring-boot:run

# 4. 启动 Product Service
cd backend/product-service
mvn spring-boot:run
```

### 启动前端

#### 用户端（React）

```bash
cd jifeng-front
npm install
npm run dev
```

访问：http://localhost:5173

#### 管理端（Vue）

```bash
cd admin-frontend
npm install
npm run dev
```

访问：http://localhost:5174

## 功能特性

### 用户端功能
- ✅ 用户登录/注册
- ✅ 积分余额查询
- ✅ 积分明细查询
- ✅ 产品列表浏览
- ✅ 产品兑换（支持短信验证码）
- ✅ 兑换记录查询

### 管理端功能
- ✅ 管理员登录
- ✅ 用户积分兑换记录查询
- ✅ 产品管理（查询、新增、上下架、库存管理）

## 安全特性

- ✅ JWT 认证和授权
- ✅ 密码加密存储（BCrypt）
- ✅ 短信验证码验证
- ✅ CORS 配置
- ✅ SQL 注入防护
- ✅ XSS 防护
- ✅ UUID 主键（防止 ID 枚举攻击）

## 默认账号

### 用户端
- 用户名：`test`
- 密码：`123456`

### 管理端
- 用户名：`admin`
- 密码：`admin123`

## API 文档

详细的 API 文档请参考：
- [API 接口文档](backend/API接口文档.md)
- [前端对接示例](backend/前端对接示例.md)

## 配置说明

### 后端配置

各服务的配置文件位于 `backend/*/src/main/resources/application.yml`

主要配置项：
- 数据库连接
- JWT 密钥和过期时间
- 服务端口
- CORS 允许的源

### 前端配置

- 用户端：`jifeng-front/vite.config.ts`
- 管理端：`admin-frontend/vite.config.ts`

## 开发指南

### 代码规范
- 后端：遵循 Java 编码规范
- 前端：使用 ESLint 和 Prettier

### 提交规范
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具相关

## 常见问题

### 端口占用
如果遇到端口占用问题，可以：
1. 修改 `application.yml` 中的端口配置
2. 或使用 `scripts/stop-all.sh` 停止所有服务

### 数据库连接失败
检查：
1. MySQL 服务是否启动
2. 数据库用户名和密码是否正确
3. 数据库是否已创建

### Token 过期
默认 Token 有效期为 30 天，过期后需要重新登录。

## 许可证

MIT License

## 联系方式

如有问题或建议，请提交 Issue 或 Pull Request。
