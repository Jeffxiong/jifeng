package com.points.points.repository;

import com.points.points.entity.ExchangeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExchangeRecordRepository extends JpaRepository<ExchangeRecord, String> {
    
    @Query("SELECT COUNT(e) FROM ExchangeRecord e " +
           "WHERE e.userId = :userId " +
           "AND e.productId = :productId " +
           "AND e.status = 'completed' " +
           "AND YEAR(e.createdAt) = YEAR(:date) " +
           "AND MONTH(e.createdAt) = MONTH(:date)")
    Integer countMonthlyExchanges(@Param("userId") String userId, 
                                   @Param("productId") String productId,
                                   @Param("date") LocalDateTime date);
    
    List<ExchangeRecord> findByUserIdOrderByCreatedAtDesc(String userId);
    
    // 管理后台查询
    List<ExchangeRecord> findAllByOrderByCreatedAtDesc();
    
    List<ExchangeRecord> findByProductIdOrderByCreatedAtDesc(String productId);
    
    List<ExchangeRecord> findByStatusOrderByCreatedAtDesc(String status);
}
