package com.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Distributed Online Marketplace Application
 * Ain Shams University - CSE352s Parallel and Distributed Systems
 * Spring 2026 Semester
 */
@SpringBootApplication
public class MarketplaceApplication {

    public static void main(String[] args) {
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        
        SpringApplication.run(MarketplaceApplication.class, args);
    }
}
