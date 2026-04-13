package com.fintrack.service.impl;

import com.fintrack.model.*;
import com.fintrack.model.Settlement.PaymentMethod;
import com.fintrack.observer.FinTrackEvent;
import com.fintrack.repository.*;
import com.fintrack.service.SettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * SettlementServiceImpl — Owner: Sanika Gupta
 * State Pattern: delegates transitions to Settlement entity.
 */
@Service
@Transactional
public class SettlementServiceImpl implements SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementServiceImpl.class);

    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SettlementServiceImpl(SettlementRepository settlementRepository,
                                  GroupRepository groupRepository,
                                  UserRepository userRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.settlementRepository = settlementRepository;
        this.groupRepository      = groupRepository;
        this.userRepository       = userRepository;
        this.eventPublisher       = eventPublisher;
    }

    @Override
    public Settlement initiate(Long groupId, Long payerId, Long payeeId,
                               BigDecimal amount, Long requestingUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        User payer = userRepository.findById(payerId)
                .orElseThrow(() -> new IllegalArgumentException("Payer not found."));
        User payee = userRepository.findById(payeeId)
                .orElseThrow(() -> new IllegalArgumentException("Payee not found."));
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Settlement amount must be positive.");

        Settlement s = new Settlement();
        s.setGroup(group);
        s.setPayer(payer);
        s.setPayee(payee);
        s.setAmount(amount);
        s.setCurrency(group.getCurrency());
        s.setStatus(Settlement.SettlementStatus.PENDING);
        s = settlementRepository.save(s);

        eventPublisher.publishEvent(new FinTrackEvent(
                this, Notification.NotificationType.SETTLEMENT_REQUEST, payeeId,
                "Settlement Request from " + payer.getFullName(),
                payer.getFullName() + " wants to settle ₹" + amount + " with you.",
                s.getId(), "SETTLEMENT"));
        log.info("Settlement initiated: {} → {} ₹{}", payer.getUsername(), payee.getUsername(), amount);
        return s;
    }

    @Override
    public Settlement submit(Long settlementId, String paymentRef,
                             PaymentMethod method, String notes, Long requestingUserId) {
        Settlement s = findOrThrow(settlementId);
        if (!s.getPayer().getId().equals(requestingUserId))
            throw new SecurityException("Only the payer can submit payment proof.");
        s.submit(paymentRef, method, notes);
        s = settlementRepository.save(s);
        eventPublisher.publishEvent(new FinTrackEvent(
                this, Notification.NotificationType.SETTLEMENT_REQUEST, s.getPayee().getId(),
                "Payment Submitted — Please Verify",
                s.getPayer().getFullName() + " submitted payment of ₹" + s.getAmount() + ". Please verify.",
                s.getId(), "SETTLEMENT"));
        return s;
    }

    @Override
    public Settlement verify(Long settlementId, Long requestingUserId) {
        Settlement s = findOrThrow(settlementId);
        if (!s.getPayee().getId().equals(requestingUserId))
            throw new SecurityException("Only the payee can verify.");
        User verifier = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        s.verify(verifier);
        s = settlementRepository.save(s);
        eventPublisher.publishEvent(new FinTrackEvent(
                this, Notification.NotificationType.SETTLEMENT_VERIFIED, s.getPayer().getId(),
                "Payment Verified ✓",
                s.getPayee().getFullName() + " verified your payment of ₹" + s.getAmount() + ".",
                s.getId(), "SETTLEMENT"));
        return s;
    }

    @Override
    public Settlement reject(Long settlementId, String reason, Long requestingUserId) {
        Settlement s = findOrThrow(settlementId);
        if (!s.getPayee().getId().equals(requestingUserId))
            throw new SecurityException("Only the payee can reject.");
        User rejector = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        s.reject(rejector, reason);
        s = settlementRepository.save(s);
        eventPublisher.publishEvent(new FinTrackEvent(
                this, Notification.NotificationType.SETTLEMENT_REJECTED, s.getPayer().getId(),
                "Payment Rejected ✗",
                s.getPayee().getFullName() + " rejected your payment. Reason: " + reason,
                s.getId(), "SETTLEMENT"));
        return s;
    }

    @Override @Transactional(readOnly = true)
    public Optional<Settlement> findById(Long id) { return settlementRepository.findById(id); }

    @Override @Transactional(readOnly = true)
    public List<Settlement> findByGroupId(Long groupId) {
        return settlementRepository.findByGroupIdOrderByInitiatedAtDesc(groupId);
    }

    @Override @Transactional(readOnly = true)
    public List<Settlement> findPendingForPayee(Long payeeId) {
        return settlementRepository.findByPayeeIdAndStatus(payeeId, Settlement.SettlementStatus.SUBMITTED);
    }

    private Settlement findOrThrow(Long id) {
        return settlementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + id));
    }
}
