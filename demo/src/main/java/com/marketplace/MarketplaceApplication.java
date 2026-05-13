package com.marketplace;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Distributed Online Marketplace Application
 * Ain Shams University - CSE352s Parallel and Distributed Systems
 * Spring 2026 Semester
 */
@SpringBootApplication
@EnableAsync
public class MarketplaceApplication {

    public static void main(String[] args) {
        // Load .env file and set system properties for Spring Boot
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        SpringApplication.run(MarketplaceApplication.class, args);
    }
}
