# API接口文档

## 基础信息

- 网关地址: http://localhost:8080
- 认证方式: Bearer Token (JWT)
- 请求格式: JSON
- 响应格式: JSON

## 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1704067200000
}
```

## 接口列表

### 1. 认证服务

#### 1.1 用户登录
- **URL**: `/api/auth/login`
- **Method**: `POST`
- **Auth**: 不需要
- **Request Body**:
```json
{
  "username": "test",
  "password": "123456"
}
```
- **Response**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400000,
    "userInfo": {
      "userId": 1,
      "username": "test",
      "nickname": "测试用户"
    }
  },
  "timestamp": 1704067200000
}
```

#### 1.2 验证Token
- **URL**: `/api/auth/validate`
- **Method**: `GET`
- **Auth**: 需要
- **Headers**: `Authorization: Bearer {token}`
- **Response**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1,
  "timestamp": 1704067200000
}
```

### 2. 积分服务

#### 2.1 获取当前积分余额
- **URL**: `/api/points/balance`
- **Method**: `GET`
- **Auth**: 需要
- **Headers**: `Authorization: Bearer {token}`
- **Response**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1250,
  "timestamp": 1704067200000
}
```

#### 2.2 获取积分明细
- **URL**: `/api/points/records`
- **Method**: `GET`
- **Auth**: 需要
- **Headers**: `Authorization: Bearer {token}`
- **Query Parameters**:
  - `type`: 类型筛选 (可选: `all`, `earned`, `spent`，默认: `all`)
  - `timeRange`: 时间范围 (可选: `30days`, `3months`, `12months`, `2years`，默认: `30days`)
- **Response**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "date": "2025-12-28T14:32:15",
      "type": "earn",
      "points": 50,
      "description": "预订机票",
      "balance": 1250,
      "details": "成功预订北京-上海航班，订单号：FLT20251228001，获得50积分"
    }
  ],
  "timestamp": 1704067200000
}
```

#### 2.3 兑换产品
- **URL**: `/api/points/exchange`
- **Method**: `POST`
- **Auth**: 需要
- **Headers**: `Authorization: Bearer {token}`
- **Request Body**:
```json
{
  "productId": 1,
  "quantity": 1,
  "verificationCode": "123456"
}
```
- **Response**:
```json
{
  "code": 200,
  "message": "兑换成功",
  "data": null,
  "timestamp": 1704067200000
}
```

#### 2.4 获取积分（供其他模块调用）
- **URL**: `/api/points/earn`
- **Method**: `POST`
- **Auth**: 可选（支持JWT Token认证或内部调用）
- **Headers**: `Authorization: Bearer {token}` (可选)
- **Request Body**:
```json
{
  "userId": 1,
  "points": 100,
  "description": "完成任务奖励",
  "details": "完成每日任务，获得100积分"
}
```
- **Response**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1350,
  "timestamp": 1704067200000
}
```
- **说明**:
  - `data` 字段返回操作后的积分余额
  - 如果提供了Token，会验证Token并确保userId一致
  - 如果不提供Token，允许内部服务直接调用（需要传递userId）
  - 会自动创建积分记录

#### 2.5 消费积分（供其他模块调用）
- **URL**: `/api/points/spend`
- **Method**: `POST`
- **Auth**: 可选（支持JWT Token认证或内部调用）
- **Headers**: `Authorization: Bearer {token}` (可选)
- **Request Body**:
```json
{
  "userId": 1,
  "points": 50,
  "description": "购买服务",
  "details": "使用50积分购买VIP服务"
}
```
- **Response**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1300,
  "timestamp": 1704067200000
}
```
- **错误响应**（积分不足）:
```json
{
  "code": 500,
  "message": "积分不足，当前余额：100，需要：200",
  "data": null,
  "timestamp": 1704067200000
}
```
- **说明**:
  - `data` 字段返回操作后的积分余额
  - 如果积分不足，会返回错误信息
  - 如果提供了Token，会验证Token并确保userId一致
  - 如果不提供Token，允许内部服务直接调用（需要传递userId）
  - 会自动创建积分记录

### 3. 产品服务

#### 3.1 获取产品列表
- **URL**: `/api/products`
- **Method**: `GET`
- **Auth**: 可选（如果提供token，会显示用户本月已使用次数）
- **Headers**: `Authorization: Bearer {token}` (可选)
- **Response**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "50元话费券",
      "points": 500,
      "description": "全国通用话费充值券",
      "stock": 100,
      "image": "https://images.unsplash.com/photo-1556656793-08538906a9f8?w=400",
      "monthlyLimit": 2,
      "usedThisMonth": 0
    }
  ],
  "timestamp": 1704067200000
}
```

#### 3.2 获取产品详情
- **URL**: `/api/products/{id}`
- **Method**: `GET`
- **Auth**: 可选
- **Headers**: `Authorization: Bearer {token}` (可选)
- **Response**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "50元话费券",
    "points": 500,
    "description": "全国通用话费充值券",
    "stock": 100,
    "image": "https://images.unsplash.com/photo-1556656793-08538906a9f8?w=400",
    "monthlyLimit": 2,
    "usedThisMonth": 0
  },
  "timestamp": 1704067200000
}
```

## 错误码说明

- `200`: 成功
- `401`: 未授权（Token无效或过期）
- `404`: 资源不存在
- `500`: 服务器错误

## 前端对接示例

### 使用fetch调用API

```typescript
// 登录
const login = async (username: string, password: string) => {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ username, password }),
  });
  const data = await response.json();
  if (data.code === 200) {
    localStorage.setItem('token', data.data.token);
    return data.data;
  }
  throw new Error(data.message);
};

// 获取积分余额
const getBalance = async () => {
  const token = localStorage.getItem('token');
  const response = await fetch('http://localhost:8080/api/points/balance', {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
  const data = await response.json();
  return data.data;
};

// 获取积分明细
const getRecords = async (type: string = 'all', timeRange: string = '30days') => {
  const token = localStorage.getItem('token');
  const response = await fetch(
    `http://localhost:8080/api/points/records?type=${type}&timeRange=${timeRange}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    }
  );
  const data = await response.json();
  return data.data;
};

// 获取积分（供其他模块调用）
const earnPoints = async (userId: number, points: number, description: string, details?: string) => {
  const response = await fetch('http://localhost:8080/api/points/earn', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      // Token可选，如果不提供则通过userId进行内部调用
      // 'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      userId,
      points,
      description,
      details: details || description
    })
  });
  const result = await response.json();
  if (result.code === 200) {
    console.log('获取积分成功，当前余额:', result.data);
    return result.data; // 返回操作后的积分余额
  } else {
    throw new Error(result.message);
  }
};

// 消费积分（供其他模块调用）
const spendPoints = async (userId: number, points: number, description: string, details?: string) => {
  const response = await fetch('http://localhost:8080/api/points/spend', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      // Token可选，如果不提供则通过userId进行内部调用
      // 'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      userId,
      points,
      description,
      details: details || description
    })
  });
  const result = await response.json();
  if (result.code === 200) {
    console.log('消费积分成功，当前余额:', result.data);
    return result.data; // 返回操作后的积分余额
  } else {
    throw new Error(result.message);
  }
};

// 兑换产品
const exchange = async (productId: number, quantity: number, verificationCode: string) => {
  const token = localStorage.getItem('token');
  const response = await fetch('http://localhost:8080/api/points/exchange', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({ productId, quantity, verificationCode }),
  });
  const data = await response.json();
  if (data.code !== 200) {
    throw new Error(data.message);
  }
  return data;
};

// 获取产品列表
const getProducts = async () => {
  const token = localStorage.getItem('token');
  const response = await fetch('http://localhost:8080/api/products', {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
  const data = await response.json();
  return data.data;
};
```

