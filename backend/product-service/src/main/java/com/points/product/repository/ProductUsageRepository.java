package com.points.product.repository;

import com.points.product.entity.ProductUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductUsageRepository extends JpaRepository<ProductUsage, String> {
    
    @Query("SELECT pu FROM ProductUsage pu " +
           "WHERE pu.userId = :userId " +
           "AND pu.productId = :productId " +
           "AND pu.year = :year " +
           "AND pu.month = :month")
    Optional<ProductUsage> findByUserAndProductAndMonth(
            @Param("userId") String userId,
            @Param("productId") String productId,
            @Param("year") Integer year,
            @Param("month") Integer month);
}
