package com.fintrack.service;

import com.fintrack.model.Settlement;
import com.fintrack.model.Settlement.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * SettlementService Interface
 * Owner: Sanika Gupta (Settlement & Analytics Lead)
 */
public interface SettlementService {
    Settlement initiate(Long groupId, Long payerId, Long payeeId, BigDecimal amount, Long requestingUserId);
    Settlement submit(Long settlementId, String paymentRef, PaymentMethod method, String notes, Long requestingUserId);
    Settlement verify(Long settlementId, Long requestingUserId);
    Settlement reject(Long settlementId, String reason, Long requestingUserId);
    Optional<Settlement> findById(Long id);
    List<Settlement> findByGroupId(Long groupId);
    List<Settlement> findPendingForPayee(Long payeeId);
}
