-- 为用户表添加手机号字段
-- 执行此脚本前请先备份数据库

USE points_system;

-- 添加手机号字段
ALTER TABLE users 
ADD COLUMN phone VARCHAR(20) COMMENT '手机号' AFTER nickname;

-- 添加手机号索引
ALTER TABLE users 
ADD INDEX idx_phone (phone);

-- 更新测试用户的手机号（可选）
-- UPDATE users SET phone = '13800138000' WHERE username = 'test';

