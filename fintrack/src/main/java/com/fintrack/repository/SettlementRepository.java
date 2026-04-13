package com.fintrack.repository;

import com.fintrack.model.Settlement;
import com.fintrack.model.Settlement.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * SettlementRepository
 * Owner: Sanika Gupta (Settlement & Analytics Lead)
 */
@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByGroupIdOrderByInitiatedAtDesc(Long groupId);

    List<Settlement> findByPayerIdAndStatus(Long payerId, SettlementStatus status);

    List<Settlement> findByPayeeIdAndStatus(Long payeeId, SettlementStatus status);

    @Query("""
        SELECT s FROM Settlement s
        WHERE s.group.id = :groupId
          AND (s.payer.id = :userId OR s.payee.id = :userId)
        ORDER BY s.initiatedAt DESC
        """)
    List<Settlement> findByGroupIdAndUserId(@Param("groupId") Long groupId,
                                             @Param("userId") Long userId);

    @Query("""
        SELECT COALESCE(SUM(s.amount), 0) FROM Settlement s
        WHERE s.group.id = :groupId
          AND s.payer.id = :payerId
          AND s.payee.id = :payeeId
          AND s.status = 'VERIFIED'
        """)
    BigDecimal sumVerifiedPayments(@Param("groupId") Long groupId,
                                   @Param("payerId") Long payerId,
                                   @Param("payeeId") Long payeeId);

    // For analytics
    @Query("""
        SELECT s FROM Settlement s
        WHERE s.group.id = :groupId AND s.status = :status
        ORDER BY s.initiatedAt DESC
        """)
    List<Settlement> findByGroupIdAndStatus(@Param("groupId") Long groupId,
                                             @Param("status") SettlementStatus status);
}
