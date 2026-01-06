# 积分系统后端服务

基于 Spring Boot 的微服务架构后端系统，提供积分管理、产品兑换、用户认证等功能。

## 项目结构

```
backend/
├── api-gateway/          # API网关服务（端口8080）
├── auth-service/         # 认证服务（端口8081）
├── points-service/       # 积分服务（端口8082）
├── product-service/      # 产品服务（端口8083）
├── common/              # 公共模块（DTO、工具类等）
└── database/            # 数据库初始化脚本
```

## 技术栈

- **Spring Boot 3.2.0** - 核心框架
- **Spring Cloud Gateway** - API网关
- **Spring Security + JWT** - 认证授权
- **Spring Data JPA** - 数据访问层
- **MySQL 8.0+** - 关系型数据库
- **Maven** - 项目构建工具
- **Jackson** - JSON序列化（支持UTF-8中文）

## 环境要求

- JDK 17 或更高版本
- Maven 3.6+
- MySQL 8.0+
- Node.js 18+ (前端开发)

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| API Gateway | 8080 | 统一入口，路由转发 |
| Auth Service | 8081 | 用户认证服务 |
| Points Service | 8082 | 积分管理服务 |
| Product Service | 8083 | 产品管理服务 |
| Frontend | 5173 | 前端开发服务器 |

## 快速开始

### 1. 数据库准备

#### 1.1 创建数据库
```sql
CREATE DATABASE points_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 1.2 初始化数据库
```bash
mysql -u root -proot points_system < backend/database/init.sql
```

**注意：** 默认数据库用户名为 `root`，密码为 `root`。如需修改，请更新各服务的 `application.yml` 配置文件。

### 2. 配置检查

确保以下配置文件中的数据库连接信息正确：

- `auth-service/src/main/resources/application.yml`
- `points-service/src/main/resources/application.yml`
- `product-service/src/main/resources/application.yml`

默认配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/points_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&connectionCollation=utf8mb4_unicode_ci
    username: root
    password: root
```

### 3. 编译公共模块

首先编译 common 模块（其他服务依赖它）：
```bash
cd backend/common
mvn clean install
```

### 4. 启动服务

**重要：** 必须按顺序启动服务！

#### 方式一：使用启动脚本（推荐）⭐

项目提供了自动化启动脚本，包含环境检查、服务自检等功能：

```bash
# 在项目根目录执行
# 启动所有后端和前端服务
./scripts/start-all.sh

# 或仅启动后端服务
./scripts/start-backend.sh

# 检查服务状态
./scripts/check-services.sh

# 停止所有服务
./scripts/stop-all.sh
```

**启动脚本功能：**
- ✅ 自动环境检查（Java、Maven、MySQL）
- ✅ 自动停止已运行的服务
- ✅ 自动编译公共模块
- ✅ 按正确顺序启动服务
- ✅ 服务健康检查
- ✅ 详细的启动状态报告

详细使用说明请查看：[scripts/README.md](../scripts/README.md)

#### 方式二：命令行启动（手动）

**终端1 - 启动认证服务**
```bash
cd backend/auth-service
mvn spring-boot:run
```

**终端2 - 启动积分服务**
```bash
cd backend/points-service
mvn spring-boot:run
```

**终端3 - 启动产品服务**
```bash
cd backend/product-service
mvn spring-boot:run
```

**终端4 - 启动API网关**
```bash
cd backend/api-gateway
mvn spring-boot:run
```

#### 方式三：后台启动

```bash
# 启动所有服务（后台运行）
cd backend/auth-service && mvn spring-boot:run > /tmp/auth-service.log 2>&1 &
sleep 5
cd backend/points-service && mvn spring-boot:run > /tmp/points-service.log 2>&1 &
sleep 5
cd backend/product-service && mvn spring-boot:run > /tmp/product-service.log 2>&1 &
sleep 8
cd backend/api-gateway && mvn spring-boot:run > /tmp/api-gateway.log 2>&1 &
```

### 5. 验证服务

检查服务是否启动成功：
```bash
# 检查端口占用
lsof -i :8080,8081,8082,8083

# 测试登录接口
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

## 测试账号

| 字段 | 值 |
|------|-----|
| 用户名 | `test` |
| 密码 | `123456` |

## API接口

详细的API接口文档请查看 [API接口文档](./API接口文档.md)

### 认证接口
- `POST /api/auth/login` - 用户登录
- `GET /api/auth/validate` - 验证token

### 积分接口
- `GET /api/points/balance` - 获取当前积分余额
- `GET /api/points/records` - 获取积分明细（支持type和timeRange参数）
- `POST /api/points/exchange` - 兑换产品

### 产品接口
- `GET /api/products` - 获取产品列表
- `GET /api/products/{id}` - 获取产品详情

**所有接口均通过API网关访问：** `http://localhost:8080/api/...`

## 前端访问

前端服务地址：http://localhost:5173

启动前端：
```bash
# 在项目根目录
npm run dev
```

## 中文显示支持

系统已配置UTF-8编码支持，确保：
1. ✅ 数据库字符集为 `utf8mb4`
2. ✅ JDBC连接字符串包含 `characterEncoding=utf8`
3. ✅ Spring Boot配置了UTF-8编码
4. ✅ Jackson JSON序列化支持中文

如果遇到中文乱码，请检查：
- 数据库表的字符集是否为 `utf8mb4`
- 数据库连接配置是否正确
- 前端请求头是否包含 `Accept: application/json;charset=UTF-8`

## 常见问题

### 1. 服务启动失败
- 检查MySQL是否运行：`mysql -u root -proot -e "SELECT 1;"`
- 检查端口是否被占用：`lsof -i :8080`
- 查看日志：`tail -f /tmp/*-service.log`

### 2. 数据库连接失败
- 确认数据库用户名和密码正确
- 确认数据库已创建：`mysql -u root -proot -e "SHOW DATABASES;"`
- 检查数据库字符集：`mysql -u root -proot -e "SHOW CREATE DATABASE points_system;"`

### 3. 中文显示乱码
- 确认数据库表字符集：`SHOW CREATE TABLE points_records;`
- 检查API响应头：`Content-Type: application/json;charset=UTF-8`
- 重新初始化数据库数据

## 相关文档

- [启动指南](./启动指南.md) - 详细的启动步骤
- [API接口文档](./API接口文档.md) - 完整的API接口说明
- [前端对接示例](./前端对接示例.md) - 前端集成示例代码

## 开发说明

### 项目特点
- ✅ 微服务架构，服务解耦
- ✅ JWT无状态认证
- ✅ RESTful API设计
- ✅ 统一异常处理
- ✅ UTF-8中文支持
- ✅ CORS跨域支持

### 日志位置
- Auth Service: `/tmp/points-system/auth-service.log`
- Points Service: `/tmp/points-system/points-service.log`
- Product Service: `/tmp/points-system/product-service.log`
- API Gateway: `/tmp/points-system/api-gateway.log`

### 停止服务

**使用脚本停止（推荐）：**
```bash
# 停止所有服务
./scripts/stop-all.sh

# 仅停止后端服务
./scripts/stop-all.sh backend
```

**手动停止：**
```bash
# 停止所有Spring Boot服务
pkill -f "spring-boot:run"
```

## License

MIT
