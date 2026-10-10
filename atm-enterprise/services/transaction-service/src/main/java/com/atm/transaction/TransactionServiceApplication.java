package com.atm.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the ATM Enterprise Transaction Service.
 *
 * <p>This microservice handles all monetary transactions including withdrawals,
 * deposits, transfers, and PIN changes. It integrates with the account-service
 * for balance management and publishes transaction events to Kafka for downstream
 * consumers (notification-service, fraud-service, etc.).</p>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableKafka
@EnableScheduling
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
