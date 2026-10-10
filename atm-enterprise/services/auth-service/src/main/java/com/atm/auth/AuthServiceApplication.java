package com.atm.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Auth Service entry point.
 *
 * <p>Responsible for ATM card authentication (PIN verification), JWT issuance,
 * PIN change operations and audit logging.</p>
 */
@SpringBootApplication
@EnableJpaAuditing
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
