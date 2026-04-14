package com.fintrack.service.impl;

import com.fintrack.model.GroupMember;
import com.fintrack.model.User;
import com.fintrack.repository.ExpenseSplitRepository;
import com.fintrack.repository.GroupMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * DebtSimplificationService — Saanvi Kakkar's MAJOR Feature
 * Graph-based net-balance algorithm to minimize transaction count.
 * 
 * UPDATED: Now uses ExpenseSplitRepository with settledAmount for consistent calculations.
 * This ensures the Settlement page shows the same values as the Dashboard.
 */
@Service
public class DebtSimplificationService {

    private static final Logger log = LoggerFactory.getLogger(DebtSimplificationService.class);

    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupMemberRepository memberRepository;

    public DebtSimplificationService(ExpenseSplitRepository expenseSplitRepository,
                                     GroupMemberRepository memberRepository) {
        this.expenseSplitRepository = expenseSplitRepository;
        this.memberRepository = memberRepository;
    }

    public record SimplifiedDebt(User payer, User payee, BigDecimal amount) {}

    @Transactional(readOnly = true)
    public List<SimplifiedDebt> computeSimplifiedDebts(Long groupId) {
        // Get net balances using the same logic as BalanceCalculationService
        Map<User, BigDecimal> netBalances = calculateGroupNetBalances(groupId);

        // Greedy simplification
        PriorityQueue<long[]> creditors = new PriorityQueue<>(
                Comparator.comparingLong((long[] a) -> -a[1]));
        PriorityQueue<long[]> debtors = new PriorityQueue<>(
                Comparator.comparingLong((long[] a) -> a[1]));
        
        Map<Long, User> userMap = new HashMap<>();

        for (Map.Entry<User, BigDecimal> entry : netBalances.entrySet()) {
            User user = entry.getKey();
            BigDecimal balance = entry.getValue();
            userMap.put(user.getId(), user);
            
            int cmp = balance.compareTo(BigDecimal.ZERO);
            if (cmp > 0) {
                creditors.add(new long[]{user.getId(),
                        balance.multiply(BigDecimal.valueOf(100)).longValue()});
            } else if (cmp < 0) {
                debtors.add(new long[]{user.getId(),
                        balance.multiply(BigDecimal.valueOf(100)).longValue()});
            }
        }

        List<SimplifiedDebt> result = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            long[] debtor = debtors.poll();
            long[] creditor = creditors.poll();
            long debtAmt = Math.abs(debtor[1]);
            long creditAmt = creditor[1];
            long settled = Math.min(debtAmt, creditAmt);

            result.add(new SimplifiedDebt(
                    userMap.get(debtor[0]),
                    userMap.get(creditor[0]),
                    BigDecimal.valueOf(settled, 2)));

            long dr = debtAmt - settled;
            long cr = creditAmt - settled;
            if (dr > 0) debtors.add(new long[]{debtor[0], -dr});
            if (cr > 0) creditors.add(new long[]{creditor[0], cr});
        }

        log.info("Debt simplification for group {}: {} transactions needed", groupId, result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<User, BigDecimal> calculateGroupNetBalances(Long groupId) {
        List<GroupMember> members = memberRepository.findMembersWithUsers(groupId);
        Map<User, BigDecimal> netBalances = new LinkedHashMap<>();

        for (GroupMember member : members) {
            User user = member.getUser();
            
            // Amount user paid for others (others owe user) - uses settledAmount
            BigDecimal paidForOthers = expenseSplitRepository
                .findSplitsOwedToUserInGroup(user.getId(), groupId)
                .stream()
                .map(split -> split.getAmount().subtract(split.getSettledAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Amount user owes to others (user owes others) - uses settledAmount  
            BigDecimal owesToOthers = expenseSplitRepository
                .findSplitsOwedByUserInGroup(user.getId(), groupId)
                .stream()
                .map(split -> split.getAmount().subtract(split.getSettledAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Net: positive = owed to them, negative = they owe
            BigDecimal net = paidForOthers.subtract(owesToOthers);
            netBalances.put(user, net.setScale(2, RoundingMode.HALF_UP));
            
            log.debug("Member {}: paidForOthers={}, owesToOthers={}, net={}", 
                user.getUsername(), paidForOthers, owesToOthers, net);
        }

        return netBalances;
    }

    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> computeNetBalances(Long groupId) {
        Map<User, BigDecimal> userBalances = calculateGroupNetBalances(groupId);
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<User, BigDecimal> entry : userBalances.entrySet()) {
            result.put(entry.getKey().getId(), entry.getValue());
        }
        return result;
    }
}
