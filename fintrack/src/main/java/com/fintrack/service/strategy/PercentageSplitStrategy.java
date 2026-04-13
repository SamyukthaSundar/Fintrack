package com.fintrack.service.strategy;

import com.fintrack.model.ExpenseSplit;
import com.fintrack.model.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PercentageSplitStrategy — Owner: Samyuktha S
 */
@Component("PERCENTAGE")
public class PercentageSplitStrategy implements SplitStrategy {

    @Override
    public List<ExpenseSplit> computeSplits(BigDecimal totalAmount,
                                             List<User> participants,
                                             Map<Long, BigDecimal> splitData) {
        List<ExpenseSplit> splits = new ArrayList<>();
        for (User user : participants) {
            BigDecimal pct = splitData.getOrDefault(user.getId(), BigDecimal.ZERO);
            BigDecimal amt = totalAmount.multiply(pct)
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            ExpenseSplit s = new ExpenseSplit();
            s.setUser(user);
            s.setAmount(amt);
            s.setPercentage(pct);
            splits.add(s);
        }
        return splits;
    }

    @Override
    public void validate(BigDecimal totalAmount, List<User> participants, Map<Long, BigDecimal> splitData) {
        BigDecimal sum = splitData.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(new BigDecimal("100.00")) != 0)
            throw new IllegalArgumentException("Percentages must sum to 100. Current sum: " + sum);
    }
}
