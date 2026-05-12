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

        Map<String, Object> report = reportService.generateSummaryReport(start, end);
        List<Transaction> userTxns = reportService.getUserTransactionsByDateRange(userId, start, end);

        model.addAttribute("report", report);
        model.addAttribute("userTransactions", userTxns);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        return "reports";
    }
}
