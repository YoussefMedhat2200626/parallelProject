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

    public Map<String, Object> generateSummaryReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalTransactions", transactionRepository.countByDateRange(start, end));
        report.put("totalRevenueCents", transactionRepository.sumCompletedPurchases(start, end));
        List<Transaction> transactions = transactionRepository.findByDateRange(start, end);
        report.put("transactions", transactions);
        long purchases = transactions.stream().filter(t -> t.getType() == Transaction.TransactionType.PURCHASE).count();
        report.put("purchaseCount", purchases);
        return report;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}
