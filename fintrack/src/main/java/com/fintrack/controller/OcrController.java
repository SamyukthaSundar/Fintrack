package com.fintrack.controller;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fintrack.service.impl.ExpenseServiceImpl;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final ExpenseServiceImpl expenseService;

    public OcrController(ExpenseServiceImpl expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("Received file for OCR: " + file.getOriginalFilename());
            
            String rawText = expenseService.extractTextFromReceipt(file);
            
            // If OCR returned an error message
            if (rawText.startsWith("ERROR:")) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", rawText.replace("ERROR: ", "")
                ));
            }

            // Extract structured data
            BigDecimal amount = expenseService.detectAmountFromOcr(rawText);
            String merchant = expenseService.detectMerchantName(rawText);
            String date = expenseService.extractDate(rawText);
            List<Map<String, Object>> items = expenseService.extractItems(rawText);
            
            // Build clean response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("merchant", merchant);
            response.put("amount", amount);
            response.put("date", date);
            response.put("items", items);
            response.put("title", merchant + " - Receipt");
            response.put("itemCount", items.size());
            
            // Clean preview (first 200 chars of cleaned text)
            String preview = rawText.replaceAll("\\s+", " ").trim();
            if (preview.length() > 200) preview = preview.substring(0, 200) + "...";
            response.put("textPreview", preview);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}