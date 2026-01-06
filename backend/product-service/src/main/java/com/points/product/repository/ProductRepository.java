package com.points.product.repository;

import com.points.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByStatusOrderByCreatedAtDesc(Integer status);
    
    List<Product> findAllByOrderByCreatedAtDesc();
}
