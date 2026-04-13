package com.fintrack.service.impl;

import com.fintrack.model.GroupMember;
import com.fintrack.model.User;
import com.fintrack.repository.ExpenseRepository;
import com.fintrack.repository.GroupMemberRepository;
import com.fintrack.repository.SettlementRepository;
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
 */
@Service
public class DebtSimplificationService {

    private static final Logger log = LoggerFactory.getLogger(DebtSimplificationService.class);

    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final GroupMemberRepository memberRepository;

    public DebtSimplificationService(ExpenseRepository expenseRepository,
                                     SettlementRepository settlementRepository,
                                     GroupMemberRepository memberRepository) {
        this.expenseRepository   = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.memberRepository    = memberRepository;
    }

    public record SimplifiedDebt(User payer, User payee, BigDecimal amount) {}

    @Transactional(readOnly = true)
    public List<SimplifiedDebt> computeSimplifiedDebts(Long groupId) {
        List<GroupMember> groupMembers = memberRepository.findMembersWithUsers(groupId);
        List<User> members = new ArrayList<>();
        for (GroupMember gm : groupMembers) members.add(gm.getUser());

        Map<Long, BigDecimal> netBalance = new HashMap<>();
        Map<Long, User> userMap = new HashMap<>();

        for (User member : members) {
            userMap.put(member.getId(), member);
            BigDecimal paid  = expenseRepository.sumPaidByUserInGroup(member.getId(), groupId);
            BigDecimal owed  = expenseRepository.sumOwedByUserInGroup(member.getId(), groupId);
            BigDecimal settledOut = BigDecimal.ZERO;
            BigDecimal settledIn  = BigDecimal.ZERO;
            for (User other : members) {
                if (!other.getId().equals(member.getId())) {
                    settledOut = settledOut.add(
                        settlementRepository.sumVerifiedPayments(groupId, member.getId(), other.getId()));
                    settledIn = settledIn.add(
                        settlementRepository.sumVerifiedPayments(groupId, other.getId(), member.getId()));
                }
            }
            BigDecimal net = paid.subtract(owed).add(settledIn).subtract(settledOut);
            netBalance.put(member.getId(), net.setScale(2, RoundingMode.HALF_UP));
            log.debug("Member {}: net={}", member.getUsername(), net);
        }

        // Greedy simplification
        PriorityQueue<long[]> creditors = new PriorityQueue<>(
                Comparator.comparingLong((long[] a) -> -a[1]));
        PriorityQueue<long[]> debtors = new PriorityQueue<>(
                Comparator.comparingLong((long[] a) -> a[1]));

        for (Map.Entry<Long, BigDecimal> entry : netBalance.entrySet()) {
            int cmp = entry.getValue().compareTo(BigDecimal.ZERO);
            if (cmp > 0) {
                creditors.add(new long[]{entry.getKey(),
                        entry.getValue().multiply(BigDecimal.valueOf(100)).longValue()});
            } else if (cmp < 0) {
                debtors.add(new long[]{entry.getKey(),
                        entry.getValue().multiply(BigDecimal.valueOf(100)).longValue()});
            }
        }

        List<SimplifiedDebt> result = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            long[] debtor   = debtors.poll();
            long[] creditor = creditors.poll();
            long debtAmt    = Math.abs(debtor[1]);
            long creditAmt  = creditor[1];
            long settled    = Math.min(debtAmt, creditAmt);

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
    public Map<Long, BigDecimal> computeNetBalances(Long groupId) {
        List<GroupMember> groupMembers = memberRepository.findMembersWithUsers(groupId);
        List<User> members = new ArrayList<>();
        for (GroupMember gm : groupMembers) members.add(gm.getUser());

        Map<Long, BigDecimal> netBalance = new LinkedHashMap<>();
        for (User member : members) {
            BigDecimal paid  = expenseRepository.sumPaidByUserInGroup(member.getId(), groupId);
            BigDecimal owed  = expenseRepository.sumOwedByUserInGroup(member.getId(), groupId);
            BigDecimal settledOut = BigDecimal.ZERO;
            BigDecimal settledIn  = BigDecimal.ZERO;
            for (User other : members) {
                if (!other.getId().equals(member.getId())) {
                    settledOut = settledOut.add(
                        settlementRepository.sumVerifiedPayments(groupId, member.getId(), other.getId()));
                    settledIn = settledIn.add(
                        settlementRepository.sumVerifiedPayments(groupId, other.getId(), member.getId()));
                }
            }
            netBalance.put(member.getId(),
                    paid.subtract(owed).add(settledIn).subtract(settledOut)
                        .setScale(2, RoundingMode.HALF_UP));
        }
        return netBalance;
    }
}
