package com.fintrack.service.impl;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fintrack.dto.ExpenseCreateDto;
import com.fintrack.model.Expense;
import com.fintrack.model.ExpenseSplit;
import com.fintrack.model.Group;
import com.fintrack.model.GroupMember;
import com.fintrack.model.Notification;
import com.fintrack.model.User;
import com.fintrack.observer.FinTrackEvent;
import com.fintrack.repository.ExpenseRepository;
import com.fintrack.repository.ExpenseSplitRepository;
import com.fintrack.repository.GroupMemberRepository;
import com.fintrack.repository.GroupRepository;
import com.fintrack.repository.UserRepository;
import com.fintrack.service.ExpenseService;
import com.fintrack.service.strategy.SplitStrategy;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

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
        File tempFile = null;
        File processedFile = null;
        try {
<<<<<<< Updated upstream
            Path tempDir  = Files.createTempDirectory("fintrack_ocr");
            File tempFile = tempDir.resolve(Objects.requireNonNull(file.getOriginalFilename())).toFile();
=======
            // Create temp file with proper extension detection
            Path tempDir = Files.createTempDirectory("fintrack_ocr");
            String originalName = Objects.requireNonNull(file.getOriginalFilename(), "unnamed");
            String extension = getExtension(originalName);
            tempFile = tempDir.resolve("receipt_" + System.currentTimeMillis() + extension).toFile();
>>>>>>> Stashed changes
            file.transferTo(tempFile);
            
            log.info("Processing receipt: {} ({} bytes)", originalName, file.getSize());
            
            // Try preprocessing, fallback to original if it fails
            try {
                processedFile = preprocessImage(tempFile);
            } catch (Exception e) {
                log.warn("Preprocessing failed: {}, using original", e.getMessage());
                processedFile = tempFile;
            }
            
            // Run OCR
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(System.getenv().getOrDefault("TESSDATA_PREFIX", "tessdata"));
            tesseract.setLanguage("eng");
            tesseract.setOcrEngineMode(1);
            tesseract.setPageSegMode(6);
            
            String result = tesseract.doOCR(processedFile);
            log.info("OCR OUTPUT:\n{}", result);
            
            return postProcessOcrText(result);
            
        } catch (TesseractException | IOException e) {
            log.error("OCR failed completely: {}", e.getMessage(), e);
            return "ERROR: " + e.getMessage();
        } finally {
            // Cleanup
            if (tempFile != null && tempFile.exists()) tempFile.delete();
            if (processedFile != null && processedFile.exists() && !processedFile.equals(tempFile)) {
                processedFile.delete();
            }
        }
    }
    
    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(dot) : ".png";
    }
    
    private File preprocessImage(File inputFile) throws IOException {
        BufferedImage original = ImageIO.read(inputFile);
        if (original == null) {
            throw new IOException("Could not read image: " + inputFile.getName());
        }
        
        log.debug("Original image: {}x{}", original.getWidth(), original.getHeight());
        
        // Scale up for better OCR (but not too big)
        double scale = Math.min(2.0, 2000.0 / Math.max(original.getWidth(), original.getHeight()));
        if (scale < 1) scale = 1.5; // At least 1.5x for small images
        
        int targetWidth = (int) (original.getWidth() * scale);
        int targetHeight = (int) (original.getHeight() * scale);
        
        // Convert to grayscale and scale
        BufferedImage gray = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        
        // Save as PNG (always works with Tesseract)
        File processed = new File(inputFile.getParent(), "processed_" + System.currentTimeMillis() + ".png");
        ImageIO.write(gray, "png", processed);
        
        log.debug("Processed image: {}x{} saved to {}", targetWidth, targetHeight, processed.getName());
        return processed;
    }
    
    private String postProcessOcrText(String text) {
        if (text == null) return "";
        
        // First pass: remove garbage characters and fix encoding artifacts
        String cleaned = text
            // Remove Unicode box drawing and special chars
            .replaceAll("[┬«┬⌐┬ú┬ó├ª├ó├ù├▓├│├╗├╜├┐├á├í├ó├ú├ñ├Ñ├ª├º├¿├®├¬├½├¼├¡├«├»├░├▒├▓├│├┤├╡├Â├À├©├╣├║├╗├╝├¢├╛├┐├Ç├ü├é├â├ä├à├å├ç├ê├ë├è├ï├î├ì├Ä├Å├É├æ├Æ├ô├ö├ò├û├ù├ÿ├Ö├Ü├¢├£├Ø├×├ƒ]", "")
            // Remove common OCR noise chars
            .replaceAll("[ΓÇöΓÇöΓÇöΓÇôΓÇöΓÇöΓÇöΓÇöΓÇöΓÇöΓÇöΓÇöΓÇöΓÇö├à├?]", "-")  // Various dashes to simple dash
            .replaceAll("[ΓÇ£ΓÇ¥ΓÇ₧ΓÇ░]", "\"")  // Smart quotes
            .replaceAll("[ΓÇÿΓÇÖΓÇÜ]", "'")   // Smart apostrophes
            .replaceAll("[Γé╣Γé│Γé▒Γé▓Γé┤]", "Rs") // Currency symbols
            .replaceAll("[┬ú┬ú┬£]", "")
            // Box drawing chars
            .replaceAll("[ΓöÇΓöéΓöÉΓöîΓöÿΓöÿΓöñΓö¼Γö┤Γö╢Γö╖Γö©Γö╣Γö║Γö╗Γö╝Γö╜Γö╛Γö┐ΓòÇΓòüΓòéΓòâΓòäΓòàΓòåΓòçΓòêΓòëΓòèΓòïΓòîΓòìΓòÄΓòÅΓòÉΓòæΓòÆΓòôΓòöΓòòΓòûΓòùΓòÿΓòÖΓòÜΓò¢Γò£Γò¥Γò×ΓòƒΓòáΓòíΓòóΓòúΓòñΓòÑΓòªΓòºΓò¿Γò⌐Γò¬Γò½Γò¼Γò¡Γò«Γò»Γò░]", " ")
            .replaceAll("[ΓÇóΓÇó┬╗┬½┬╝┬╜┬╛]", " ")
            // Special symbols
            .replaceAll("[├⌐├⌐├⌐├⌐]", "e")
            .replaceAll("[├á├á├á├á]", "a")
            // Clean up multiple dashes/underscores
            .replaceAll("[-_]{2,}", " ")
            .replaceAll("\\s+", " ");
        
        return cleaned
            .replaceAll("[|]", "I")
            .replaceAll("[0](?=[A-Za-z])", "O")  // Zero before letter -> O
            .replaceAll("[\"']", "")
            .replaceAll("[\\]\\\\/<>]", " ")
            .replaceAll("@{2,}", " ")
            .replaceAll("%(?!\\d)", " ")
            .replaceAll("\\s+", " ")
            // Fix common words
            .replaceAll("(?i)tota[li]+|tofa[li]+|totai|tota[li]", "Total")
            .replaceAll("(?i)amo[un]+t|amoun|amnt", "Amount")
            .replaceAll("(?i)fioat|fioatbrew|fioat brew", "Float Brew")
            .replaceAll("(?i)grossamount|grossamo[un]+t|gross amnt", "Gross Amount")
            .replaceAll("(?i)poweredby", "Powered by")
            .replaceAll("(?i)thankyou|thank you", "Thank You")
            .replaceAll("(?i)visitagain|visit again", "Visit Again")
            .replaceAll("(?i)bake\s*house|bakehouse", "Bake House")
            .replaceAll("(?i)glens", "Glens")
            .trim();
    }
    
    @Override
    public List<Map<String, Object>> extractItems(String ocrText) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (ocrText == null) return items;
        
        // Pattern: ItemName Qty Price Amount (common receipt format)
        Pattern itemPattern = Pattern.compile("(?i)([A-Za-z][A-Za-z\\s&-]{2,30})\\s+(\\d+)\\s+(\\d+\\.\\d{2})\\s+(\\d+\\.\\d{2})");
        Matcher m = itemPattern.matcher(ocrText);
        
        while (m.find()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", m.group(1).trim());
            item.put("qty", Integer.parseInt(m.group(2)));
            item.put("price", new BigDecimal(m.group(3)));
            item.put("amount", new BigDecimal(m.group(4)));
            items.add(item);
        }
        
        // Alternative: Just name and price (simpler receipts)
        if (items.isEmpty()) {
            Pattern simplePattern = Pattern.compile("(?i)([A-Za-z][A-Za-z\\s&-]{3,25})\\s+(\\d{3,4}(?:\\.\\d{2})?)");
            Matcher m2 = simplePattern.matcher(ocrText);
            while (m2.find()) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", m2.group(1).trim());
                item.put("amount", new BigDecimal(m2.group(2)));
                items.add(item);
            }
        }
        
        return items;
    }
    
    @Override
    public String extractDate(String ocrText) {
        if (ocrText == null) return null;
        
        Pattern[] datePatterns = {
            Pattern.compile("(?i)date[:\\s]+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})"),
            Pattern.compile("(?i)date[:\\s]+(\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{2,4})"),
            Pattern.compile("(?i)(\\d{2}[/-]\\d{2}[/-]\\d{2,4})"),  // DD/MM/YY or DD/MM/YYYY
            Pattern.compile("(?i)(\\d{2}\\s+[A-Za-z]{3}\\s+\\d{4})")   // 21 Jan 2024
        };
        
        for (Pattern p : datePatterns) {
            Matcher m = p.matcher(ocrText);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    private String mockOcrExtraction() {
        return "RECEIPT\n-------\nItems: Food & Beverages\nSubtotal: 850.00\nTax (18%): 153.00\n"
             + "TOTAL: INR 1003.00\nDate: " + LocalDate.now() + "\nThank you!";
    }

    public BigDecimal detectAmountFromOcr(String ocrText) {
        if (ocrText == null || ocrText.isEmpty()) return null;
        
        String cleaned = ocrText.replaceAll("o\\s*([0-9])", "0$1");  // Fix "o 2304" -> "0 2304"
        
        Pattern[] patterns = {
            // Gross Amount (your receipt format)
            Pattern.compile("(?i)gross\\s*amount\\s*[:o]?\\s*([\\d,]+\\.?\\d{0,2})"),
            // Total patterns
            Pattern.compile("(?i)total(?:\\s*amount)?[:\\s]+(?:inr|rs\\.?|₹)?\\s*([\\d,]+\\.?\\d{0,2})"),
            Pattern.compile("(?i)grand\\s+total[:\\s]+(?:inr|rs\\.?|₹)?\\s*([\\d,]+\\.?\\d{0,2})"),
            Pattern.compile("(?i)net\\s+total[:\\s]+(?:inr|rs\\.?|₹)?\\s*([\\d,]+\\.?\\d{0,2})"),
            Pattern.compile("(?i)total[:\\s]*₹?\\s*([\\d,]+\\.?\\d{0,2})"),
            // Amount patterns
            Pattern.compile("(?i)amount\\s*[:\\s]+(?:inr|rs\\.?|₹)?\\s*([\\d,]+\\.?\\d{0,2})"),
            Pattern.compile("(?i)payable[:\\s]+(?:inr|rs\\.?|₹)?\\s*([\\d,]+\\.?\\d{0,2})"),
            // Amount with currency symbol
            Pattern.compile("(?i)(?:inr|rs\\.?|₹)\\s*([\\d,]+\\.?\\d{0,2})"),
            // Loose patterns for badly OCR'd text
            Pattern.compile("(?i)amount[o:]?\\s*([\\d,]+)", Pattern.CASE_INSENSITIVE),
            // Final fallback: standalone 3-5 digit numbers
            Pattern.compile("\\b([\\d,]{3,5}(?:\\.\\d{2})?)\\b")
        };
        
        for (Pattern p : patterns) {
            Matcher m = p.matcher(cleaned);
            while (m.find()) {
                try {
                    String amountStr = m.group(1).replace(",", "").replaceAll("[^0-9.]", "").trim();
                    if (amountStr.isEmpty()) continue;
                    BigDecimal amount = new BigDecimal(amountStr);
                    // Reasonable receipt amount range
                    if (amount.compareTo(new BigDecimal("10")) > 0 && amount.compareTo(new BigDecimal("100000")) < 0) {
                        return amount;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
    
    @Override
    public String detectMerchantName(String ocrText) {
        if (ocrText == null || ocrText.isEmpty()) return "Unknown Merchant";
        
        // Look for common patterns in receipts
        String[] lines = ocrText.split("\\r?\\n");
        
        // Check first few lines (merchant name usually at top)
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            String line = lines[i].trim();
            // Skip if line is too short, too long, or looks like an address/phone
            if (line.length() < 3 || line.length() > 40) continue;
            if (line.matches(".*\\d{3,}.*")) continue; // Has numbers (likely address/phone)
            if (line.matches("(?i).*street|st\\.|road|rd\\.|avenue|ave\\.|city|pin\\d+.*")) continue;
            if (line.toLowerCase().contains("receipt") || line.toLowerCase().contains("bill")) continue;
            
            // Clean up common OCR errors in merchant names
            line = line.replaceAll("(?i)fioat", "Float");
            line = line.replaceAll("(?i)brew", "Brew");
            
            // Return if looks like a business name (2+ words or capitalized)
            if (line.matches(".*[A-Z].*") && line.split("\\s+").length <= 4) {
                return line;
            }
        }
        
        // Fallback: look for specific patterns
        Pattern[] merchantPatterns = {
            Pattern.compile("(?i)([A-Z][a-zA-Z]+\\s+(?:Brew|Cafe|Restaurant|Kitchen|Bistro|Grill|Bar|Lounge|Hotel))"),
            Pattern.compile("(?i)([A-Z][a-zA-Z]+(?:[\\s-][A-Z][a-zA-Z]+)?)")
        };
        
        for (Pattern p : merchantPatterns) {
            Matcher m = p.matcher(ocrText);
            if (m.find()) {
                String name = m.group(1).trim();
                if (name.length() > 3 && name.length() < 30) {
                    return name;
                }
            }
        }
        
        return "Unknown Merchant";
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