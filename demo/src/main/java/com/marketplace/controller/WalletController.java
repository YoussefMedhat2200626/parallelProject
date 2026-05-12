package com.marketplace.controller;

import com.marketplace.service.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public String walletPage(HttpSession session, Model model) {
        Long userId = getSessionUserId(session);
        if (userId == null) return "redirect:/login";
        model.addAttribute("wallet", walletService.getWallet(userId));
        model.addAttribute("deposits", walletService.getDepositHistory(userId));
        return "wallet";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam double amount, HttpSession session,
                          RedirectAttributes redirectAttributes) {
        Long userId = getSessionUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            long amountCents = Math.round(amount * 100);
            walletService.deposit(userId, amountCents);
            redirectAttributes.addFlashAttribute("success",
                    String.format("$%.2f deposited successfully!", amount));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/wallet";
    }

    private Long getSessionUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }
}
