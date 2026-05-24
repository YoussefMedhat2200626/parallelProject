package com.marketplace.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.marketplace.entity.Inventory;
import com.marketplace.entity.Item;
import com.marketplace.service.CsvImportService;
import com.marketplace.service.CsvImportService.CsvImportResult;
import com.marketplace.service.InventoryService;
import com.marketplace.service.ItemService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;
    private final InventoryService inventoryService;
    private final CsvImportService csvImportService;

    public ItemController(ItemService itemService, InventoryService inventoryService,
                          CsvImportService csvImportService) {
        this.itemService = itemService;
        this.inventoryService = inventoryService;
        this.csvImportService = csvImportService;
    }

    @GetMapping
    public String myItems(HttpSession session, Model model) {
        Long userId = getSessionUserId(session);
        if (userId == null) return "redirect:/login";
        List<Item> items = itemService.getSellerItems(userId);
        model.addAttribute("items", items);
        Map<Long, Integer> inventoryMap = new java.util.HashMap<>();
        for (Item item : items) {
        inventoryService.getByItemId(item.getItemId())
            .ifPresent(inv -> inventoryMap.put(item.getItemId(), inv.getQuantity()));
    }
    model.addAttribute("inventoryMap", inventoryMap);
    return "items/list";
    }

    @GetMapping("/create")
    public String createForm(HttpSession session) {
        if (getSessionUserId(session) == null) return "redirect:/login";
        return "items/create";
    }

    @PostMapping("/create")
    public String createItem(@RequestParam String name, @RequestParam String description,
                             @RequestParam String brand, @RequestParam String category,
                             @RequestParam double price, @RequestParam int quantity,
                             HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = getSessionUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            long priceCents = Math.round(price * 100);
            itemService.createItem(userId, name, description, brand, category, priceCents, quantity);
            redirectAttributes.addFlashAttribute("success", "Item created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/items";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        Long userId = getSessionUserId(session);
        if (userId == null) return "redirect:/login";
        Optional<Item> item = itemService.findById(id);
        if (item.isEmpty() || !item.get().getSellerId().equals(userId)) {
            return "redirect:/items";
        }
        model.addAttribute("item", item.get());
        if (item.get().getStatus() == com.marketplace.entity.Item.ItemStatus.SOLD) {
            model.addAttribute("soldWarning", "This item has been sold and cannot be edited.");
        }
        Optional<Inventory> inv = inventoryService.getByItemId(id);
        inv.ifPresent(inventory -> model.addAttribute("inventory", inventory));
        return "items/edit";
        }

    @PostMapping("/edit/{id}")
    public String editItem(@PathVariable Long id, @RequestParam String name,
                           @RequestParam String description, @RequestParam String brand,
                           @RequestParam String category, @RequestParam double price,
                           @RequestParam int quantity, HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Long userId = getSessionUserId(session);
        if (userId == null) return "redirect:/login";
        try {
        long priceCents = Math.round(price * 100);
        itemService.updateItem(id, userId, name, description, brand, category, priceCents);
        if (inventoryService.getByItemId(id).isPresent()) {
            inventoryService.updateQuantity(id, quantity);
            redirectAttributes.addFlashAttribute("success", "Item updated!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Inventory record not found");
        }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/items";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id, HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Long userId = getSessionUserId(session);
        if (userId == null) return "redirect:/login";
        try {
            itemService.removeItem(id, userId);
            redirectAttributes.addFlashAttribute("success", "Item removed!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/items";
    }

    @GetMapping("/import")
    public String importPage(HttpSession session) {
        if (getSessionUserId(session) == null) return "redirect:/login";
        return "items/import";
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam("file") MultipartFile file, HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Long userId = getSessionUserId(session);
        if (userId == null) return "redirect:/login";
        CsvImportResult result = csvImportService.importItems(file, userId);
        redirectAttributes.addFlashAttribute("importResult", result);
        redirectAttributes.addFlashAttribute("success",
                result.successCount() + " items imported, " + result.failCount() + " failed");
        return "redirect:/items/import";
    }

    private Long getSessionUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }
}
