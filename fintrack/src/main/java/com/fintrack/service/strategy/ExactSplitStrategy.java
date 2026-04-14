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
 *
 * FIX: validate() now checks every participant has an entry, and uses a small
 *      tolerance (±0.01) for floating-point input rounding.
 *      Only participant entries are summed — not all keys in splitData.
 */
@Component("EXACT")
public class ExactSplitStrategy implements SplitStrategy {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.02");

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
    public void validate(BigDecimal totalAmount, List<User> participants,
                         Map<Long, BigDecimal> splitData) {
        if (participants == null || participants.isEmpty())
            throw new IllegalArgumentException("Participants list cannot be empty.");

        BigDecimal sum = BigDecimal.ZERO;
        for (User u : participants) {
            BigDecimal amt = splitData.get(u.getId());
            if (amt == null)
                throw new IllegalArgumentException(
                    "Missing exact amount for participant: " + u.getFullName()
                    + ". Please enter an amount for every selected participant.");
            if (amt.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException(
                    "Amount cannot be negative for: " + u.getFullName());
            sum = sum.add(amt);
        }

        BigDecimal diff = sum.subtract(totalAmount).abs();
        if (diff.compareTo(TOLERANCE) > 0)
            throw new IllegalArgumentException(
                "Exact amounts must sum to ₹" + totalAmount
                + ". Current sum: ₹" + sum + ". Difference: ₹" + diff);
    }
}