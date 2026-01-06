package com.points.points.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 短信验证码服务
 * 
 * 注意：当前实现使用内存存储验证码，生产环境应使用Redis
 * 短信发送功能需要集成真实的短信服务提供商（如阿里云、腾讯云等）
 */
@Slf4j
@Service
public class SmsService {

    @Value("${verification.expiration:300}")
    private int expirationSeconds;

    // 验证码存储（key: phone, value: {code, expireTime}）
    private final ConcurrentMap<String, CodeInfo> codeStorage = new ConcurrentHashMap<>();

    /**
     * 发送短信验证码
     * 
     * @param phone 手机号
     * @return 验证码（仅开发环境返回，生产环境不应返回）
     */
    public String sendVerificationCode(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }

        // 生成6位数字验证码
        String code = generateCode();
        long expireTime = System.currentTimeMillis() + expirationSeconds * 1000L;

        // 存储验证码
        codeStorage.put(phone, new CodeInfo(code, expireTime));

        // TODO: 集成真实的短信服务发送验证码
        // 示例：调用阿里云、腾讯云等短信服务API
        log.info("发送短信验证码到手机号: {}, 验证码: {} (有效期: {}秒)", phone, code, expirationSeconds);
        
        // 开发环境返回验证码，生产环境不应返回
        // 生产环境应该只返回成功/失败，不返回验证码
        return code;
    }

    /**
     * 验证短信验证码
     * 
     * @param phone 手机号
     * @param code 验证码
     * @return 验证结果，包含是否通过和错误信息
     */
    public VerificationResult verifyCode(String phone, String code) {
        if (phone == null || code == null) {
            log.warn("验证码验证失败：手机号或验证码为空，phone={}, code={}", phone, code);
            return new VerificationResult(false, "验证码不能为空");
        }

        // 去除空格
        phone = phone.trim();
        code = code.trim();

        if (code.isEmpty()) {
            log.warn("验证码为空");
            return new VerificationResult(false, "请输入验证码");
        }

        CodeInfo codeInfo = codeStorage.get(phone);
        if (codeInfo == null) {
            log.warn("手机号 {} 的验证码不存在或已过期，当前存储的验证码数量: {}", phone, codeStorage.size());
            return new VerificationResult(false, "验证码不存在或已过期，请重新发送验证码");
        }

        // 检查是否过期
        if (System.currentTimeMillis() > codeInfo.expireTime) {
            codeStorage.remove(phone);
            log.warn("手机号 {} 的验证码已过期，过期时间: {}", phone, codeInfo.expireTime);
            return new VerificationResult(false, "验证码已过期，请重新发送验证码");
        }

        // 验证码匹配（去除前后空格后比较）
        if (codeInfo.code.equals(code)) {
            // 验证成功后删除验证码（一次性使用）
            codeStorage.remove(phone);
            log.info("手机号 {} 验证码验证成功，验证码: {}", phone, code);
            return new VerificationResult(true, null);
        }

        log.warn("手机号 {} 验证码错误，输入: {}，存储: {}", phone, code, codeInfo.code);
        return new VerificationResult(false, "验证码错误，请检查后重新输入");
    }

    /**
     * 验证结果
     */
    public static class VerificationResult {
        private final boolean success;
        private final String errorMessage;

        public VerificationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * 验证码信息
     */
    private static class CodeInfo {
        final String code;
        final long expireTime;

        CodeInfo(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }
}

