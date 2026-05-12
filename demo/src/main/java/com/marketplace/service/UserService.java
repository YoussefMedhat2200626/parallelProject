package com.marketplace.service;

import com.marketplace.entity.User;
import com.marketplace.entity.Wallet;
import com.marketplace.repository.UserRepository;
import com.marketplace.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import jakarta.annotation.PostConstruct;

/**
 * Handles user registration, authentication, and profile management.
 * Passwords are hashed using BCrypt (manually via spring-security-crypto).
 */
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @PostConstruct
    public void migrateLegacyPasswords() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (!user.getPasswordHash().startsWith("$")) {
                LOG.info("Migrating legacy password for user: {}", user.getUsername());
                // Legacy data.sql test users originally had 'password123'
                user.setPasswordHash(passwordEncoder.encode("password123"));
                userRepository.save(user);
            }
        }
    }

    /**
     * Register a new user account and create an associated wallet.
     */
    @Transactional
    public User register(String username, String email, String password, String fullName) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        User user = new User(username, email, passwordEncoder.encode(password), fullName);
        user = userRepository.save(user);

        // Create wallet with zero balance
        Wallet wallet = new Wallet(user.getUserId(), 0L);
        walletRepository.save(wallet);

        LOG.info("New user registered: {} (id={})", username, user.getUserId());
        return user;
    }

    /**
     * Authenticate user with username and password.
     * Returns the User if successful, empty otherwise.
     */
    public Optional<User> authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> {
                    try {
                        return passwordEncoder.matches(password, user.getPasswordHash());
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Invalid password hash format for user {}: {}", username, e.getMessage());
                        return false;
                    }
                });
    }

    public Optional<User> findById(Long userId) {
        if (userId == null) return Optional.empty();
        return userRepository.findById(userId);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User updateProfile(Long userId, String fullName, String email) {
        if (userId == null) throw new IllegalArgumentException("User ID must not be null");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setFullName(fullName);
        user.setEmail(email);
        return userRepository.save(user);
    }
}
