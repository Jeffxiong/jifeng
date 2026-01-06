package com.points.points.repository;

import com.points.points.entity.PointsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointsAccountRepository extends JpaRepository<PointsAccount, String> {
    Optional<PointsAccount> findByUserId(String userId);
}
