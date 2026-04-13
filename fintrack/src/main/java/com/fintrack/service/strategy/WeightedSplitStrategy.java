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
 * WeightedSplitStrategy — Owner: Samyuktha S
 */
@Component("WEIGHTED")
public class WeightedSplitStrategy implements SplitStrategy {

    @Override
    public List<ExpenseSplit> computeSplits(BigDecimal totalAmount,
                                             List<User> participants,
                                             Map<Long, BigDecimal> splitData) {
        BigDecimal totalWeight = splitData.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ExpenseSplit> splits = new ArrayList<>();
        BigDecimal assigned = BigDecimal.ZERO;

        for (int i = 0; i < participants.size(); i++) {
            User user = participants.get(i);
            BigDecimal weight = splitData.getOrDefault(user.getId(), BigDecimal.ONE);
            BigDecimal amt;
            if (i == participants.size() - 1) {
                amt = totalAmount.subtract(assigned);
            } else {
                amt = totalAmount.multiply(weight)
                                 .divide(totalWeight, 2, RoundingMode.HALF_UP);
                assigned = assigned.add(amt);
            }
            ExpenseSplit s = new ExpenseSplit();
            s.setUser(user);
            s.setAmount(amt);
            s.setWeight(weight);
            splits.add(s);
        }
        return splits;
    }

    @Override
    public void validate(BigDecimal totalAmount, List<User> participants, Map<Long, BigDecimal> splitData) {
        boolean anyNonPositive = splitData.values().stream()
                .anyMatch(v -> v.compareTo(BigDecimal.ZERO) <= 0);
        if (anyNonPositive)
            throw new IllegalArgumentException("All weights must be positive values.");
    }
}
