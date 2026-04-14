package com.fintrack.service;

import com.fintrack.model.ExpenseSplit;
import com.fintrack.model.Group;
import com.fintrack.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * BalanceCalculationService — Centralized service for all balance calculations.
 * 
 * This is the SINGLE SOURCE OF TRUTH for all debt/balance calculations in the system.
 * All controllers and other services must use this for consistent calculations.
 * 
 * Owner: System Fix — Production Critical
 */
public interface BalanceCalculationService {

    /**
     * Record a verified settlement against expense splits.
     * This updates the settledAmount on splits and marks them paid if fully settled.
     * 
     * @param payerId The user who paid
     * @param payeeId The user who received payment
     * @param groupId The group where settlement occurred
     * @param amount The settlement amount
     * @return The amount actually applied (may be less if splits are already partially settled)
     */
    BigDecimal recordSettlement(Long payerId, Long payeeId, Long groupId, BigDecimal amount);

    /**
     * Calculate the remaining unpaid amount for a specific expense split.
     * This accounts for partial settlements.
     * 
     * @param split The expense split
     * @return The remaining unpaid amount
     */
    BigDecimal getRemainingAmount(ExpenseSplit split);

    /**
     * Get all unpaid expense splits for a user (what they owe to others).
     * Only returns splits with remainingAmount > 0 from active groups.
     * 
     * @param userId The user who owes money
     * @return List of unpaid splits with expense and group details
     */
    List<ExpenseSplit> getUnpaidSplitsForUser(Long userId);

    /**
     * Get all unpaid expense splits where others owe the user.
     * Only returns splits with remainingAmount > 0 from active groups.
     * 
     * @param userId The user who is owed money
     * @return List of unpaid splits with expense and group details
     */
    List<ExpenseSplit> getUnpaidSplitsOwedToUser(Long userId);

    /**
     * Get total amount owed by a user (what they need to pay to others).
     * 
     * @param userId The user
     * @return Total outstanding amount
     */
    BigDecimal getTotalOwedByUser(Long userId);

    /**
     * Get total amount owed to a user (what others need to pay them).
     * 
     * @param userId The user
     * @return Total outstanding amount
     */
    BigDecimal getTotalOwedToUser(Long userId);

    /**
     * Get net balance for a user (positive = owed to them, negative = they owe).
     * 
     * @param userId The user
     * @return Net balance
     */
    BigDecimal getNetBalance(Long userId);

    /**
     * Calculate simplified debts for a group (net balances after settlements).
     * This is used for the settlement page to show optimal payment paths.
     * 
     * @param groupId The group
     * @return Map of user -> net balance in the group
     */
    Map<User, BigDecimal> calculateGroupNetBalances(Long groupId);

    /**
     * Get all outstanding debts for a user across all active groups.
     * Each debt includes the split, expense, group, and remaining amount.
     * 
     * @param userId The user
     * @return List of outstanding debts with full details
     */
    List<OutstandingDebt> getOutstandingDebtsForUser(Long userId);

    /**
     * Data transfer object for outstanding debt information.
     */
    record OutstandingDebt(
        ExpenseSplit split,
        String expenseTitle,
        Group group,
        User otherPerson,  // payee if OWE, payer if OWED
        String type,       // "OWE" or "OWED"
        BigDecimal originalAmount,
        BigDecimal settledAmount,
        BigDecimal remainingAmount
    ) {}
}
