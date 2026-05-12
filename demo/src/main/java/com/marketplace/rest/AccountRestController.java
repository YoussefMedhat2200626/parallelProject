package com.marketplace.rest;

import com.marketplace.entity.*;
import com.marketplace.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Web Service #2: Account Info API
 * Returns balance, purchased items, sold items, and items for sale.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountRestController {

    private final UserService userService;
    private final WalletService walletService;
    private final ItemService itemService;
    private final TransactionService transactionService;

    public AccountRestController(UserService userService, WalletService walletService,
                                 ItemService itemService, TransactionService transactionService) {
        this.userService = userService;
        this.walletService = walletService;
        this.itemService = itemService;
        this.transactionService = transactionService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getAccountInfo(@PathVariable Long userId) {
        return userService.findById(userId)
                .map(user -> {
                    Wallet wallet = walletService.getWallet(userId);
                    List<Item> itemsForSale = itemService.getSellerItems(userId);
                    List<Transaction> purchases = transactionService.getBuyerTransactions(userId);
                    List<Transaction> sales = transactionService.getSellerTransactions(userId);

                    Map<String, Object> info = new HashMap<>();
                    info.put("userId", user.getUserId());
                    info.put("username", user.getUsername());
                    info.put("fullName", user.getFullName());
                    info.put("email", user.getEmail());
                    info.put("balanceCents", wallet.getBalanceCents());
                    info.put("balanceFormatted", wallet.getBalanceFormatted());
                    info.put("itemsForSale", itemsForSale.size());
                    info.put("totalPurchases", purchases.size());
                    info.put("totalSales", sales.size());
                    return ResponseEntity.ok((Object) info);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
