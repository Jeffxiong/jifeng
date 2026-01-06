package com.points.product.controller;

import com.points.common.dto.ApiResponse;
import com.points.common.dto.ProductDTO;
import com.points.common.util.JwtUtil;
import com.points.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品控制器
 * 使用String类型ID（UUID）
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final JwtUtil jwtUtil;

    /**
     * 获取产品列表
     */
    @GetMapping(produces = "application/json;charset=UTF-8")
    public ApiResponse<List<ProductDTO>> getProducts(@RequestHeader(value = "Authorization", required = false) String token) {
        String userId = null;
        if (token != null && token.startsWith("Bearer ")) {
            try {
                String actualToken = token.replace("Bearer ", "");
                userId = jwtUtil.getUserIdFromToken(actualToken);
            } catch (Exception e) {
                // 忽略token错误，允许未登录用户查看产品
            }
        }
        List<ProductDTO> products = productService.getAllProducts(userId);
        return ApiResponse.success(products);
    }

    /**
     * 获取产品详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ProductDTO> getProduct(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        String userId = null;
        if (token != null && token.startsWith("Bearer ")) {
            try {
                String actualToken = token.replace("Bearer ", "");
                userId = jwtUtil.getUserIdFromToken(actualToken);
            } catch (Exception e) {
                // 忽略token错误
            }
        }
        ProductDTO product = productService.getProductById(id, userId);
        if (product == null) {
            return ApiResponse.error(404, "产品不存在");
        }
        return ApiResponse.success(product);
    }

    /**
     * 更新产品使用次数（内部接口）
     * 安全修复：添加认证，仅允许内部服务调用
     */
    @PostMapping("/{id}/usage")
    public ApiResponse<Void> updateUsage(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String id,
            @RequestBody UsageRequest request) {
        // 验证内部服务调用（简化处理，生产环境应使用服务间认证）
        if (token == null || !token.startsWith("Bearer ")) {
            return ApiResponse.error(401, "需要认证");
        }

        try {
            String actualToken = token.replace("Bearer ", "");
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }
            
            productService.updateProductUsage(id, request.getQuantity(), request.getUserId());
            return ApiResponse.success("更新成功", null);
        } catch (Exception e) {
            return ApiResponse.error(401, "Token无效");
        }
    }

    /**
     * 获取所有产品（管理后台，包括下架产品）
     * 安全修复：必须验证管理员权限
     */
    @GetMapping("/admin/all")
    public ApiResponse<List<ProductDTO>> getAllProductsForAdmin(
            @RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ApiResponse.error(401, "需要认证");
            }

            String actualToken = token.replace("Bearer ", "");
            
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }

            // 验证管理员权限（在 try-catch 中处理可能的异常）
            String role;
            try {
                role = jwtUtil.getRoleFromToken(actualToken);
            } catch (Exception e) {
                // 如果获取角色时出错，说明 token 可能有问题
                return ApiResponse.error(401, "Token无效或已过期");
            }
            
            if (role == null || !"admin".equals(role)) {
                return ApiResponse.error(403, "需要管理员权限");
            }

            List<ProductDTO> products = productService.getAllProductsForAdmin();
            return ApiResponse.success(products);
        } catch (Exception e) {
            // 记录异常日志以便调试
            e.printStackTrace();
            return ApiResponse.error(401, "Token无效");
        }
    }

    /**
     * 更新产品（管理后台）
     * 安全修复：必须验证管理员权限
     */
    @PutMapping("/admin/{id}")
    public ApiResponse<ProductDTO> updateProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestBody ProductUpdateRequest request) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ApiResponse.error(401, "需要认证");
            }

            String actualToken = token.replace("Bearer ", "");
            
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }

            // 验证管理员权限
            String role = jwtUtil.getRoleFromToken(actualToken);
            if (role == null || !"admin".equals(role)) {
                return ApiResponse.error(403, "需要管理员权限");
            }

            ProductDTO product = productService.updateProduct(id, request);
            return ApiResponse.success(product);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(401, "Token无效");
        }
    }

    /**
     * 更新产品库存（管理后台）
     * 安全修复：必须验证管理员权限
     */
    @PutMapping("/admin/{id}/stock")
    public ApiResponse<ProductDTO> updateProductStock(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestBody StockUpdateRequest request) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ApiResponse.error(401, "需要认证");
            }

            String actualToken = token.replace("Bearer ", "");
            
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }

            // 验证管理员权限
            String role = jwtUtil.getRoleFromToken(actualToken);
            if (role == null || !"admin".equals(role)) {
                return ApiResponse.error(403, "需要管理员权限");
            }

            ProductDTO product = productService.updateProductStock(id, request.getStock());
            return ApiResponse.success(product);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(401, "Token无效");
        }
    }

    /**
     * 上下架产品（管理后台）
     * 安全修复：必须验证管理员权限
     */
    @PutMapping("/admin/{id}/status")
    public ApiResponse<ProductDTO> updateProductStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestBody StatusUpdateRequest request) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ApiResponse.error(401, "需要认证");
            }

            String actualToken = token.replace("Bearer ", "");
            
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }

            // 验证管理员权限
            String role = jwtUtil.getRoleFromToken(actualToken);
            if (role == null || !"admin".equals(role)) {
                return ApiResponse.error(403, "需要管理员权限");
            }

            ProductDTO product = productService.updateProductStatus(id, request.getStatus());
            return ApiResponse.success(product);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(401, "Token无效");
        }
    }

    /**
     * 创建新产品（管理后台）
     * 安全修复：必须验证管理员权限
     */
    @PostMapping("/admin")
    public ApiResponse<ProductDTO> createProduct(
            @RequestHeader("Authorization") String token,
            @RequestBody ProductCreateRequest request) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ApiResponse.error(401, "需要认证");
            }

            String actualToken = token.replace("Bearer ", "");
            
            // 验证 token 有效性
            if (!jwtUtil.validateToken(actualToken)) {
                return ApiResponse.error(401, "Token无效或已过期");
            }

            // 验证管理员权限
            String role = jwtUtil.getRoleFromToken(actualToken);
            if (role == null || !"admin".equals(role)) {
                return ApiResponse.error(403, "需要管理员权限");
            }

            ProductDTO product = productService.createProduct(request);
            return ApiResponse.success(product);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(401, "Token无效");
        }
    }

    public static class UsageRequest {
        private Integer quantity;
        private String userId;

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class ProductUpdateRequest {
        private String name;
        private Integer points;
        private String description;
        private Integer stock;
        private String image;
        private Integer monthlyLimit;
        private Integer status;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getPoints() { return points; }
        public void setPoints(Integer points) { this.points = points; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
        public Integer getMonthlyLimit() { return monthlyLimit; }
        public void setMonthlyLimit(Integer monthlyLimit) { this.monthlyLimit = monthlyLimit; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    public static class StockUpdateRequest {
        private Integer stock;

        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
    }

    public static class StatusUpdateRequest {
        private Integer status;

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    public static class ProductCreateRequest {
        private String name;
        private Integer points;
        private String description;
        private Integer stock;
        private String image;
        private Integer monthlyLimit;
        private Integer status;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getPoints() { return points; }
        public void setPoints(Integer points) { this.points = points; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
        public Integer getMonthlyLimit() { return monthlyLimit; }
        public void setMonthlyLimit(Integer monthlyLimit) { this.monthlyLimit = monthlyLimit; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}
