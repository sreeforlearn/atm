package com.atm.auth.service;

import com.atm.auth.dto.LoginRequest;
import com.atm.auth.dto.LoginResponse;
import com.atm.auth.dto.PinChangeRequest;
import com.atm.auth.entity.AuditLog;
import com.atm.auth.entity.Card;
import com.atm.auth.exception.CustomException;
import com.atm.auth.repository.AuditLogRepository;
import com.atm.auth.repository.CardRepository;
import com.atm.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Core authentication business logic: PIN verification, JWT issuance, PIN change
 * and card blocking. All operations write to the {@code audit_logs} table.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Number of failed attempts before a card is auto-blocked. */
    public static final int MAX_FAILED_ATTEMPTS = 3;

    private final CardRepository cardRepository;
    private final AuditLogRepository auditLogRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate a card and issue a JWT.
     *
     * @param request   login payload (card number + PIN)
     * @param ipAddress originating IP (for audit)
     * @return login response with token
     * @throws CustomException on invalid credentials, blocked/expired card
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        Card card = cardRepository.findByCardNumber(request.getCardNumber())
                .orElseThrow(() -> new CustomException(
                        "Card not found", HttpStatus.NOT_FOUND));

        // Verify the card is in a usable state
        if (card.getStatus() == Card.CardStatus.BLOCKED) {
            audit(card.getCardNumber(), "LOGIN", ipAddress, "BLOCKED", "Card is blocked");
            throw new CustomException("Card is blocked. Please contact your bank.",
                    HttpStatus.LOCKED);
        }
        if (card.getStatus() == Card.CardStatus.EXPIRED || isExpired(card)) {
            card.setStatus(Card.CardStatus.EXPIRED);
            cardRepository.save(card);
            audit(card.getCardNumber(), "LOGIN", ipAddress, "EXPIRED", "Card has expired");
            throw new CustomException("Card has expired", HttpStatus.GONE);
        }

        if (!passwordEncoder.matches(request.getPin(), card.getPinHash())) {
            int attempts = card.getFailedAttempts() + 1;
            card.setFailedAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                card.setStatus(Card.CardStatus.BLOCKED);
                cardRepository.save(card);
                audit(card.getCardNumber(), "LOGIN", ipAddress, "BLOCKED",
                        "Card blocked after " + attempts + " failed attempts");
                throw new CustomException(
                        "Card blocked after " + MAX_FAILED_ATTEMPTS + " failed attempts",
                        HttpStatus.LOCKED);
            }
            cardRepository.save(card);
            audit(card.getCardNumber(), "LOGIN", ipAddress, "FAILED",
                    "Wrong PIN - attempt " + attempts + "/" + MAX_FAILED_ATTEMPTS);
            throw new CustomException(
                    "Invalid PIN. Attempts remaining: " + (MAX_FAILED_ATTEMPTS - attempts),
                    HttpStatus.UNAUTHORIZED);
        }

        // Successful login - reset counter
        card.setFailedAttempts(0);
        cardRepository.save(card);

        String token = jwtUtil.generateToken(card.getCardNumber());
        audit(card.getCardNumber(), "LOGIN", ipAddress, "SUCCESS", "Login successful");

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationMs() / 1000)
                .cardNumber(card.getCardNumber())
                .holderName(card.getHolderName())
                .build();
    }

    /**
     * Change a card's PIN after verifying the old one.
     *
     * @param request    pin change payload
     * @param cardNumber the card number (extracted from JWT)
     * @throws CustomException on mismatched old PIN or confirmation
     */
    @Transactional
    public void changePin(PinChangeRequest request, String cardNumber) {
        if (!request.getNewPin().equals(request.getConfirmPin())) {
            throw new CustomException("New PIN and confirmation do not match",
                    HttpStatus.BAD_REQUEST);
        }
        if (request.getOldPin().equals(request.getNewPin())) {
            throw new CustomException("New PIN must differ from the old PIN",
                    HttpStatus.BAD_REQUEST);
        }

        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new CustomException("Card not found", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPin(), card.getPinHash())) {
            audit(cardNumber, "PIN_CHANGE", null, "FAILED", "Old PIN mismatch");
            throw new CustomException("Old PIN is incorrect", HttpStatus.UNAUTHORIZED);
        }

        card.setPinHash(passwordEncoder.encode(request.getNewPin()));
        cardRepository.save(card);
        audit(cardNumber, "PIN_CHANGE", null, "SUCCESS", "PIN changed successfully");
    }

    /**
     * Register (provision) a new card.
     *
     * @param cardNumber 16-digit card number
     * @param holderName cardholder name
     * @param pin        plain PIN (will be hashed)
     * @return saved card (without pinHash in logs)
     * @throws CustomException if card already exists
     */
    @Transactional
    public Card registerCard(String cardNumber, String holderName, String pin) {
        if (cardRepository.existsByCardNumber(cardNumber)) {
            throw new CustomException("Card already registered: " + cardNumber,
                    HttpStatus.CONFLICT);
        }
        Card card = Card.builder()
                .cardNumber(cardNumber)
                .pinHash(passwordEncoder.encode(pin))
                .holderName(holderName)
                .status(Card.CardStatus.ACTIVE)
                .failedAttempts(0)
                .expiryDate(LocalDate.now().plusYears(5))
                .build();
        Card saved = cardRepository.save(card);
        audit(cardNumber, "REGISTER", null, "SUCCESS", "Card registered");
        return saved;
    }

    /**
     * Block a card explicitly (user/admin action).
     *
     * @param cardNumber card to block
     * @throws CustomException if the card doesn't exist
     */
    @Transactional
    public void blockCard(String cardNumber) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new CustomException("Card not found", HttpStatus.NOT_FOUND));
        card.setStatus(Card.CardStatus.BLOCKED);
        cardRepository.save(card);
        audit(cardNumber, "BLOCK", null, "SUCCESS", "Card blocked");
    }

    /**
     * Look up a card by its number (returns sanitized data only - no pinHash).
     *
     * @param cardNumber card number
     * @return card entity
     * @throws CustomException if not found
     */
    @Transactional(readOnly = true)
    public Card getCard(String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new CustomException("Card not found", HttpStatus.NOT_FOUND));
    }

    /**
     * Persist an audit log entry.
     */
    private void audit(String cardNumber, String action, String ip, String status, String details) {
        AuditLog log = AuditLog.builder()
                .cardNumber(cardNumber)
                .action(action)
                .ipAddress(ip)
                .status(status)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    /**
     * Check whether the card has passed its expiry date.
     */
    private boolean isExpired(Card card) {
        return card.getExpiryDate() != null
                && card.getExpiryDate().isBefore(LocalDate.now());
    }
}
