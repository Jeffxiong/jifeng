package com.points.points.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 积分账户实体
 * 使用UUID作为主键，提高安全性和分布式支持
 */
@Entity
@Table(name = "points_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsAccount {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    private String userId;

    @Column(nullable = false)
    private Integer balance; // 当前积分余额

    @Column(name = "total_earned", nullable = false)
    private Integer totalEarned; // 累计获得积分

    @Column(name = "total_spent", nullable = false)
    private Integer totalSpent; // 累计消耗积分

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (balance == null) balance = 0;
        if (totalEarned == null) totalEarned = 0;
        if (totalSpent == null) totalSpent = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
