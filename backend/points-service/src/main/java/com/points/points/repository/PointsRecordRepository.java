package com.points.points.repository;

import com.points.points.entity.PointsRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointsRecordRepository extends JpaRepository<PointsRecord, String> {
    
    Page<PointsRecord> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    @Query("SELECT pr FROM PointsRecord pr WHERE pr.userId = :userId " +
           "AND (:type IS NULL OR pr.type = :type) " +
           "AND pr.createdAt >= :startTime " +
           "ORDER BY pr.createdAt DESC")
    Page<PointsRecord> findByUserIdAndTypeAndTimeRange(
            @Param("userId") String userId,
            @Param("type") String type,
            @Param("startTime") LocalDateTime startTime,
            Pageable pageable);
    
    List<PointsRecord> findByUserIdOrderByCreatedAtDesc(String userId);
}
