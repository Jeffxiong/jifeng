package com.points.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security配置
 * 
 * 安全配置说明：
 * 1. CSRF：REST API 使用无状态 JWT 认证，不需要 CSRF 保护
 * 2. CORS：限制允许的来源，生产环境必须配置具体域名
 * 3. 会话管理：使用无状态会话策略
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 允许的前端域名列表
     * 从环境变量或配置文件读取，支持多个域名用逗号分隔
     */
    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // REST API 使用 JWT 无状态认证，禁用 CSRF
            .csrf(csrf -> csrf.disable())
            // 配置 CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 无状态会话
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 请求授权配置
            .authorizeHttpRequests(auth -> auth
                // 公开接口：登录、注册、刷新token、获取用户信息（供服务间调用）
                .requestMatchers("/api/auth/**").permitAll()
                // 其他接口需要认证
                .anyRequest().authenticated()
            )
            // 禁用默认的认证要求（因为我们已经配置了permitAll）
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 配置
     * 
     * 安全说明：
     * - 生产环境必须配置具体的前端域名
     * - 不允许使用通配符 "*"
     * - 仅允许必要的 HTTP 方法
     * - 仅允许必要的请求头
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 解析允许的来源列表（创建可变列表）
        List<String> origins = new java.util.ArrayList<>(Arrays.asList(allowedOrigins.split(",")));
        // 添加服务间调用的来源（localhost）
        origins.add("http://localhost:8080");
        origins.add("http://localhost:8081");
        origins.add("http://localhost:8082");
        origins.add("http://localhost:8083");
        configuration.setAllowedOrigins(origins);
        
        // 仅允许必要的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 仅允许必要的请求头
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
        ));
        
        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        
        // 允许携带凭证（cookies、HTTP认证等）
        configuration.setAllowCredentials(true);
        
        // 预检请求缓存时间（秒）
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
