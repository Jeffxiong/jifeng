package com.points.points.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 积分记录实体
 * 使用UUID作为主键，提高安全性和分布式支持
 */
@Entity
@Table(name = "points_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsRecord {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false)
    private String type; // "earn" | "spend"

    @Column(nullable = false)
    private Integer points; // 正数表示获得，负数表示消耗

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer balance; // 操作后的余额

    @Column(columnDefinition = "TEXT")
    private String details; // 详细信息

    @Column(name = "related_id", length = 36)
    private String relatedId; // 关联的业务ID（如订单ID、兑换ID等）

    @Column(name = "related_type", length = 50)
    private String relatedType; // 关联的业务类型

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
    }
}
