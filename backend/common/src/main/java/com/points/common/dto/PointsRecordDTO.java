package com.points.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 积分记录DTO
 * 使用String类型ID（UUID）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsRecordDTO {
    private String id;
    private LocalDateTime date;
    private String type; // "earn" | "spend"
    private Integer points;
    private String description;
    private Integer balance;
    private String details;
}
