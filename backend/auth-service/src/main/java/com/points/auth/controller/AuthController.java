package com.points.auth.controller;

import com.points.common.dto.ApiResponse;
import com.points.common.dto.LoginRequest;
import com.points.common.dto.LoginResponse;
import com.points.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping(value = "/login", produces = "application/json;charset=UTF-8")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ApiResponse.success("登录成功", response);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 验证Token
     */
    @GetMapping("/validate")
    public ApiResponse<String> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String actualToken = token.replace("Bearer ", "");
            String userId = authService.validateToken(actualToken);
            return ApiResponse.success(userId);
        } catch (RuntimeException e) {
            return ApiResponse.error(401, e.getMessage());
        }
    }

    /**
     * 获取用户信息（供其他服务调用）
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<UserInfo> getUserInfo(@PathVariable String userId) {
        try {
            UserInfo userInfo = authService.getUserInfo(userId);
            if (userInfo == null) {
                return ApiResponse.error(404, "用户不存在");
            }
            return ApiResponse.success(userInfo);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取用户信息失败: " + e.getMessage());
        }
    }

    public static class UserInfo {
        private String id;
        private String username;
        private String nickname;
        private String phone;

        public UserInfo() {}

        public UserInfo(String id, String username, String nickname, String phone) {
            this.id = id;
            this.username = username;
            this.nickname = nickname;
            this.phone = phone;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}

