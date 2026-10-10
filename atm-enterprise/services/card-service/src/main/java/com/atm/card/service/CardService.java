package com.atm.card.service;

import com.atm.card.dto.BlockCardRequest;
import com.atm.card.dto.CardResponse;
import com.atm.card.dto.IssueCardRequest;
import com.atm.card.dto.UpdateLimitRequest;
import com.atm.card.entity.Card;
import com.atm.card.entity.CardLimit;
import com.atm.card.exception.CardAlreadyBlockedException;
import com.atm.card.exception.CardNotFoundException;
import com.atm.card.repository.CardLimitRepository;
import com.atm.card.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Core business service managing card lifecycle.
 *
 * <p>Holds CRUD operations and runs scheduled jobs to reset spending
 * trackers (daily at midnight UTC, monthly on the 1st at 00:05 UTC).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("MM/yy");

    private final CardRepository cardRepository;
    private final CardLimitRepository cardLimitRepository;

    /**
     * Retrieve card details for a given card number.
     *
     * @param cardNumber the card number
     * @return masked response DTO
     * @throws CardNotFoundException if the card is not present
     */
    @Transactional(readOnly = true)
    public CardResponse getCardDetails(String cardNumber) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + cardNumber));
        return toResponse(card);
    }

    /**
     * Block an existing card and store the reason for the block.
     *
     * @param request the block request
     * @return updated card response
     * @throws CardNotFoundException     if card does not exist
     * @throws CardAlreadyBlockedException if card is already blocked
     */
    @Transactional
    public CardResponse blockCard(BlockCardRequest request) {
        Card card = cardRepository.findByCardNumber(request.getCardNumber())
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + request.getCardNumber()));
        if (card.getStatus() == Card.Status.BLOCKED) {
            throw new CardAlreadyBlockedException("Card already blocked: " + request.getCardNumber());
        }
        card.setStatus(Card.Status.BLOCKED);
        card.setBlockedAt(LocalDateTime.now());
        card.setBlockedReason(request.getReason());
        Card saved = cardRepository.save(card);
        log.info("Card {} blocked for reason: {}", saved.getCardNumber(), request.getReason());
        return toResponse(saved);
    }

    /**
     * Update spending limits on an existing card.
     *
     * @param request the update request
     * @return updated card response
     * @throws CardNotFoundException if card does not exist
     */
    @Transactional
    public CardResponse updateLimits(UpdateLimitRequest request) {
        Card card = cardRepository.findByCardNumber(request.getCardNumber())
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + request.getCardNumber()));
        card.setDailyLimit(request.getDailyLimit());
        card.setMonthlyLimit(request.getMonthlyLimit());
        Card saved = cardRepository.save(card);
        log.info("Limits updated for card {}: daily={}, monthly={}",
                saved.getCardNumber(), saved.getDailyLimit(), saved.getMonthlyLimit());
        return toResponse(saved);
    }

    /**
     * Issue a new card.
     *
     * <p>Generates a random 16-character card number, sets a default 5-year
     * expiry, initializes spending trackers, and persists the card.</p>
     *
     * @param request the issue request
     * @return the new card response (with masked number)
     */
    @Transactional
    public CardResponse issueCard(IssueCardRequest request) {
        String cardNumber = generateCardNumber();
        Card card = Card.builder()
                .cardNumber(cardNumber)
                .holderName(request.getHolderName())
                .status(Card.Status.ACTIVE)
                .cardType(Card.CardType.valueOf(request.getCardType().name()))
                .expiryDate(LocalDate.now().plusYears(5))
                .dailyLimit(request.getDailyLimit())
                .monthlyLimit(request.getMonthlyLimit())
                .build();
        Card saved = cardRepository.save(card);

        // Initialize spending tracker
        CardLimit limit = CardLimit.builder()
                .cardNumber(cardNumber)
                .dailySpent(BigDecimal.ZERO)
                .monthlySpent(BigDecimal.ZERO)
                .lastResetDate(LocalDate.now())
                .build();
        cardLimitRepository.save(limit);

        log.info("Issued new card {} for holder {}", saved.getCardNumber(), saved.getHolderName());
        return toResponse(saved);
    }

    /**
     * Scheduled job that resets daily spent counters at midnight UTC daily.
     *
     * <p>Cron expression {@code "0 0 0 * * ?"} fires every day at 00:00 UTC.</p>
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
    @Transactional
    public void resetDailyLimits() {
        log.info("Running daily limit reset job at {}", LocalDateTime.now());
        int affected = cardLimitRepository.resetAllDailySpent(LocalDate.now());
        log.info("Daily reset job completed: {} card limit rows affected", affected);
    }

    /**
     * Scheduled job that resets monthly spent counters at 00:05 UTC on the 1st of each month.
     */
    @Scheduled(cron = "0 5 0 1 * ?", zone = "UTC")
    @Transactional
    public void resetMonthlyLimits() {
        log.info("Running monthly limit reset job at {}", LocalDateTime.now());
        int affected = cardLimitRepository.resetAllMonthlySpent();
        log.info("Monthly reset job completed: {} card limit rows affected", affected);
    }

    /**
     * Scheduled job (runs daily at 01:00 UTC) that flips expired cards
     * (i.e. cards whose expiry date has passed) into EXPIRED status.
     */
    @Scheduled(cron = "0 0 1 * * ?", zone = "UTC")
    @Transactional
    public void checkCardStatus() {
        log.info("Running card status check job at {}", LocalDateTime.now());
        LocalDate today = LocalDate.now();
        cardRepository.findAll().forEach(card -> {
            if (card.getStatus() == Card.Status.ACTIVE && card.getExpiryDate().isBefore(today)) {
                card.setStatus(Card.Status.EXPIRED);
                cardRepository.save(card);
                log.info("Card {} auto-expired (expiry date {})", card.getCardNumber(), card.getExpiryDate());
            }
        });
    }

    /* ---------- helpers ---------- */

    private CardResponse toResponse(Card card) {
        CardLimit limit = cardLimitRepository.findByCardNumber(card.getCardNumber())
                .orElseGet(() -> CardLimit.builder()
                        .dailySpent(BigDecimal.ZERO)
                        .monthlySpent(BigDecimal.ZERO)
                        .build());
        return CardResponse.builder()
                .cardNumber(CardResponse.mask(card.getCardNumber()))
                .holderName(card.getHolderName())
                .status(card.getStatus())
                .cardType(card.getCardType())
                .expiryDate(card.getExpiryDate().format(EXPIRY_FORMAT))
                .dailyLimit(card.getDailyLimit())
                .monthlyLimit(card.getMonthlyLimit())
                .dailySpent(limit.getDailySpent())
                .monthlySpent(limit.getMonthlySpent())
                .build();
    }

    private String generateCardNumber() {
        // 16-digit pseudo-random card number; in production use a tokenization service.
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
