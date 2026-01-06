-- 积分系统数据库初始化脚本（UUID版本）
-- 注意：此脚本会删除旧表并重新创建，请先备份数据！

-- 创建数据库
CREATE DATABASE IF NOT EXISTS points_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE points_system;

-- 删除旧表（如果存在）
DROP TABLE IF EXISTS product_usages;
DROP TABLE IF EXISTS exchange_records;
DROP TABLE IF EXISTS points_records;
DROP TABLE IF EXISTS points_accounts;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;

-- 用户表（UUID主键）
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY COMMENT 'UUID主键',
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    phone VARCHAR(20) COMMENT '手机号',
    status INT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT 'user-普通用户 admin-管理员',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_username (username),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 积分账户表（UUID主键）
CREATE TABLE IF NOT EXISTS points_accounts (
    id VARCHAR(36) PRIMARY KEY COMMENT 'UUID主键',
    user_id VARCHAR(36) NOT NULL UNIQUE COMMENT '关联用户UUID',
    balance INT NOT NULL DEFAULT 0 COMMENT '当前积分余额',
    total_earned INT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    total_spent INT NOT NULL DEFAULT 0 COMMENT '累计消耗积分',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 积分记录表（UUID主键）
CREATE TABLE IF NOT EXISTS points_records (
    id VARCHAR(36) PRIMARY KEY COMMENT 'UUID主键',
    user_id VARCHAR(36) NOT NULL COMMENT '关联用户UUID',
    type VARCHAR(10) NOT NULL COMMENT 'earn-获得 spend-消耗',
    points INT NOT NULL COMMENT '积分数量（正数表示获得，负数表示消耗）',
    description VARCHAR(255) NOT NULL,
    balance INT NOT NULL COMMENT '操作后的余额',
    details TEXT COMMENT '详细信息',
    related_id VARCHAR(36) COMMENT '关联的业务ID（UUID）',
    related_type VARCHAR(50) COMMENT '关联的业务类型',
    created_at DATETIME NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 产品表（UUID主键）
CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(36) PRIMARY KEY COMMENT 'UUID主键',
    name VARCHAR(100) NOT NULL,
    points INT NOT NULL COMMENT '所需积分',
    description TEXT,
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    image VARCHAR(500) COMMENT '图片URL',
    monthly_limit INT NOT NULL COMMENT '每月兑换限制',
    status INT NOT NULL DEFAULT 1 COMMENT '0-下架 1-上架',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 兑换记录表（UUID主键）
CREATE TABLE IF NOT EXISTS exchange_records (
    id VARCHAR(36) PRIMARY KEY COMMENT 'UUID主键',
    user_id VARCHAR(36) NOT NULL COMMENT '关联用户UUID',
    product_id VARCHAR(36) NOT NULL COMMENT '关联产品UUID',
    quantity INT NOT NULL,
    points INT NOT NULL COMMENT '消耗的积分',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending-待处理 completed-已完成 cancelled-已取消',
    coupon_code VARCHAR(100) COMMENT '优惠券码',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 产品使用记录表（UUID主键）
CREATE TABLE IF NOT EXISTS product_usages (
    id VARCHAR(36) PRIMARY KEY COMMENT 'UUID主键',
    user_id VARCHAR(36) NOT NULL COMMENT '关联用户UUID',
    product_id VARCHAR(36) NOT NULL COMMENT '关联产品UUID',
    year INT NOT NULL,
    month INT NOT NULL,
    count INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE KEY uk_user_product_month (user_id, product_id, year, month),
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试用户（密码：123456，已加密）
-- 使用新生成的BCrypt哈希，确保与当前Spring Security版本兼容
INSERT INTO users (id, username, password, nickname, status, role, created_at) VALUES
(UUID(), 'test', '$2a$10$1h8PXbS7S0g3i9qauJ5/k.21HOZQ.7uiM4J3Q9E2ClQbtXcFMsLoy', '测试用户', 1, 'admin', NOW());

-- 获取测试用户ID用于后续插入
SET @test_user_id = (SELECT id FROM users WHERE username = 'test');

-- 插入测试产品数据
INSERT INTO products (id, name, points, description, stock, image, monthly_limit, status, created_at) VALUES
(UUID(), '50元话费券', 500, '全国通用话费充值券', 100, 'https://images.unsplash.com/photo-1556656793-08538906a9f8?w=400', 2, 1, NOW()),
(UUID(), '100元话费券', 1000, '全国通用话费充值券', 50, 'https://images.unsplash.com/photo-1556656793-08538906a9f8?w=400', 1, 1, NOW()),
(UUID(), '20元出行优惠券', 200, '机票、火车票通用优惠券', 200, 'https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=400', 5, 1, NOW()),
(UUID(), '50元酒店优惠券', 500, '指定酒店通用优惠券', 150, 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=400', 3, 1, NOW()),
(UUID(), '10元商城代金券', 100, '全场通用代金券', 0, 'https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=400', 10, 1, NOW()),
(UUID(), '积分加倍卡', 800, '7天内获得积分翻倍', 30, 'https://images.unsplash.com/photo-1614680376593-902f74cf0d41?w=400', 1, 1, NOW()),
(UUID(), 'VIP会员月卡', 1500, '30天VIP会员权益', 80, 'https://images.unsplash.com/photo-1633158829585-23ba8f7c8caf?w=400', 1, 1, NOW()),
(UUID(), '签到双倍卡', 300, '7天内签到积分翻倍', 120, 'https://images.unsplash.com/photo-1606787366850-de6330128bfc?w=400', 4, 1, NOW());

-- 为测试用户创建积分账户并添加初始积分
INSERT INTO points_accounts (id, user_id, balance, total_earned, total_spent, created_at) VALUES
(UUID(), @test_user_id, 1250, 1250, 0, NOW());

-- 插入测试积分记录
INSERT INTO points_records (id, user_id, type, points, description, balance, details, created_at) VALUES
(UUID(), @test_user_id, 'earn', 50, '预订机票', 1250, '成功预订北京-上海航班，订单号：FLT20251228001，获得50积分', NOW() - INTERVAL 1 DAY),
(UUID(), @test_user_id, 'spend', -100, '兑换话费券', 1200, '兑换了价值100积分的50元话费券，券码：TEL12345678', NOW() - INTERVAL 2 DAY),
(UUID(), @test_user_id, 'earn', 200, '预订酒店', 1300, '成功预订希尔顿酒店2晚，订单号：HTL20251226002，获得200积分', NOW() - INTERVAL 3 DAY);

