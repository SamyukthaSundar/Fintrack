package com.fintrack.service.strategy;

import com.fintrack.model.ExpenseSplit;
import com.fintrack.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * SplitStrategy — Strategy Pattern Interface
 * Owner: Samyuktha S (Automation & Strategy Lead)
 *
 * Design Pattern: Strategy
 *   Defines a family of split algorithms (Equal, Percentage, Exact, Weighted).
 *   Algorithms are encapsulated and interchangeable at runtime.
 *
 * Design Principle: OCP — new split strategies can be added without modifying
 *   the ExpenseService (open for extension, closed for modification).
 */
public interface SplitStrategy {

    /**
     * Compute splits for a given total among participants.
     *
     * @param totalAmount   the total expense amount
     * @param participants  list of users to split among
     * @param splitData     strategy-specific data (percentages, weights, exact amounts)
     * @return              list of computed ExpenseSplit objects (not yet persisted)
     */
    List<ExpenseSplit> computeSplits(BigDecimal totalAmount,
                                     List<User> participants,
                                     Map<Long, BigDecimal> splitData);

    /**
     * Validate that the provided splitData is consistent with this strategy.
     * E.g., percentages must sum to 100, exact amounts must sum to totalAmount.
     */
    void validate(BigDecimal totalAmount,
                  List<User> participants,
                  Map<Long, BigDecimal> splitData);
}
