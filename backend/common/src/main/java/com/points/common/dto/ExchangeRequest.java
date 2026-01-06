package com.points.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 兑换请求DTO
 * 使用String类型产品ID（UUID）
 */
@Data
public class ExchangeRequest {
    @NotNull(message = "产品ID不能为空")
    private String productId;

    @NotNull(message = "兑换数量不能为空")
    @Min(value = 1, message = "兑换数量必须大于0")
    private Integer quantity;

    @NotNull(message = "验证码不能为空")
    private String verificationCode;
}
