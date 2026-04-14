package com.fintrack.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.fintrack.dto.ExpenseCreateDto;
import com.fintrack.model.Expense;

/**
 * ExpenseService Interface
 * Owner: Samyuktha S (Automation & Strategy Lead)
 */
public interface ExpenseService {
    Expense createExpense(ExpenseCreateDto dto, Long creatorUserId);
    Expense createExpenseWithReceipt(ExpenseCreateDto dto, MultipartFile receiptFile, Long creatorUserId);
    Expense updateExpense(Long expenseId, ExpenseCreateDto dto, Long requestingUserId);
    Optional<Expense> findById(Long id);
    List<Expense> findByGroupId(Long groupId);
    void deleteExpense(Long expenseId, Long requestingUserId);
    String extractTextFromReceipt(MultipartFile file);
    
    BigDecimal detectAmountFromOcr(String text);
    String detectMerchantName(String ocrText);
    List<Map<String, Object>> extractItems(String ocrText);
    String extractDate(String ocrText);
}
