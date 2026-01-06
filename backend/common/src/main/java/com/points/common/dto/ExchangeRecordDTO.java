package com.points.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 兑换记录DTO
 * 使用String类型ID（UUID）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRecordDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String userId;
    private String username;
    private String nickname;
    private String phone; // 用户手机号
    private String productId;
    private String productName;
    private Integer quantity;
    private Integer points;
    private String status;
    private String couponCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
