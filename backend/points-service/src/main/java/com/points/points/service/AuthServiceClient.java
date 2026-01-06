package com.points.points.service;

import com.points.common.dto.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 认证服务客户端
 * 用于获取用户信息（包括手机号）
 */
@Service
@RequiredArgsConstructor
public class AuthServiceClient {

    private final RestTemplate restTemplate;
    private static final String AUTH_SERVICE_URL = "http://localhost:8081";

    /**
     * 获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息（包含手机号、用户名等）
     */
    public UserInfo getUserInfo(String userId) {
        try {
            String url = AUTH_SERVICE_URL + "/api/auth/user/" + userId;
            ResponseEntity<ApiResponse<UserInfo>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<UserInfo>>() {}
            );
            ApiResponse<UserInfo> apiResponse = response.getBody();
            if (apiResponse != null && apiResponse.getCode() == 200 && apiResponse.getData() != null) {
                return apiResponse.getData();
            } else {
                System.err.println("获取用户信息失败: userId=" + userId + ", code=" + 
                    (apiResponse != null ? apiResponse.getCode() : "null") + ", message=" + 
                    (apiResponse != null ? apiResponse.getMessage() : "null"));
            }
            return null;
        } catch (Exception e) {
            // 记录详细错误信息
            System.err.println("获取用户信息异常: userId=" + userId + ", error=" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Data
    public static class UserInfo {
        private String id;
        private String username;
        private String nickname;
        private String phone;
    }
}

