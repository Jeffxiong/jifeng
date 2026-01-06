package com.points.auth.service;

import com.points.auth.entity.User;
import com.points.auth.repository.UserRepository;
import com.points.common.dto.LoginRequest;
import com.points.common.dto.LoginResponse;
import com.points.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 认证服务
 * 使用String类型用户ID（UUID）
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户名或密码错误");
        }

        User user = userOpt.get();
        
        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String role = user.getRole() != null ? user.getRole() : "user";
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getNickname() != null ? user.getNickname() : user.getUsername()
        );

        return new LoginResponse(token, refreshToken, 2592000000L, userInfo);  // 30天
    }

    /**
     * 验证Token
     */
    public String validateToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("登录信息已过期");
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * 验证用户是否为管理员
     */
    @Transactional(readOnly = true)
    public boolean isAdmin(String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        return "admin".equals(user.getRole());
    }

    /**
     * 根据用户ID获取用户角色
     */
    @Transactional(readOnly = true)
    public String getUserRole(String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return "user";
        }
        User user = userOpt.get();
        return user.getRole() != null ? user.getRole() : "user";
    }

    /**
     * 获取用户信息（供其他服务调用）
     */
    @Transactional(readOnly = true)
    public com.points.auth.controller.AuthController.UserInfo getUserInfo(String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();
        return new com.points.auth.controller.AuthController.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getNickname() != null ? user.getNickname() : user.getUsername(),
                user.getPhone()
        );
    }
}
