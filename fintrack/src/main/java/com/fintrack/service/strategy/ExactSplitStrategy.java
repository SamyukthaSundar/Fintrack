package com.fintrack.service.strategy;

import com.fintrack.model.ExpenseSplit;
import com.fintrack.model.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ExactSplitStrategy — Owner: Samyuktha S
 */
@Component("EXACT")
public class ExactSplitStrategy implements SplitStrategy {

    @Override
    public List<ExpenseSplit> computeSplits(BigDecimal totalAmount,
                                             List<User> participants,
                                             Map<Long, BigDecimal> splitData) {
        List<ExpenseSplit> splits = new ArrayList<>();
        for (User user : participants) {
            BigDecimal amt = splitData.getOrDefault(user.getId(), BigDecimal.ZERO);
            ExpenseSplit s = new ExpenseSplit();
            s.setUser(user);
            s.setAmount(amt);
            splits.add(s);
        }
        return splits;
    }

    @Override
    public void validate(BigDecimal totalAmount, List<User> participants, Map<Long, BigDecimal> splitData) {
        BigDecimal sum = splitData.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(totalAmount) != 0)
            throw new IllegalArgumentException(
                "Exact amounts must sum to " + totalAmount + ". Current sum: " + sum);
    }
}
