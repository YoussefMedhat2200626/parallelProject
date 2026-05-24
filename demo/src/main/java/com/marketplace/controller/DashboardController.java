package com.marketplace.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.marketplace.entity.Inventory;
import com.marketplace.entity.Item;
import com.marketplace.entity.Transaction;
import com.marketplace.entity.User;
import com.marketplace.entity.Wallet;
import com.marketplace.service.InventoryService;
import com.marketplace.service.ItemService;
import com.marketplace.service.TransactionService;
import com.marketplace.service.UserService;
import com.marketplace.service.WalletService;

import jakarta.servlet.http.HttpSession;

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
        Map<Long, String> itemNameMap = new java.util.HashMap<>();
        for (Transaction txn : purchases) {    
        itemService.findById(txn.getItemId())
        .ifPresent(item -> itemNameMap.put(txn.getItemId(), item.getName()));
        }
        for (Transaction txn : sales) {
            itemService.findById(txn.getItemId())
            .ifPresent(item -> itemNameMap.put(txn.getItemId(), item.getName()));
        }
        model.addAttribute("itemNameMap", itemNameMap);
        Map<Long, String> buyerNameMap = new java.util.HashMap<>();
        for (Transaction txn : sales) {
            userService.findById(txn.getBuyerId())
            .ifPresent(user2 -> buyerNameMap.put(txn.getBuyerId(), user2.getFullName()));
        }
        model.addAttribute("buyerNameMap", buyerNameMap);
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
