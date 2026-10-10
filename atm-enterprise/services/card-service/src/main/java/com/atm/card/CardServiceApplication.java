package com.atm.card;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the ATM Enterprise Card Service.
 *
 * <p>This microservice owns CRUD operations for card metadata (issue, block,
 * limit updates) and runs scheduled jobs that reset daily and monthly
 * spending trackers at midnight UTC.</p>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class CardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardServiceApplication.class, args);
    }
}
