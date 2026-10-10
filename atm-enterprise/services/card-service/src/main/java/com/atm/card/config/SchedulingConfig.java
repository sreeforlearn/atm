package com.atm.card.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's @Scheduled support for daily/monthly card limit resets
 * and the card status sweep.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
