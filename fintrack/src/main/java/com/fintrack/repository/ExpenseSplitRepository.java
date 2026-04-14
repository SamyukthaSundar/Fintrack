package com.fintrack.repository;

import com.fintrack.model.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ExpenseSplitRepository
 * Owner: Samyuktha S
 * Updated: Balance Calculation Fix — Now supports partial settlements via settledAmount
 */
@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    List<ExpenseSplit> findByExpenseId(Long expenseId);

    List<ExpenseSplit> findByUserId(Long userId);

    @Query("""
        SELECT s FROM ExpenseSplit s
        JOIN FETCH s.user
        WHERE s.expense.id = :expenseId
        """)
    List<ExpenseSplit> findByExpenseIdWithUser(@Param("expenseId") Long expenseId);

    /**
     * Find all splits where user owes money (expense paid by someone else).
     * Returns splits with remainingAmount > 0 from active groups.
     * Uses settledAmount to calculate remaining: amount - settledAmount.
     */
    @Query("""
        SELECT s FROM ExpenseSplit s
        JOIN FETCH s.expense e
        JOIN FETCH e.group g
        JOIN FETCH e.paidBy payer
        WHERE s.user.id = :userId
          AND e.paidBy.id != :userId
          AND g.isActive = true
          AND (s.settledAmount IS NULL OR s.amount > s.settledAmount)
        ORDER BY e.expenseDate DESC
        """)
    List<ExpenseSplit> findSplitsOwedByUser(@Param("userId") Long userId);

    /**
     * Find all splits where others owe money to user (user paid the expense).
     * Returns splits with remainingAmount > 0 from active groups.
     * Uses settledAmount to calculate remaining: amount - settledAmount.
     */
    @Query("""
        SELECT s FROM ExpenseSplit s
        JOIN FETCH s.expense e
        JOIN FETCH e.group g
        JOIN FETCH s.user debtor
        WHERE e.paidBy.id = :userId
          AND s.user.id != :userId
          AND g.isActive = true
          AND (s.settledAmount IS NULL OR s.amount > s.settledAmount)
        ORDER BY e.expenseDate DESC
        """)
    List<ExpenseSplit> findSplitsOwedToUser(@Param("userId") Long userId);

    /**
     * Find splits in a specific group where user owes money.
     */
    @Query("""
        SELECT s FROM ExpenseSplit s
        JOIN FETCH s.expense e
        JOIN FETCH e.paidBy payer
        WHERE s.user.id = :userId
          AND e.paidBy.id != :userId
          AND e.group.id = :groupId
          AND (s.settledAmount IS NULL OR s.amount > s.settledAmount)
        ORDER BY e.expenseDate DESC
        """)
    List<ExpenseSplit> findSplitsOwedByUserInGroup(@Param("userId") Long userId, 
                                                    @Param("groupId") Long groupId);

    /**
     * Find splits in a specific group where others owe user money.
     */
    @Query("""
        SELECT s FROM ExpenseSplit s
        JOIN FETCH s.expense e
        JOIN FETCH s.user debtor
        WHERE e.paidBy.id = :userId
          AND s.user.id != :userId
          AND e.group.id = :groupId
          AND (s.settledAmount IS NULL OR s.amount > s.settledAmount)
        ORDER BY e.expenseDate DESC
        """)
    List<ExpenseSplit> findSplitsOwedToUserInGroup(@Param("userId") Long userId, 
                                                    @Param("groupId") Long groupId);

    /**
     * Find unpaid splits between two specific users in a group.
     * Used when recording settlements to apply against specific debts.
     */
    @Query("""
        SELECT s FROM ExpenseSplit s
        JOIN FETCH s.expense e
        WHERE s.user.id = :payerId
          AND e.paidBy.id = :payeeId
          AND e.group.id = :groupId
          AND (s.settledAmount IS NULL OR s.amount > s.settledAmount)
        ORDER BY e.expenseDate ASC
        """)
    List<ExpenseSplit> findUnpaidSplitsBetweenUsers(@Param("payerId") Long payerId,
                                                     @Param("payeeId") Long payeeId,
                                                     @Param("groupId") Long groupId);

    /**
     * Cleanup method: Delete all splits for expenses in a specific group.
     * Used when deleting a group to ensure no orphaned splits remain.
     */
    @Modifying
    @Query("""
        DELETE FROM ExpenseSplit s
        WHERE s.expense.id IN (SELECT e.id FROM Expense e WHERE e.group.id = :groupId)
        """)
    void deleteAllSplitsForGroup(@Param("groupId") Long groupId);
}
