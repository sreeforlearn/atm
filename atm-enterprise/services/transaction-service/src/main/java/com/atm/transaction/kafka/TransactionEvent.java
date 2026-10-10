package com.atm.transaction.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event payload published to the {@code transaction-events} Kafka topic
 * for downstream consumers (notification-service, fraud-service, analytics, etc.).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String referenceNumber;
    private String type;
    private BigDecimal amount;
    private String fromAccount;
    private String toAccount;
    private String status;
    private LocalDateTime timestamp;
}
