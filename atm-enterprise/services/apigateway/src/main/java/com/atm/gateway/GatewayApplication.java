package com.atm.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway entry point.
 *
 * <p>Edge service for the ATM Enterprise platform. Routes requests to the
 * downstream microservices (auth, account, transaction, card) after JWT
 * validation, IP-based rate limiting and CORS handling.</p>
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
