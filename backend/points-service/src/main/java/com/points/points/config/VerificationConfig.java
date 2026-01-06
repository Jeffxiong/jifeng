package com.points.points.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 验证码配置
 * 
 * 支持两种模式：
 * 1. simple：简单验证模式，用于开发和测试环境
 * 2. sms：短信验证模式，用于生产环境
 * 
 * 安全说明：
 * - 生产环境必须使用短信验证模式
 * - 测试验证码仅在开发环境有效
 */
@Configuration
public class VerificationConfig {

    /**
     * 验证模式：simple 或 sms
     */
    @Value("${verification.mode:simple}")
    private String mode;

    /**
     * 开发环境测试验证码
     * 生产环境此值应为空
     */
    @Value("${verification.test-code:}")
    private String testCode;

    /**
     * 验证码有效期（秒）
     */
    @Value("${verification.expiration:300}")
    private int expiration;

    /**
     * 是否使用简单验证模式
     */
    public boolean isSimpleMode() {
        return "simple".equalsIgnoreCase(mode);
    }

    /**
     * 是否使用短信验证模式
     */
    public boolean isSmsMode() {
        return "sms".equalsIgnoreCase(mode);
    }

    /**
     * 获取测试验证码（仅开发环境有效）
     */
    public String getTestCode() {
        return testCode;
    }

    /**
     * 获取验证码有效期
     */
    public int getExpiration() {
        return expiration;
    }

    /**
     * 验证验证码
     * 
     * @param inputCode 用户输入的验证码
     * @param storedCode 存储的验证码（从Redis或其他存储获取）
     * @return 验证是否通过
     */
    public boolean verify(String inputCode, String storedCode) {
        if (inputCode == null || inputCode.trim().isEmpty()) {
            return false;
        }

        // 简单模式：使用测试验证码
        if (isSimpleMode() && testCode != null && !testCode.isEmpty()) {
            return testCode.equals(inputCode);
        }

        // 短信模式：验证存储的验证码
        if (storedCode == null || storedCode.isEmpty()) {
            return false;
        }

        return storedCode.equals(inputCode);
    }
}

