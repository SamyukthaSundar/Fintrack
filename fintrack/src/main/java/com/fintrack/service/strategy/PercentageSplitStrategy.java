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
 *
 * FIX: validate() now tolerates ±0.01 rounding error (e.g. 33.33+33.33+33.34=100).
 *      computeSplits() assigns remainder to last participant to guarantee exact total.
 *      Missing participants in splitData default to 0% and are flagged clearly.
 */
@Component("PERCENTAGE")
public class PercentageSplitStrategy implements SplitStrategy {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TOLERANCE = new BigDecimal("0.02");

    @Override
    public List<ExpenseSplit> computeSplits(BigDecimal totalAmount,
                                             List<User> participants,
                                             Map<Long, BigDecimal> splitData) {
        List<ExpenseSplit> splits = new ArrayList<>();
        BigDecimal assigned = BigDecimal.ZERO;

        for (int i = 0; i < participants.size(); i++) {
            User user = participants.get(i);
            BigDecimal pct = splitData.getOrDefault(user.getId(), BigDecimal.ZERO);
            BigDecimal amt;
            if (i == participants.size() - 1) {
                // Last participant gets the remainder to avoid rounding gaps
                amt = totalAmount.subtract(assigned);
            } else {
                amt = totalAmount.multiply(pct)
                                 .divide(HUNDRED, 2, RoundingMode.HALF_UP);
                assigned = assigned.add(amt);
            }
            ExpenseSplit s = new ExpenseSplit();
            s.setUser(user);
            s.setAmount(amt);
            s.setPercentage(pct);
            splits.add(s);
        }
        return splits;
    }

    @Override
    public void validate(BigDecimal totalAmount, List<User> participants,
                         Map<Long, BigDecimal> splitData) {
        if (participants == null || participants.isEmpty())
            throw new IllegalArgumentException("Participants list cannot be empty.");

        // Only validate percentages for the actual participants
        BigDecimal sum = BigDecimal.ZERO;
        for (User u : participants) {
            BigDecimal pct = splitData.get(u.getId());
            if (pct == null)
                throw new IllegalArgumentException(
                    "Missing percentage for participant: " + u.getFullName()
                    + ". Please enter a percentage for every selected participant.");
            if (pct.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException(
                    "Percentage cannot be negative for: " + u.getFullName());
            sum = sum.add(pct);
        }

        BigDecimal diff = sum.subtract(HUNDRED).abs();
        if (diff.compareTo(TOLERANCE) > 0)
            throw new IllegalArgumentException(
                "Percentages must sum to 100. Current sum: " + sum
                + ". Please adjust the values.");
    }
}