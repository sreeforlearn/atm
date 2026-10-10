package com.atm.transaction.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Produces {@link TransactionEvent} messages onto the Kafka
 * {@code transaction-events} topic.
 *
 * <p>Send is asynchronous — failures are logged but do not propagate to the
 * caller to avoid rolling back committed DB transactions.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Value("${app.kafka.topic:transaction-events}")
    private String topic;

    /**
     * Publish the given event to the configured Kafka topic.
     *
     * @param event the event payload
     */
    public void send(TransactionEvent event) {
        String key = event.getReferenceNumber();
        log.debug("Publishing transaction event ref={} type={} amount={}",
                key, event.getType(), event.getAmount());

        CompletableFuture<SendResult<String, TransactionEvent>> future = kafkaTemplate.send(topic, key, event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish transaction event ref={}: {}", key, ex.getMessage(), ex);
            } else if (result != null) {
                log.info("Transaction event published ref={} partition={} offset={}",
                        key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
