package com.marketplace.service;

import com.marketplace.entity.Transaction;
import com.marketplace.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        return transactionRepository.findByDateRange(start, end);
    }

    public List<Transaction> getUserTransactionsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        return transactionRepository.findByUserIdAndDateRange(userId, start, end);
    }

    public Map<String, Object> generateSummaryReport(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);

        // Scope all counts to the current user's transactions only
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateRange(userId, start, end);
        report.put("transactions", transactions);
        report.put("totalTransactions", (long) transactions.size());

        long purchases = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.PURCHASE && t.getBuyerId().equals(userId))
                .count();
        report.put("purchaseCount", purchases);

        long totalRevenueCents = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.PURCHASE
                        && t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .mapToLong(t -> t.getTotalCents() != null ? t.getTotalCents() : 0L)
                .sum();
        report.put("totalRevenueCents", totalRevenueCents);

        return report;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}
