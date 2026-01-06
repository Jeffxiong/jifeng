package com.points.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 产品DTO
 * 使用String类型ID（UUID）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private String id;
    private String name;
    private Integer points;
    private String description;
    private Integer stock;
    private String image;
    private Integer monthlyLimit;
    private Integer usedThisMonth;
    private Integer status; // 0-下架 1-上架
}
