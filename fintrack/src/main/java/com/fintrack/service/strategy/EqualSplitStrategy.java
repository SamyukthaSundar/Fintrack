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
 * EqualSplitStrategy — Owner: Samyuktha S
 * Divides expense equally among all participants.
 */
@Component("EQUAL")
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public List<ExpenseSplit> computeSplits(BigDecimal totalAmount,
                                             List<User> participants,
                                             Map<Long, BigDecimal> splitData) {
        int n = participants.size();
        BigDecimal base      = totalAmount.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        BigDecimal assigned  = base.multiply(BigDecimal.valueOf(n));
        BigDecimal remainder = totalAmount.subtract(assigned);

        List<ExpenseSplit> splits = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            BigDecimal amt = (i == 0) ? base.add(remainder) : base;
            ExpenseSplit s = new ExpenseSplit();
            s.setUser(participants.get(i));
            s.setAmount(amt);
            splits.add(s);
        }
        return splits;
    }

    @Override
    public void validate(BigDecimal totalAmount, List<User> participants, Map<Long, BigDecimal> splitData) {
        if (participants == null || participants.isEmpty())
            throw new IllegalArgumentException("Participants list cannot be empty.");
    }
}
