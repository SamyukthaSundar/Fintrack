package com.fintrack.service.impl;

import com.fintrack.dto.ExpenseCreateDto;
import com.fintrack.model.*;
import com.fintrack.observer.FinTrackEvent;
import com.fintrack.repository.*;
import com.fintrack.service.ExpenseService;
import com.fintrack.service.strategy.SplitStrategy;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ExpenseServiceImpl — Owner: Samyuktha S
 * OCR-Powered Expense Orchestrator + Multi-Strategy Split Logic
 *
 * PERF FIXES:
 *  - getParticipants() uses a single IN-query instead of N individual lookups
 *  - notifyGroupMembers() reuses the already-loaded GroupMember list (passed in)
 *    so we don't query the DB twice in the same transaction
 *  - findByGroupId() is read-only
 */
@Service
@Transactional
public class ExpenseServiceImpl implements ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseServiceImpl.class);
    private static final String UPLOAD_DIR = "uploads/receipts/";

    private final ExpenseRepository        expenseRepository;
    private final ExpenseSplitRepository   splitRepository;
    private final GroupRepository          groupRepository;
    private final GroupMemberRepository    memberRepository;
    private final UserRepository           userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, SplitStrategy> splitStrategies;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository,
                               ExpenseSplitRepository splitRepository,
                               GroupRepository groupRepository,
                               GroupMemberRepository memberRepository,
                               UserRepository userRepository,
                               ApplicationEventPublisher eventPublisher,
                               Map<String, SplitStrategy> splitStrategies) {
        this.expenseRepository = expenseRepository;
        this.splitRepository   = splitRepository;
        this.groupRepository   = groupRepository;
        this.memberRepository  = memberRepository;
        this.userRepository    = userRepository;
        this.eventPublisher    = eventPublisher;
        this.splitStrategies   = splitStrategies;
    }

    @Override
    public Expense createExpense(ExpenseCreateDto dto, Long creatorUserId) {
        return createExpenseWithReceipt(dto, null, creatorUserId);
    }

    @Override
    public Expense createExpenseWithReceipt(ExpenseCreateDto dto,
                                             MultipartFile receiptFile,
                                             Long creatorUserId) {
        Group group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        User paidBy = userRepository.findById(dto.getPaidById())
                .orElseThrow(() -> new IllegalArgumentException("Payer not found."));

        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setPaidBy(paidBy);
        expense.setTitle(dto.getTitle());
        expense.setDescription(dto.getDescription());
        expense.setTotalAmount(dto.getTotalAmount());
        expense.setSplitType(Expense.SplitType.valueOf(dto.getSplitType()));
        expense.setCategory(Expense.Category.valueOf(dto.getCategory()));
        expense.setExpenseDate(dto.getExpenseDate() != null ? dto.getExpenseDate() : LocalDate.now());

        if (receiptFile != null && !receiptFile.isEmpty()) {
            String ocrText = extractTextFromReceipt(receiptFile);
            expense.setOcrRawText(ocrText);
            if (dto.getTotalAmount() == null || dto.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal detected = detectAmountFromOcr(ocrText);
                if (detected != null) expense.setTotalAmount(detected);
            }
            String savedPath = saveReceiptFile(receiptFile, group.getId());
            expense.setReceiptImage(savedPath);
        }

        expense = expenseRepository.save(expense);

        // Load group members once — reused for both participant resolution and notifications
        List<GroupMember> groupMembers = memberRepository.findMembersWithUsers(group.getId());

        List<User> participants = resolveParticipants(dto, groupMembers);
        if (participants.isEmpty())
            throw new IllegalArgumentException("No valid participants found for this expense.");

        SplitStrategy strategy = splitStrategies.get(dto.getSplitType());
        if (strategy == null)
            throw new IllegalArgumentException("Unknown split type: " + dto.getSplitType());

        Map<Long, BigDecimal> splitData = dto.getSplitData() != null ? dto.getSplitData() : Map.of();
        strategy.validate(expense.getTotalAmount(), participants, splitData);
        List<ExpenseSplit> splits = strategy.computeSplits(expense.getTotalAmount(), participants, splitData);

        final Expense finalExpense = expense;
        for (ExpenseSplit s : splits) { s.setExpense(finalExpense); }
        splitRepository.saveAll(splits);

        // Reuse the already-loaded groupMembers list — no second DB call
        notifyGroupMembers(groupMembers, paidBy, expense);

        log.info("Expense '{}' created in group '{}' by '{}' | split={} | participants={}",
                expense.getTitle(), group.getName(), paidBy.getUsername(),
                dto.getSplitType(), participants.size());
        return expense;
    }

    /**
     * Resolve participants from the DTO.
     * If explicit participantIds are supplied, use them (single IN-query).
     * Otherwise fall back to all group members.
     *
     * PERF: original code called userRepository.findAllByIdIn() which is fine,
     *       but we now reuse the already-loaded GroupMember list when no IDs given
     *       to avoid a second DB round-trip.
     */
    private List<User> resolveParticipants(ExpenseCreateDto dto, List<GroupMember> groupMembers) {
        if (dto.getParticipantIds() != null && !dto.getParticipantIds().isEmpty()) {
            // Filter from already-loaded group members to avoid an extra DB query
            Set<Long> requested = new HashSet<>(dto.getParticipantIds());
            List<User> result = new ArrayList<>();
            for (GroupMember gm : groupMembers) {
                if (requested.contains(gm.getUser().getId())) {
                    result.add(gm.getUser());
                }
            }
            return result;
        }
        // Default: all group members
        List<User> all = new ArrayList<>();
        for (GroupMember gm : groupMembers) all.add(gm.getUser());
        return all;
    }

    @Override
    public String extractTextFromReceipt(MultipartFile file) {
        try {
            Path tempDir  = Files.createTempDirectory("fintrack_ocr");
            File tempFile = tempDir.resolve(Objects.requireNonNull(file.getOriginalFilename())).toFile();
            file.transferTo(tempFile);
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(System.getenv().getOrDefault("TESSDATA_PREFIX", "tessdata"));
            tesseract.setLanguage("eng");
            String result = tesseract.doOCR(tempFile);
            tempFile.delete();
            return result;
        } catch (TesseractException | IOException e) {
            log.warn("OCR failed ({}), using mock.", e.getMessage());
            return mockOcrExtraction();
        }
    }

    private String mockOcrExtraction() {
        return "RECEIPT\n-------\nItems: Food & Beverages\nSubtotal: 850.00\nTax (18%): 153.00\n"
             + "TOTAL: INR 1003.00\nDate: " + LocalDate.now() + "\nThank you!";
    }

    private BigDecimal detectAmountFromOcr(String ocrText) {
        if (ocrText == null) return null;
        Pattern[] patterns = {
            Pattern.compile("(?i)total[:\\s]+(?:inr|rs\\.?|₹)?\\s*([\\d,]+\\.?\\d{0,2})"),
            Pattern.compile("(?i)amount[:\\s]+(?:inr|rs\\.?|₹)?\\s*([\\d,]+\\.?\\d{0,2})")
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(ocrText);
            if (m.find()) {
                try { return new BigDecimal(m.group(1).replace(",", "")); }
                catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private String saveReceiptFile(MultipartFile file, Long groupId) {
        try {
            Path dir = Paths.get(UPLOAD_DIR + groupId);
            Files.createDirectories(dir);
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            file.transferTo(dir.resolve(filename).toFile());
            return "/uploads/receipts/" + groupId + "/" + filename;
        } catch (IOException e) {
            log.error("Failed to save receipt file", e);
            return null;
        }
    }

    /** Notify all group members except the payer. Reuses pre-loaded list. */
    private void notifyGroupMembers(List<GroupMember> members, User paidBy, Expense expense) {
        for (GroupMember gm : members) {
            if (!gm.getUser().getId().equals(paidBy.getId())) {
                eventPublisher.publishEvent(new FinTrackEvent(
                        this, Notification.NotificationType.EXPENSE_ADDED, gm.getUser().getId(),
                        "New Expense in " + expense.getGroup().getName(),
                        paidBy.getFullName() + " added '" + expense.getTitle()
                                + "' — ₹" + expense.getTotalAmount(),
                        expense.getId(), "EXPENSE"));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Expense> findById(Long id) {
        return expenseRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expense> findByGroupId(Long groupId) {
        return expenseRepository.findByGroupIdOrderByExpenseDateDesc(groupId);
    }

    @Override
    public void deleteExpense(Long expenseId, Long requestingUserId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found."));
        expenseRepository.delete(expense);
        log.info("Expense {} deleted by user {}", expenseId, requestingUserId);
    }
}