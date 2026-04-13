package com.fintrack.repository;

import com.fintrack.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ExpenseRepository
 * Owner: Samyuktha S
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroupIdOrderByExpenseDateDesc(Long groupId);

    List<Expense> findByGroupIdAndPaidByIdOrderByExpenseDateDesc(Long groupId, Long userId);

    @Query("""
        SELECT e FROM Expense e
        WHERE e.group.id = :groupId
          AND e.expenseDate BETWEEN :from AND :to
        ORDER BY e.expenseDate DESC
        """)
    List<Expense> findByGroupIdAndDateRange(@Param("groupId") Long groupId,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(e.totalAmount), 0)
        FROM Expense e WHERE e.group.id = :groupId
        """)
    BigDecimal sumTotalByGroupId(@Param("groupId") Long groupId);

    @Query("""
        SELECT COALESCE(SUM(s.amount), 0) FROM ExpenseSplit s
        WHERE s.user.id = :userId AND s.expense.group.id = :groupId
        """)
    BigDecimal sumOwedByUserInGroup(@Param("userId") Long userId,
                                    @Param("groupId") Long groupId);

    @Query("""
        SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expense e
        WHERE e.paidBy.id = :userId AND e.group.id = :groupId
        """)
    BigDecimal sumPaidByUserInGroup(@Param("userId") Long userId,
                                    @Param("groupId") Long groupId);

    // Analytics queries (Sanika)
    @Query("""
        SELECT e.category AS category, SUM(e.totalAmount) AS total
        FROM Expense e WHERE e.group.id = :groupId
        GROUP BY e.category
        """)
    List<Object[]> sumByCategory(@Param("groupId") Long groupId);

    @Query("""
        SELECT MONTH(e.expenseDate) AS month, SUM(e.totalAmount) AS total
        FROM Expense e WHERE e.group.id = :groupId AND YEAR(e.expenseDate) = :year
        GROUP BY MONTH(e.expenseDate)
        ORDER BY MONTH(e.expenseDate)
        """)
    List<Object[]> monthlyTotals(@Param("groupId") Long groupId,
                                  @Param("year") int year);
}
