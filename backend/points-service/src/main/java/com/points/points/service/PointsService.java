package com.points.points.service;

import com.points.common.dto.ExchangeRequest;
import com.points.common.dto.PointsRecordDTO;
import com.points.common.util.JwtUtil;
import com.points.points.config.VerificationConfig;
import com.points.points.entity.ExchangeRecord;
import com.points.points.entity.PointsAccount;
import com.points.points.entity.PointsRecord;
import com.points.points.repository.ExchangeRecordRepository;
import com.points.points.repository.PointsAccountRepository;
import com.points.points.repository.PointsRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 积分服务
 * 使用String类型ID（UUID）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsAccountRepository accountRepository;
    private final PointsRecordRepository recordRepository;
    private final ExchangeRecordRepository exchangeRecordRepository;
    private final JwtUtil jwtUtil;
    private final ProductServiceClient productServiceClient;
    private final VerificationConfig verificationConfig;
    private final SmsService smsService;
    private final AuthServiceClient authServiceClient;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取用户积分余额
     */
    @Transactional(readOnly = true)
    public Integer getBalance(String userId) {
        PointsAccount account = accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PointsAccount newAccount = new PointsAccount();
                    newAccount.setUserId(userId);
                    newAccount.setBalance(0);
                    newAccount.setTotalEarned(0);
                    newAccount.setTotalSpent(0);
                    return accountRepository.save(newAccount);
                });
        return account.getBalance();
    }

    /**
     * 获取积分明细
     */
    @Transactional(readOnly = true)
    public List<PointsRecordDTO> getRecords(String userId, String type, String timeRange) {
        LocalDateTime startTime = calculateStartTime(timeRange);
        String recordType = convertType(type);
        
        Pageable pageable = PageRequest.of(0, 100);
        Page<PointsRecord> records;
        
        if (recordType == null) {
            records = recordRepository.findByUserIdAndTypeAndTimeRange(userId, null, startTime, pageable);
        } else {
            records = recordRepository.findByUserIdAndTypeAndTimeRange(userId, recordType, startTime, pageable);
        }
        
        return records.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 兑换产品
     */
    @Transactional
    public void exchange(String userId, ExchangeRequest request) {
        // 获取用户信息（包括手机号）
        AuthServiceClient.UserInfo userInfo = authServiceClient.getUserInfo(userId);
        if (userInfo == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户是否绑定手机号
        if (userInfo.getPhone() == null || userInfo.getPhone().trim().isEmpty()) {
            throw new RuntimeException("请先绑定手机号");
        }

        // 验证短信验证码
        String phone = userInfo.getPhone() != null ? userInfo.getPhone().trim() : null;
        String verificationCode = request.getVerificationCode() != null ? request.getVerificationCode().trim() : null;
        
        SmsService.VerificationResult verificationResult = smsService.verifyCode(phone, verificationCode);
        if (!verificationResult.isSuccess()) {
            log.warn("用户 {} 手机号 {} 验证码验证失败，输入的验证码: {}，错误信息: {}", 
                    userId, phone, verificationCode, verificationResult.getErrorMessage());
            throw new RuntimeException(verificationResult.getErrorMessage());
        }

        // 获取产品信息
        var product = productServiceClient.getProduct(request.getProductId());
        if (product == null) {
            throw new RuntimeException("产品不存在");
        }

        // 检查月度兑换限制
        LocalDateTime now = LocalDateTime.now();
        Integer monthlyLimit = product.getMonthlyLimit();
        if (monthlyLimit != null && monthlyLimit > 0) {
            Integer monthlyCount = exchangeRecordRepository.countMonthlyExchanges(
                    userId, request.getProductId(), now);
            if (monthlyCount + request.getQuantity() > monthlyLimit) {
                throw new RuntimeException(String.format("超过月度兑换限制，本月已兑换 %d 次，限制 %d 次，剩余 %d 次", 
                    monthlyCount, monthlyLimit, Math.max(0, monthlyLimit - monthlyCount)));
            }
        }

        // 检查库存
        Integer stock = product.getStock();
        if (stock == null || stock < request.getQuantity()) {
            throw new RuntimeException(String.format("库存不足，当前库存：%d，需要：%d", 
                    stock != null ? stock : 0, request.getQuantity()));
        }

        // 计算所需积分
        Integer productPoints = product.getPoints();
        if (productPoints == null || productPoints <= 0) {
            throw new RuntimeException("产品积分配置错误");
        }
        Integer requiredPoints = productPoints * request.getQuantity();

        // 获取或创建积分账户
        PointsAccount account = accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PointsAccount newAccount = new PointsAccount();
                    newAccount.setUserId(userId);
                    newAccount.setBalance(0);
                    newAccount.setTotalEarned(0);
                    newAccount.setTotalSpent(0);
                    return accountRepository.save(newAccount);
                });

        // 检查积分余额
        if (account.getBalance() < requiredPoints) {
            throw new RuntimeException("积分不足");
        }

        // 扣除积分
        account.setBalance(account.getBalance() - requiredPoints);
        account.setTotalSpent(account.getTotalSpent() + requiredPoints);
        accountRepository.save(account);

        // 创建积分记录
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType("spend");
        record.setPoints(-requiredPoints);
        record.setDescription("兑换" + product.getName());
        record.setBalance(account.getBalance());
        record.setDetails(String.format("兑换了价值%d积分的%s，数量：%d", 
                requiredPoints, product.getName(), request.getQuantity()));
        record.setRelatedType("exchange");
        recordRepository.save(record);

        // 创建兑换记录
        ExchangeRecord exchangeRecord = new ExchangeRecord();
        exchangeRecord.setUserId(userId);
        exchangeRecord.setProductId(request.getProductId());
        exchangeRecord.setQuantity(request.getQuantity());
        exchangeRecord.setPoints(requiredPoints);
        exchangeRecord.setStatus("completed");
        exchangeRecord.setCouponCode(generateCouponCode());
        exchangeRecordRepository.save(exchangeRecord);

        // 通知产品服务更新库存和使用次数
        productServiceClient.updateProductUsage(request.getProductId(), request.getQuantity(), userId);
        
        log.info("用户 {} 成功兑换产品 {}，数量 {}，消耗积分 {}", 
                userId, product.getName(), request.getQuantity(), requiredPoints);
    }

    /**
     * 添加积分（用于测试或系统操作）
     */
    @Transactional
    public void addPoints(String userId, Integer points, String description, String details) {
        PointsAccount account = accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PointsAccount newAccount = new PointsAccount();
                    newAccount.setUserId(userId);
                    newAccount.setBalance(0);
                    newAccount.setTotalEarned(0);
                    newAccount.setTotalSpent(0);
                    return accountRepository.save(newAccount);
                });

        account.setBalance(account.getBalance() + points);
        account.setTotalEarned(account.getTotalEarned() + points);
        accountRepository.save(account);

        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType("earn");
        record.setPoints(points);
        record.setDescription(description);
        record.setBalance(account.getBalance());
        record.setDetails(details);
        recordRepository.save(record);
    }

    /**
     * 获取积分（供其他模块调用）
     * @param userId 用户ID
     * @param points 积分数量
     * @param description 描述
     * @param details 详情
     * @return 操作后的积分余额
     */
    @Transactional
    public Integer earnPoints(String userId, Integer points, String description, String details) {
        PointsAccount account = accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PointsAccount newAccount = new PointsAccount();
                    newAccount.setUserId(userId);
                    newAccount.setBalance(0);
                    newAccount.setTotalEarned(0);
                    newAccount.setTotalSpent(0);
                    return accountRepository.save(newAccount);
                });

        account.setBalance(account.getBalance() + points);
        account.setTotalEarned(account.getTotalEarned() + points);
        accountRepository.save(account);

        // 创建积分记录并保存到 points_records 表
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType("earn");
        record.setPoints(points);
        record.setDescription(description);
        record.setBalance(account.getBalance());
        record.setDetails(details != null ? details : description);
        recordRepository.save(record);

        log.info("用户 {} 获取积分 {}，描述：{}", userId, points, description);
        return account.getBalance();
    }

    /**
     * 消费积分（供其他模块调用）
     * @param userId 用户ID
     * @param points 积分数量
     * @param description 描述
     * @param details 详情
     * @return 操作后的积分余额
     * @throws RuntimeException 如果积分不足
     */
    @Transactional
    public Integer spendPoints(String userId, Integer points, String description, String details) {
        PointsAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("用户积分账户不存在"));

        if (account.getBalance() < points) {
            throw new RuntimeException("积分不足，当前余额：" + account.getBalance() + "，需要：" + points);
        }

        account.setBalance(account.getBalance() - points);
        account.setTotalSpent(account.getTotalSpent() + points);
        accountRepository.save(account);

        // 创建积分记录并保存到 points_records 表
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType("spend");
        record.setPoints(-points);
        record.setDescription(description);
        record.setBalance(account.getBalance());
        record.setDetails(details != null ? details : description);
        recordRepository.save(record);

        log.info("用户 {} 消费积分 {}，描述：{}", userId, points, description);
        return account.getBalance();
    }

    private LocalDateTime calculateStartTime(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        return switch (timeRange) {
            case "30days" -> now.minusDays(30);
            case "3months" -> now.minusMonths(3);
            case "12months" -> now.minusMonths(12);
            case "2years" -> now.minusYears(2);
            default -> now.minusYears(10);
        };
    }

    private String convertType(String type) {
        return switch (type) {
            case "earned" -> "earn";
            case "spent" -> "spend";
            default -> null;
        };
    }

    private PointsRecordDTO convertToDTO(PointsRecord record) {
        PointsRecordDTO dto = new PointsRecordDTO();
        dto.setId(record.getId());
        dto.setDate(record.getCreatedAt());
        dto.setType(record.getType());
        dto.setPoints(record.getPoints());
        dto.setDescription(record.getDescription());
        dto.setBalance(record.getBalance());
        dto.setDetails(record.getDetails());
        return dto;
    }

    private String generateCouponCode() {
        return "CPN" + System.currentTimeMillis();
    }

    /**
     * 发送短信验证码
     */
    @Transactional(readOnly = true)
    public String sendSmsCode(String userId) {
        // 获取用户信息（包括手机号）
        AuthServiceClient.UserInfo userInfo = authServiceClient.getUserInfo(userId);
        if (userInfo == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户是否绑定手机号
        if (userInfo.getPhone() == null || userInfo.getPhone().trim().isEmpty()) {
            throw new RuntimeException("请先绑定手机号");
        }

        // 发送短信验证码
        return smsService.sendVerificationCode(userInfo.getPhone());
    }

    /**
     * 获取所有兑换记录（管理后台）
     */
    @Transactional(readOnly = true)
    public List<com.points.common.dto.ExchangeRecordDTO> getAllExchangeRecords(String userId, String productId, String status) {
        List<ExchangeRecord> records;
        
        if (userId != null && productId != null) {
            records = exchangeRecordRepository.findAll().stream()
                    .filter(e -> e.getUserId().equals(userId) && e.getProductId().equals(productId))
                    .filter(e -> status == null || e.getStatus().equals(status))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
        } else if (userId != null) {
            records = exchangeRecordRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                    .filter(e -> status == null || e.getStatus().equals(status))
                    .collect(Collectors.toList());
        } else if (productId != null) {
            records = exchangeRecordRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                    .filter(e -> status == null || e.getStatus().equals(status))
                    .collect(Collectors.toList());
        } else if (status != null) {
            records = exchangeRecordRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            records = exchangeRecordRepository.findAllByOrderByCreatedAtDesc();
        }
        
        // 批量获取用户信息和产品信息（使用数据库JOIN查询，更高效）
        Map<String, UserInfo> userInfoMap = batchGetUserInfo(records.stream()
                .map(ExchangeRecord::getUserId)
                .distinct()
                .collect(Collectors.toList()));
        
        Map<String, String> productNameMap = batchGetProductNames(records.stream()
                .map(ExchangeRecord::getProductId)
                .distinct()
                .collect(Collectors.toList()));
        
        // 转换为DTO
        return records.stream()
                .map(record -> convertExchangeToDTO(record, userInfoMap, productNameMap))
                .collect(Collectors.toList());
    }
    
    /**
     * 批量获取用户信息（使用数据库查询，避免服务间调用）
     */
    private Map<String, UserInfo> batchGetUserInfo(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            String placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String sql = "SELECT id, username, nickname, phone FROM users WHERE id IN (" + placeholders + ")";
            
            Map<String, UserInfo> map = new HashMap<>();
            jdbcTemplate.query(sql, userIds.toArray(), (rs, rowNum) -> {
                UserInfo info = new UserInfo();
                info.id = rs.getString("id");
                info.username = rs.getString("username");
                info.nickname = rs.getString("nickname");
                info.phone = rs.getString("phone");
                map.put(info.id, info);
                return info;
            });
            return map;
        } catch (Exception e) {
            log.error("批量获取用户信息失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * 批量获取产品名称（使用数据库查询，避免服务间调用）
     */
    private Map<String, String> batchGetProductNames(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            String placeholders = productIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String sql = "SELECT id, name FROM products WHERE id IN (" + placeholders + ")";
            
            Map<String, String> map = new HashMap<>();
            jdbcTemplate.query(sql, productIds.toArray(), (rs, rowNum) -> {
                map.put(rs.getString("id"), rs.getString("name"));
                return null;
            });
            return map;
        } catch (Exception e) {
            log.error("批量获取产品名称失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * 用户信息内部类
     */
    private static class UserInfo {
        String id;
        String username;
        String nickname;
        String phone;
    }
    
    private com.points.common.dto.ExchangeRecordDTO convertExchangeToDTO(
            ExchangeRecord record, 
            Map<String, UserInfo> userInfoMap, 
            Map<String, String> productNameMap) {
        com.points.common.dto.ExchangeRecordDTO dto = new com.points.common.dto.ExchangeRecordDTO();
        dto.setId(record.getId());
        dto.setUserId(record.getUserId());
        dto.setProductId(record.getProductId());
        dto.setQuantity(record.getQuantity());
        dto.setPoints(record.getPoints());
        dto.setStatus(record.getStatus());
        dto.setCouponCode(record.getCouponCode());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        
        // 设置用户信息
        UserInfo userInfo = userInfoMap.get(record.getUserId());
        if (userInfo != null) {
            dto.setUsername(userInfo.username);
            dto.setNickname(userInfo.nickname != null ? userInfo.nickname : userInfo.username);
            dto.setPhone(userInfo.phone);
        }
        
        // 设置产品名称
        String productName = productNameMap.get(record.getProductId());
        if (productName != null) {
            dto.setProductName(productName);
        }
        
        return dto;
    }
}
