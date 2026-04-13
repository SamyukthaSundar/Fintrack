package com.fintrack.service.impl;

import com.fintrack.repository.ExpenseRepository;
import com.fintrack.repository.GroupMemberRepository;
import com.fintrack.repository.SettlementRepository;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AnalyticsService — Owner: Sanika Gupta
 * Minor Feature: Visual Analytics & Reporting (Chart.js data + PDF export)
 */
@Service
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final GroupMemberRepository memberRepository;

    public AnalyticsService(ExpenseRepository expenseRepository,
                             SettlementRepository settlementRepository,
                             GroupMemberRepository memberRepository) {
        this.expenseRepository   = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.memberRepository    = memberRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getMonthlyTotals(Long groupId, int year) {
        java.util.List<Object[]> rows = expenseRepository.monthlyTotals(groupId, year);
        String[] months = {"", "January", "February", "March", "April", "May", "June",
                           "July", "August", "September", "October", "November", "December"};
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) result.put(months[i], BigDecimal.ZERO);
        for (Object[] row : rows) {
            int month = ((Number) row[0]).intValue();
            BigDecimal total = (BigDecimal) row[1];
            result.put(months[month], total);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getCategoryBreakdown(Long groupId) {
        java.util.List<Object[]> rows = expenseRepository.sumByCategory(groupId);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(row[0].toString(), (BigDecimal) row[1]);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSettlementStats(Long groupId) {
        long total    = settlementRepository.findByGroupIdOrderByInitiatedAtDesc(groupId).size();
        long verified = settlementRepository.findByGroupIdAndStatus(
                groupId, com.fintrack.model.Settlement.SettlementStatus.VERIFIED).size();
        long pending  = settlementRepository.findByGroupIdAndStatus(
                groupId, com.fintrack.model.Settlement.SettlementStatus.SUBMITTED).size();
        return Map.of("total", total, "verified", verified, "pending", pending);
    }

    @Transactional(readOnly = true)
    public byte[] generateGroupStatement(Long groupId, String groupName) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 60, 40);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font titleFont  = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,  new BaseColor(0, 200, 150));
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,  BaseColor.WHITE);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);
        Font subFont    = new Font(Font.FontFamily.HELVETICA,  9, Font.ITALIC, BaseColor.GRAY);

        Paragraph title = new Paragraph("FinTrack — Group Statement", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph sub = new Paragraph("Group: " + groupName + "  |  Generated: " + LocalDate.now(), subFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(20);
        doc.add(sub);

        doc.add(new Paragraph("Monthly Expense Summary (" + Year.now().getValue() + ")", headerFont));
        Map<String, BigDecimal> monthly = getMonthlyTotals(groupId, Year.now().getValue());
        PdfPTable monthTable = new PdfPTable(2);
        monthTable.setWidthPercentage(100);
        monthTable.setWidths(new float[]{3f, 2f});
        addTableHeader(monthTable, headerFont, "Month", "Total (₹)");
        for (Map.Entry<String, BigDecimal> entry : monthly.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                monthTable.addCell(new Phrase(entry.getKey(), normalFont));
                monthTable.addCell(new Phrase(entry.getValue().toPlainString(), normalFont));
            }
        }
        doc.add(monthTable);
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Expense by Category", headerFont));
        Map<String, BigDecimal> cats = getCategoryBreakdown(groupId);
        PdfPTable catTable = new PdfPTable(2);
        catTable.setWidthPercentage(100);
        catTable.setWidths(new float[]{3f, 2f});
        addTableHeader(catTable, headerFont, "Category", "Total (₹)");
        for (Map.Entry<String, BigDecimal> entry : cats.entrySet()) {
            catTable.addCell(new Phrase(entry.getKey(), normalFont));
            catTable.addCell(new Phrase(entry.getValue().toPlainString(), normalFont));
        }
        doc.add(catTable);
        doc.add(Chunk.NEWLINE);

        BigDecimal grandTotal = expenseRepository.sumTotalByGroupId(groupId);
        Paragraph total = new Paragraph("Grand Total: ₹" + grandTotal.toPlainString(),
                new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(0, 200, 150)));
        total.setSpacingBefore(10);
        doc.add(total);

        Paragraph footer = new Paragraph(
                "\nGenerated by FinTrack — Collaborative Expense Engine\nUE23CS352B OOAD Mini Project", subFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
        return baos.toByteArray();
    }

    private void addTableHeader(PdfPTable table, Font font, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(new BaseColor(30, 30, 30));
            cell.setPadding(6);
            table.addCell(cell);
        }
    }
}
