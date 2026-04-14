package com.fintrack.service.impl;

import com.fintrack.model.ExpenseSplit;
import com.fintrack.model.Group;
import com.fintrack.model.GroupMember;
import com.fintrack.model.User;
import com.fintrack.repository.ExpenseSplitRepository;
import com.fintrack.repository.GroupMemberRepository;
import com.fintrack.repository.SettlementRepository;
import com.fintrack.service.BalanceCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * BalanceCalculationServiceImpl — Centralized implementation for all balance calculations.
 * 
 * This is the SINGLE SOURCE OF TRUTH for:
 * 1. Recording settlements against expense splits
 * 2. Calculating remaining unpaid amounts
 * 3. Computing net balances for users and groups
 * 
 * All balance calculations go through this service to ensure consistency.
 * 
 * Owner: System Fix — Production Critical
 */
@Service
@Transactional
public class BalanceCalculationServiceImpl implements BalanceCalculationService {

    private static final Logger log = LoggerFactory.getLogger(BalanceCalculationServiceImpl.class);

    private final ExpenseSplitRepository expenseSplitRepository;
    private final SettlementRepository settlementRepository;
    private final GroupMemberRepository groupMemberRepository;

    public BalanceCalculationServiceImpl(ExpenseSplitRepository expenseSplitRepository,
                                         SettlementRepository settlementRepository,
                                         GroupMemberRepository groupMemberRepository) {
        this.expenseSplitRepository = expenseSplitRepository;
        this.settlementRepository = settlementRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Override
    @Transactional
    public BigDecimal recordSettlement(Long payerId, Long payeeId, Long groupId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Find all unpaid splits where payer owes payee
        List<ExpenseSplit> splits = expenseSplitRepository.findUnpaidSplitsBetweenUsers(payerId, payeeId, groupId);
        
        BigDecimal remainingToApply = amount;
        BigDecimal totalApplied = BigDecimal.ZERO;

        for (ExpenseSplit split : splits) {
            if (remainingToApply.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal applied = split.recordSettlement(remainingToApply);
            remainingToApply = remainingToApply.subtract(applied);
            totalApplied = totalApplied.add(applied);

            // Save the updated split
            expenseSplitRepository.save(split);
            
            log.debug("Applied ₹{} to split {} (remaining: ₹{})", 
                applied, split.getId(), split.getRemainingAmount());
        }

        log.info("Settlement recorded: payer={}, payee={}, group={}, amount={}, applied={}",
            payerId, payeeId, groupId, amount, totalApplied);

        return totalApplied;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRemainingAmount(ExpenseSplit split) {
        if (split == null) return BigDecimal.ZERO;
        return split.getRemainingAmount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseSplit> getUnpaidSplitsForUser(Long userId) {
        // Get all splits for user where they owe money (expense paid by someone else)
        List<ExpenseSplit> splits = expenseSplitRepository.findSplitsOwedByUser(userId);
        
        // Filter to only those with remaining amount > 0
        return splits.stream()
            .filter(s -> s.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseSplit> getUnpaidSplitsOwedToUser(Long userId) {
        // Get all splits where user is the payer and others owe them
        List<ExpenseSplit> splits = expenseSplitRepository.findSplitsOwedToUser(userId);
        
        // Filter to only those with remaining amount > 0
        return splits.stream()
            .filter(s -> s.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalOwedByUser(Long userId) {
        return getUnpaidSplitsForUser(userId).stream()
            .map(ExpenseSplit::getRemainingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalOwedToUser(Long userId) {
        return getUnpaidSplitsOwedToUser(userId).stream()
            .map(ExpenseSplit::getRemainingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getNetBalance(Long userId) {
        return getTotalOwedToUser(userId).subtract(getTotalOwedByUser(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<User, BigDecimal> calculateGroupNetBalances(Long groupId) {
        List<GroupMember> members = groupMemberRepository.findMembersWithUsers(groupId);
        Map<User, BigDecimal> netBalances = new LinkedHashMap<>();

        for (GroupMember member : members) {
            User user = member.getUser();
            
            // Amount user paid for others
            BigDecimal paidForOthers = expenseSplitRepository
                .findSplitsOwedToUserInGroup(user.getId(), groupId)
                .stream()
                .map(ExpenseSplit::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Amount user owes to others
            BigDecimal owesToOthers = expenseSplitRepository
                .findSplitsOwedByUserInGroup(user.getId(), groupId)
                .stream()
                .map(ExpenseSplit::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Net: positive = owed to them, negative = they owe
            BigDecimal net = paidForOthers.subtract(owesToOthers);
            netBalances.put(user, net.setScale(2, RoundingMode.HALF_UP));
        }

        return netBalances;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutstandingDebt> getOutstandingDebtsForUser(Long userId) {
        List<OutstandingDebt> debts = new ArrayList<>();

        // Debts user owes (type = OWE)
        for (ExpenseSplit split : getUnpaidSplitsForUser(userId)) {
            debts.add(new OutstandingDebt(
                split,
                split.getExpense().getTitle(),
                split.getExpense().getGroup(),
                split.getExpense().getPaidBy(),  // person who paid
                "OWE",
                split.getAmount(),
                split.getSettledAmount(),
                split.getRemainingAmount()
            ));
        }

        // Debts owed to user (type = OWED)
        for (ExpenseSplit split : getUnpaidSplitsOwedToUser(userId)) {
            debts.add(new OutstandingDebt(
                split,
                split.getExpense().getTitle(),
                split.getExpense().getGroup(),
                split.getUser(),  // person who owes
                "OWED",
                split.getAmount(),
                split.getSettledAmount(),
                split.getRemainingAmount()
            ));
        }

        // Sort by remaining amount descending (largest debts first)
        debts.sort((a, b) -> b.remainingAmount().compareTo(a.remainingAmount()));

        return debts;
    }
}
