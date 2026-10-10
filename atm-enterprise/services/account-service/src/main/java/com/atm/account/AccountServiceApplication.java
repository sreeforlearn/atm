package com.atm.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Account Service entry point.
 *
 * <p>Manages bank accounts, balances, and the audit trail of credits/debits
 * against each account.</p>
 */
@SpringBootApplication
@EnableJpaAuditing
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
