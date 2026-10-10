package com.atm.auth.controller;

import com.atm.auth.dto.ApiResponse;
import com.atm.auth.dto.LoginRequest;
import com.atm.auth.dto.LoginResponse;
import com.atm.auth.dto.PinChangeRequest;
import com.atm.auth.dto.RegisterRequest;
import com.atm.auth.entity.Card;
import com.atm.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST endpoints for card authentication, registration and PIN management.
 */
@Tag(name = "Auth", description = "ATM card authentication and PIN management")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticate a card and issue a JWT.
     *
     * @param request login payload
     * @return JWT and card info
     */
    @Operation(summary = "Card login", description = "Verify card + PIN and obtain a JWT")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = extractIp(httpRequest);
        LoginResponse response = authService.login(request, ip);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
    }

    /**
     * Provision a new card.
     *
     * @param request registration payload
     * @return confirmation
     */
    @Operation(summary = "Register new card", description = "Provision a new ATM card")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(
            @Valid @RequestBody RegisterRequest request) {
        Card saved = authService.registerCard(
                request.getCardNumber(),
                request.getHolderName(),
                request.getPin()
        );
        Map<String, String> data = new HashMap<>();
        data.put("cardNumber", saved.getCardNumber());
        data.put("holderName", saved.getHolderName());
        data.put("status", saved.getStatus().name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Card registered successfully"));
    }

    /**
     * Change the PIN for the authenticated card.
     *
     * @param request pin change payload
     * @return confirmation
     */
    @Operation(summary = "Change PIN", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/change-pin")
    public ResponseEntity<ApiResponse<Void>> changePin(
            @Valid @RequestBody PinChangeRequest request,
            Authentication authentication) {
        String cardNumber = authentication.getName();
        authService.changePin(request, cardNumber);
        return ResponseEntity.ok(ApiResponse.ok("PIN changed successfully"));
    }

    /**
     * Return the cardholder identity for the authenticated card.
     *
     * @return card holder name + status
     */
    @Operation(summary = "Current card info", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> me(Authentication authentication) {
        Card card = authService.getCard(authentication.getName());
        Map<String, String> data = new HashMap<>();
        data.put("cardNumber", card.getCardNumber());
        data.put("holderName", card.getHolderName());
        data.put("status", card.getStatus().name());
        data.put("expiryDate", card.getExpiryDate() != null ? card.getExpiryDate().toString() : null);
        return ResponseEntity.ok(ApiResponse.ok(data, "Card info"));
    }

    /**
     * Block the authenticated card.
     *
     * @return confirmation
     */
    @Operation(summary = "Block card", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/block")
    public ResponseEntity<ApiResponse<Void>> block(Authentication authentication) {
        authService.blockCard(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Card blocked"));
    }

    /**
     * Extract client IP (with proxy-awareness).
     */
    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
