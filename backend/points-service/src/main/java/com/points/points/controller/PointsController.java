package com.points.points.controller;

import com.points.common.dto.ApiResponse;
import com.points.common.dto.EarnPointsRequest;
import com.points.common.dto.ExchangeRecordDTO;
import com.points.common.dto.ExchangeRequest;
import com.points.common.dto.PointsRecordDTO;
import com.points.common.dto.SpendPointsRequest;
import com.points.common.util.JwtUtil;
import com.points.points.service.PointsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 积分控制器
 * 使用String类型ID（UUID）
 */
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;
    private final JwtUtil jwtUtil;

    /**
     * 获取当前积分余额
     */
    @GetMapping("/balance")
    public ApiResponse<Integer> getBalance(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ApiResponse.error(401, "需要认证");
            }

            String actualToken = token.replace("Bearer ", "");
            
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }

            String userId = jwtUtil.getUserIdFromToken(actualToken);
            Integer balance = pointsService.getBalance(userId);
            return ApiResponse.success(balance);
        } catch (Exception e) {
            return ApiResponse.error(401, "Token无效");
        }
    }

    /**
     * 获取积分明细
     */
    @GetMapping(value = "/records", produces = "application/json;charset=UTF-8")
    public ApiResponse<List<PointsRecordDTO>> getRecords(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false, defaultValue = "30days") String timeRange) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ApiResponse.error(401, "需要认证");
            }

            String actualToken = token.replace("Bearer ", "");
            
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }

            String userId = jwtUtil.getUserIdFromToken(actualToken);
            List<PointsRecordDTO> records = pointsService.getRecords(userId, type, timeRange);
            return ApiResponse.success(records);
        } catch (Exception e) {
            return ApiResponse.error(401, "Token无效");
        }
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/send-sms-code")
    public ApiResponse<String> sendSmsCode(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ApiResponse.error(401, "需要认证");
            }

            String actualToken = token.replace("Bearer ", "");
            
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }

            String userId = jwtUtil.getUserIdFromToken(actualToken);
            String code = pointsService.sendSmsCode(userId);
            // 开发环境返回验证码，生产环境不应返回
            return ApiResponse.success("验证码已发送", code);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(401, "Token无效");
        }
    }

    /**
     * 兑换产品
     */
    @PostMapping("/exchange")
    public ApiResponse<Void> exchange(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ExchangeRequest request) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ApiResponse.error(401, "需要认证");
            }

            String actualToken = token.replace("Bearer ", "");
            
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }

            String userId = jwtUtil.getUserIdFromToken(actualToken);
            pointsService.exchange(userId, request);
            return ApiResponse.success("兑换成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(401, "Token无效");
        }
    }

    /**
     * 获取积分（供其他模块调用）
     * 安全修复：必须通过JWT Token认证，且只能操作自己的积分
     */
    @PostMapping("/earn")
    public ApiResponse<Integer> earnPoints(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody EarnPointsRequest request) {
        try {
            // 必须提供有效的 token
            if (token == null || !token.startsWith("Bearer ")) {
                return ApiResponse.error(401, "需要认证");
            }

            String actualToken = token.replace("Bearer ", "");
            
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }

            // 从 token 获取用户ID，忽略请求体中的 userId（防止越权）
            String userId = jwtUtil.getUserIdFromToken(actualToken);
            
            // 验证积分数量
            if (request.getPoints() == null || request.getPoints() <= 0) {
                return ApiResponse.error("积分数量必须大于0");
            }
            
            Integer balance = pointsService.earnPoints(
                    userId,
                    request.getPoints(),
                    request.getDescription(),
                    request.getDetails()
            );
            return ApiResponse.success(balance);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(401, "Token无效");
        }
    }

    /**
     * 消费积分（供其他模块调用）
     * 安全修复：必须通过JWT Token认证，且只能操作自己的积分
     */
    @PostMapping("/spend")
    public ApiResponse<Integer> spendPoints(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody SpendPointsRequest request) {
        try {
            // 必须提供有效的 token
            if (token == null || !token.startsWith("Bearer ")) {
                return ApiResponse.error(401, "需要认证");
            }

            String actualToken = token.replace("Bearer ", "");
            
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }

            // 从 token 获取用户ID，忽略请求体中的 userId（防止越权）
            String userId = jwtUtil.getUserIdFromToken(actualToken);
            
            // 验证积分数量
            if (request.getPoints() == null || request.getPoints() <= 0) {
                return ApiResponse.error("积分数量必须大于0");
            }
            
            Integer balance = pointsService.spendPoints(
                    userId,
                    request.getPoints(),
                    request.getDescription(),
                    request.getDetails()
            );
            return ApiResponse.success(balance);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(401, "Token无效");
        }
    }

    /**
     * 获取所有兑换记录（管理后台）
     * 安全修复：必须验证管理员权限
     */
    @GetMapping("/admin/exchanges")
    public ApiResponse<List<ExchangeRecordDTO>> getAllExchanges(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String status) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ApiResponse.error(401, "需要认证");
            }

            String actualToken = token.replace("Bearer ", "");
            
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }

            // 验证管理员权限
            String role = jwtUtil.getRoleFromToken(actualToken);
            if (role == null || !"admin".equals(role)) {
                return ApiResponse.error(403, "需要管理员权限");
            }

            List<ExchangeRecordDTO> records = pointsService.getAllExchangeRecords(userId, productId, status);
            return ApiResponse.success(records);
        } catch (Exception e) {
            return ApiResponse.error(401, "Token无效");
        }
    }
}
