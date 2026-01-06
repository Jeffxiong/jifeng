# 积分系统管理后台

基于 Vue 3 + TypeScript + Element Plus 的后台管理系统

## 功能特性

- ✅ 用户登录认证
- ✅ 用户积分兑换记录查询
- ✅ 产品管理（查询、编辑、上下架、库存管理）

## 技术栈

- Vue 3
- TypeScript
- Vue Router
- Pinia
- Element Plus
- Axios
- Vite

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问地址：http://localhost:5174

### 构建生产版本

```bash
npm run build
```

## 项目结构

```
admin-frontend/
├── src/
│   ├── api/          # API接口
│   ├── views/        # 页面组件
│   ├── layouts/      # 布局组件
│   ├── router/       # 路由配置
│   ├── stores/       # Pinia状态管理
│   └── main.ts       # 入口文件
├── index.html
├── package.json
└── vite.config.ts
```

## 功能说明

### 1. 登录页面
- 用户名/密码登录
- 自动保存token
- 登录状态管理

### 2. 兑换记录查询
- 支持按用户ID、产品ID、状态筛选
- 显示兑换详情（数量、积分、状态、优惠券码等）
- 实时查询

### 3. 产品管理
- 查看所有产品（包括下架产品）
- 编辑产品信息
- 实时更新库存
- 产品上下架管理

## API接口

### 认证接口
- `POST /api/auth/login` - 登录

### 兑换记录接口
- `GET /api/points/admin/exchanges` - 获取所有兑换记录

### 产品管理接口
- `GET /api/products/admin/all` - 获取所有产品
- `PUT /api/products/admin/{id}` - 更新产品
- `PUT /api/products/admin/{id}/stock` - 更新库存
- `PUT /api/products/admin/{id}/status` - 更新状态

## 测试账号

- 用户名: `test`
- 密码: `123456`

