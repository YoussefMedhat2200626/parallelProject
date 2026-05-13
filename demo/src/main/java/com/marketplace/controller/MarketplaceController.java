package com.marketplace.controller;

import com.marketplace.entity.Item;
import com.marketplace.entity.OtpCode.OtpPurpose;
import com.marketplace.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class MarketplaceController {

    private final ItemService itemService;
    private final TransactionService transactionService;
    private final InventoryService inventoryService;
    private final TwoFactorService twoFactorService;
    private final UserService userService;

    public MarketplaceController(ItemService itemService, TransactionService transactionService,
                                 InventoryService inventoryService, TwoFactorService twoFactorService,
                                 UserService userService) {
        this.itemService = itemService;
        this.transactionService = transactionService;
        this.inventoryService = inventoryService;
        this.twoFactorService = twoFactorService;
        this.userService = userService;
    }

    @GetMapping("/search")
    public String searchPage(@RequestParam(required = false) String q,
                             HttpSession session, Model model) {
        Long userId = getSessionUserId(session);
        if (userId == null) return "redirect:/login";

        List<Item> items;
        if (q != null && !q.isBlank()) {
            items = itemService.searchItems(q, userId);
            model.addAttribute("query", q);
        } else {
            items = itemService.browseItems(userId);
        }

        // Attach inventory info
        for (Item item : items) {
            int available = inventoryService.getAvailableQuantity(item.getItemId());
            model.addAttribute("inv_" + item.getItemId(), available);
        }

        model.addAttribute("items", items);
        return "search";
    }

    @GetMapping("/buy/{itemId}")
    public String buyPage(@PathVariable Long itemId, HttpSession session, Model model) {
        Long userId = getSessionUserId(session);
        if (userId == null) return "redirect:/login";

        Item item = itemService.findById(itemId).orElse(null);
        if (item == null) return "redirect:/search";

        int available = inventoryService.getAvailableQuantity(itemId);
        String sellerName = userService.findById(item.getSellerId())
                .map(u -> u.getFullName()).orElse("Unknown");

        // Generate OTP for purchase verification (sent via email)
        twoFactorService.generateOtp(userId, OtpPurpose.PURCHASE);

        model.addAttribute("item", item);
        model.addAttribute("available", available);
        model.addAttribute("sellerName", sellerName);
        return "purchase";
    }

    @PostMapping("/buy/{itemId}")
    public String executePurchase(@PathVariable Long itemId,
                                 @RequestParam int quantity,
                                 @RequestParam String otpCode,
                                 HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = getSessionUserId(session);
        if (userId == null) return "redirect:/login";

        try {
            // Verify 2FA
            boolean otpValid = twoFactorService.validateOtp(userId, otpCode, OtpPurpose.PURCHASE);
            if (!otpValid) {
                redirectAttributes.addFlashAttribute("error", "Invalid or expired verification code");
                return "redirect:/buy/" + itemId;
            }

            transactionService.purchaseItem(userId, itemId, quantity);
            redirectAttributes.addFlashAttribute("success", "Purchase completed successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/buy/" + itemId;
        }
    }

    private Long getSessionUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }
}
