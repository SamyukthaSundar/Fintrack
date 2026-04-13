package com.fintrack.repository;

import com.fintrack.model.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ExpenseSplitRepository
 * Owner: Samyuktha S
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
}
