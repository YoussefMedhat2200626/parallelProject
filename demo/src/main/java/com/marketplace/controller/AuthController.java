package com.marketplace.controller;

import com.marketplace.entity.User;
import com.marketplace.service.TwoFactorService;
import com.marketplace.service.UserService;
import com.marketplace.entity.OtpCode.OtpPurpose;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;
    private final TwoFactorService twoFactorService;

    public AuthController(UserService userService, TwoFactorService twoFactorService) {
        this.userService = userService;
        this.twoFactorService = twoFactorService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        HttpSession session, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userService.authenticate(username, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // BLOCK: account exists but OTP never verified
            if (!twoFactorService.isUserVerified(user.getUserId())) {
                // Re-seed the pending state so they can still verify
                session.setAttribute("pendingRegistrationUserId", user.getUserId());
                // Re-send OTP so it isn't expired
                twoFactorService.generateOtpWithEmail(user.getUserId(), user.getEmail(), OtpPurpose.ACCOUNT_CREATE);
                redirectAttributes.addFlashAttribute("error", "Please verify your email first. A new code has been sent.");
                redirectAttributes.addFlashAttribute("showOtp", true);
                redirectAttributes.addFlashAttribute("username", username);
                redirectAttributes.addFlashAttribute("email", user.getEmail());
                redirectAttributes.addFlashAttribute("fullName", user.getFullName());
                return "redirect:/register";
            }

            session.setAttribute("userId", user.getUserId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("fullName", user.getFullName());
            return "redirect:/dashboard";
        }
        redirectAttributes.addFlashAttribute("error", "Invalid username or password");
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String email,
                           @RequestParam String password, @RequestParam String fullName,
                           @RequestParam(required = false) String otpCode,
                           HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            // Check if OTP verification is pending
            Long pendingUserId = (Long) session.getAttribute("pendingRegistrationUserId");
            if (pendingUserId != null && otpCode != null) {
                // Verify OTP
                boolean valid = twoFactorService.validateOtp(pendingUserId, otpCode, OtpPurpose.ACCOUNT_CREATE);
                if (valid) {
                    session.removeAttribute("pendingRegistrationUserId");
                    session.setAttribute("userId", pendingUserId);
                    User user = userService.findById(pendingUserId).orElseThrow();
                    session.setAttribute("username", user.getUsername());
                    session.setAttribute("fullName", user.getFullName());
                    redirectAttributes.addFlashAttribute("success", "Account created successfully!");
                    return "redirect:/dashboard";
                } else {
                    redirectAttributes.addFlashAttribute("error", "Invalid or expired OTP code");
                    redirectAttributes.addFlashAttribute("showOtp", true);
                    return "redirect:/register";
                }
            }

            // Register user
            User user = userService.register(username, email, password, fullName);
            // Generate 2FA OTP for account creation verification
            twoFactorService.generateOtpWithEmail(user.getUserId(), email, OtpPurpose.ACCOUNT_CREATE);
            session.setAttribute("pendingRegistrationUserId", user.getUserId());
            redirectAttributes.addFlashAttribute("success", "Account created! A verification code has been sent to your email.");
            redirectAttributes.addFlashAttribute("showOtp", true);
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("fullName", fullName);
            return "redirect:/register";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
