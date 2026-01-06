package com.points.points.service;

import com.points.common.dto.ApiResponse;
import com.points.common.dto.ProductDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 产品服务客户端（Feign或RestTemplate）
 * 使用String类型ID（UUID）
 */
@Service
@RequiredArgsConstructor
public class ProductServiceClient {

    private final RestTemplate restTemplate;
    private static final String PRODUCT_SERVICE_URL = "http://localhost:8083";

    public ProductDTO getProduct(String productId) {
        try {
            String url = PRODUCT_SERVICE_URL + "/api/products/" + productId;
            ResponseEntity<ApiResponse<ProductDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<ProductDTO>>() {}
            );
            ApiResponse<ProductDTO> apiResponse = response.getBody();
            if (apiResponse != null && apiResponse.getCode() == 200 && apiResponse.getData() != null) {
                return apiResponse.getData();
            }
            throw new RuntimeException("获取产品信息失败: 产品不存在或数据为空");
        } catch (Exception e) {
            throw new RuntimeException("获取产品信息失败: " + e.getMessage());
        }
    }

    public void updateProductUsage(String productId, Integer quantity, String userId) {
        try {
            String url = PRODUCT_SERVICE_URL + "/api/products/" + productId + "/usage";
            restTemplate.postForObject(url, new UsageRequest(quantity, userId), Void.class);
        } catch (Exception e) {
            // 记录日志，但不影响主流程
            System.err.println("更新产品使用次数失败: " + e.getMessage());
        }
    }

    public static class UsageRequest {
        private Integer quantity;
        private String userId;

        public UsageRequest(Integer quantity, String userId) {
            this.quantity = quantity;
            this.userId = userId;
        }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
