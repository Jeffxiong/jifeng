package com.points.product.service;

import com.points.common.dto.ProductDTO;
import com.points.product.entity.Product;
import com.points.product.entity.ProductUsage;
import com.points.product.repository.ProductRepository;
import com.points.product.repository.ProductUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 产品服务
 * 使用String类型ID（UUID）
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductUsageRepository usageRepository;

    /**
     * 获取所有上架产品
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts(String userId) {
        List<Product> products = productRepository.findByStatusOrderByCreatedAtDesc(1);
        return products.stream()
                .map(product -> convertToDTO(product, userId))
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取产品
     */
    @Transactional(readOnly = true)
    public ProductDTO getProductById(String productId, String userId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty() || productOpt.get().getStatus() == 0) {
            return null;
        }
        return convertToDTO(productOpt.get(), userId);
    }

    /**
     * 更新产品使用次数
     */
    @Transactional
    public void updateProductUsage(String productId, Integer quantity, String userId) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        Optional<ProductUsage> usageOpt = usageRepository.findByUserAndProductAndMonth(
                userId, productId, year, month);

        ProductUsage usage;
        if (usageOpt.isPresent()) {
            usage = usageOpt.get();
            usage.setCount(usage.getCount() + quantity);
        } else {
            usage = new ProductUsage();
            usage.setUserId(userId);
            usage.setProductId(productId);
            usage.setYear(year);
            usage.setMonth(month);
            usage.setCount(quantity);
        }
        usageRepository.save(usage);

        // 更新产品库存
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
        }
    }

    private ProductDTO convertToDTO(Product product, String userId) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPoints(product.getPoints());
        dto.setDescription(product.getDescription());
        // 确保 stock 不为 null，如果为 null 则设置为 0
        dto.setStock(product.getStock() != null ? product.getStock() : 0);
        dto.setImage(product.getImage());
        // 确保 monthlyLimit 不为 null，如果为 null 则设置为 0（无限制）
        dto.setMonthlyLimit(product.getMonthlyLimit() != null ? product.getMonthlyLimit() : 0);
        // 设置状态，确保不为 null，默认为 1（上架）
        dto.setStatus(product.getStatus() != null ? product.getStatus() : 1);

        // 计算本月已使用次数
        if (userId != null) {
            LocalDateTime now = LocalDateTime.now();
            int year = now.getYear();
            int month = now.getMonthValue();
            
            Optional<ProductUsage> usageOpt = usageRepository.findByUserAndProductAndMonth(
                    userId, product.getId(), year, month);
            dto.setUsedThisMonth(usageOpt.map(ProductUsage::getCount).orElse(0));
        } else {
            dto.setUsedThisMonth(0);
        }

        return dto;
    }

    /**
     * 获取所有产品（管理后台，包括下架产品）
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProductsForAdmin() {
        List<Product> products = productRepository.findAllByOrderByCreatedAtDesc();
        return products.stream()
                .map(product -> convertToDTO(product, null))
                .collect(Collectors.toList());
    }

    /**
     * 更新产品（管理后台）
     */
    @Transactional
    public ProductDTO updateProduct(String productId, com.points.product.controller.ProductController.ProductUpdateRequest request) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("产品不存在");
        }
        
        Product product = productOpt.get();
        if (request.getName() != null) product.setName(request.getName());
        if (request.getPoints() != null) product.setPoints(request.getPoints());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getStock() != null) product.setStock(request.getStock());
        if (request.getImage() != null) product.setImage(request.getImage());
        if (request.getMonthlyLimit() != null) product.setMonthlyLimit(request.getMonthlyLimit());
        if (request.getStatus() != null) product.setStatus(request.getStatus());
        
        productRepository.save(product);
        return convertToDTO(product, null);
    }

    /**
     * 更新产品库存（管理后台）
     */
    @Transactional
    public ProductDTO updateProductStock(String productId, Integer stock) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("产品不存在");
        }
        
        Product product = productOpt.get();
        product.setStock(stock);
        productRepository.save(product);
        return convertToDTO(product, null);
    }

    /**
     * 更新产品状态（管理后台）
     */
    @Transactional
    public ProductDTO updateProductStatus(String productId, Integer status) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("产品不存在");
        }
        
        Product product = productOpt.get();
        product.setStatus(status);
        productRepository.save(product);
        return convertToDTO(product, null);
    }

    /**
     * 创建新产品（管理后台）
     */
    @Transactional
    public ProductDTO createProduct(com.points.product.controller.ProductController.ProductCreateRequest request) {
        // 验证必填字段
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("产品名称不能为空");
        }
        if (request.getPoints() == null || request.getPoints() <= 0) {
            throw new RuntimeException("所需积分必须大于0");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw new RuntimeException("库存不能为负数");
        }
        if (request.getMonthlyLimit() == null || request.getMonthlyLimit() < 0) {
            throw new RuntimeException("月度限制不能为负数");
        }

        // 创建新产品
        Product product = new Product();
        product.setName(request.getName().trim());
        product.setPoints(request.getPoints());
        product.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        product.setStock(request.getStock() != null ? request.getStock() : 0);
        product.setImage(request.getImage() != null ? request.getImage().trim() : "");
        product.setMonthlyLimit(request.getMonthlyLimit() != null ? request.getMonthlyLimit() : 0);
        product.setStatus(request.getStatus() != null ? request.getStatus() : 1); // 默认上架

        product = productRepository.save(product);
        return convertToDTO(product, null);
    }
}
