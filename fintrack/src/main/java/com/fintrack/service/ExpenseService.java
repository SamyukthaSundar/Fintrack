package com.fintrack.service;

import com.fintrack.dto.ExpenseCreateDto;
import com.fintrack.model.Expense;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * ExpenseService Interface
 * Owner: Samyuktha S (Automation & Strategy Lead)
 */
public interface ExpenseService {
    Expense createExpense(ExpenseCreateDto dto, Long creatorUserId);
    Expense createExpenseWithReceipt(ExpenseCreateDto dto, MultipartFile receiptFile, Long creatorUserId);
    Optional<Expense> findById(Long id);
    List<Expense> findByGroupId(Long groupId);
    void deleteExpense(Long expenseId, Long requestingUserId);
    String extractTextFromReceipt(MultipartFile file);
}
