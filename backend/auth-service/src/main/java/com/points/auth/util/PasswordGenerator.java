package com.points.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = encoder.encode("123456");
        System.out.println("Password hash for '123456':");
        System.out.println(password);
        System.out.println("\nSQL to update:");
        System.out.println("UPDATE users SET password = '" + password + "' WHERE username = 'test';");
        
        // 验证
        boolean matches = encoder.matches("123456", password);
        System.out.println("\nVerification: " + matches);
    }
}

