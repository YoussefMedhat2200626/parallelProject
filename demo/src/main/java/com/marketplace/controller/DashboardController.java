package com.marketplace.controller;

import com.marketplace.entity.*;
import com.marketplace.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final UserService userService;
    private final WalletService walletService;
    private final ItemService itemService;
    private final TransactionService transactionService;
    private final InventoryService inventoryService;

    public DashboardController(UserService userService, WalletService walletService,
                               ItemService itemService, TransactionService transactionService,
                               InventoryService inventoryService) {
        this.userService = userService;
        this.walletService = walletService;
        this.itemService = itemService;
        this.transactionService = transactionService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        Long userId = getSessionUserId(session);
        if (userId == null) return "redirect:/login";

        User user = userService.findById(userId).orElseThrow();
        Wallet wallet = walletService.getWallet(userId);
        List<Item> myItems = itemService.getSellerItems(userId);
        List<Transaction> purchases = transactionService.getBuyerTransactions(userId);
        List<Transaction> sales = transactionService.getSellerTransactions(userId);
        List<Inventory> inventory = inventoryService.getSellerInventory(userId);

        model.addAttribute("user", user);
        model.addAttribute("wallet", wallet);
        model.addAttribute("myItems", myItems);
        model.addAttribute("purchases", purchases);
        model.addAttribute("sales", sales);
        model.addAttribute("inventory", inventory);
        model.addAttribute("itemCount", myItems.size());
        model.addAttribute("purchaseCount", purchases.size());
        model.addAttribute("saleCount", sales.size());

        return "dashboard";
    }

    private Long getSessionUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }
}
