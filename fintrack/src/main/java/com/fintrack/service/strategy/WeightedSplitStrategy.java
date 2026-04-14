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
 *
 * FIX: validate() now checks every participant has a positive weight entry.
 *      computeSplits() only sums weights for actual participants (not all of splitData).
 *      Last participant receives the remainder to avoid rounding gaps.
 */
@Component("WEIGHTED")
public class WeightedSplitStrategy implements SplitStrategy {

    @Override
    public List<ExpenseSplit> computeSplits(BigDecimal totalAmount,
                                             List<User> participants,
                                             Map<Long, BigDecimal> splitData) {
        // Sum only the weights of actual participants, not all keys in splitData
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (User u : participants) {
            totalWeight = totalWeight.add(
                splitData.getOrDefault(u.getId(), BigDecimal.ONE));
        }

        List<ExpenseSplit> splits = new ArrayList<>();
        BigDecimal assigned = BigDecimal.ZERO;

        for (int i = 0; i < participants.size(); i++) {
            User user = participants.get(i);
            BigDecimal weight = splitData.getOrDefault(user.getId(), BigDecimal.ONE);
            BigDecimal amt;
            if (i == participants.size() - 1) {
                // Last participant absorbs any rounding remainder
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
    public void validate(BigDecimal totalAmount, List<User> participants,
                         Map<Long, BigDecimal> splitData) {
        if (participants == null || participants.isEmpty())
            throw new IllegalArgumentException("Participants list cannot be empty.");

        for (User u : participants) {
            BigDecimal weight = splitData.get(u.getId());
            if (weight == null)
                throw new IllegalArgumentException(
                    "Missing weight for participant: " + u.getFullName()
                    + ". Please enter a weight for every selected participant.");
            if (weight.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException(
                    "Weight must be a positive value for: " + u.getFullName());
        }
    }
}