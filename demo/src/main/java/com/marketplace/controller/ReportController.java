package com.marketplace.controller;

import com.marketplace.entity.Transaction;
import com.marketplace.service.ReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String reportsPage(@RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        Map<String, Object> report = reportService.generateSummaryReport(userId, start, end);
        List<Transaction> userTxns = reportService.getUserTransactionsByDateRange(userId, start, end);

        // Compute stats directly from the user's own transaction list so cards always match the table
        long totalTxns = userTxns.size();
        long purchaseCount = userTxns.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.PURCHASE && t.getBuyerId().equals(userId))
                .count();
        long revenueCents = userTxns.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.PURCHASE
                        && t.getStatus() == Transaction.TransactionStatus.COMPLETED
                        && t.getBuyerId().equals(userId))
                .mapToLong(t -> t.getTotalCents() != null ? t.getTotalCents() : 0L)
                .sum();

        report.put("totalTransactions", totalTxns);
        report.put("purchaseCount", purchaseCount);
        report.put("totalRevenueCents", revenueCents);

        model.addAttribute("report", report);
        model.addAttribute("userTransactions", userTxns);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        return "reports";
    }
}
