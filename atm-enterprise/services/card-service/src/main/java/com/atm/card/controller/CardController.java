package com.atm.card.controller;

import com.atm.card.dto.ApiResponse;
import com.atm.card.dto.BlockCardRequest;
import com.atm.card.dto.CardResponse;
import com.atm.card.dto.IssueCardRequest;
import com.atm.card.dto.UpdateLimitRequest;
import com.atm.card.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller exposing the ATM Enterprise card management endpoints.
 *
 * <p>Base path: {@code /api/cards}</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card lifecycle management (issue, block, limits, status)")
public class CardController {

    private final CardService cardService;

    /**
     * Retrieve card details by card number.
     *
     * @param cardNumber the card number
     * @return masked card response
     */
    @Operation(summary = "Fetch card details by card number")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Card found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Card not found")
    })
    @GetMapping("/{cardNumber}")
    public ResponseEntity<ApiResponse<CardResponse>> getCard(
            @Parameter(description = "Card number", required = true)
            @PathVariable String cardNumber) {
        log.info("Fetching card details for {}", cardNumber);
        CardResponse response = cardService.getCardDetails(cardNumber);
        return ResponseEntity.ok(ApiResponse.ok(response, "Card details retrieved"));
    }

    /**
     * Block an existing card.
     *
     * @param request the block request
     * @return masked card response
     */
    @Operation(summary = "Block an existing card")
    @PostMapping("/block")
    public ResponseEntity<ApiResponse<CardResponse>> blockCard(@Valid @RequestBody BlockCardRequest request) {
        log.info("Blocking card {}", request.getCardNumber());
        CardResponse response = cardService.blockCard(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Card blocked"));
    }

    /**
     * Update spending limits on a card.
     *
     * @param request the update request
     * @return masked card response
     */
    @Operation(summary = "Update card spending limits")
    @PutMapping("/limits")
    public ResponseEntity<ApiResponse<CardResponse>> updateLimits(@Valid @RequestBody UpdateLimitRequest request) {
        log.info("Updating limits for card {}", request.getCardNumber());
        CardResponse response = cardService.updateLimits(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Card limits updated"));
    }

    /**
     * Issue a new card.
     *
     * @param request the issue request
     * @return masked card response
     */
    @Operation(summary = "Issue a new card")
    @PostMapping("/issue")
    public ResponseEntity<ApiResponse<CardResponse>> issueCard(@Valid @RequestBody IssueCardRequest request) {
        log.info("Issuing new card for holder {}", request.getHolderName());
        CardResponse response = cardService.issueCard(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Card issued"));
    }

    /**
     * Health-check endpoint.
     *
     * @return simple status payload
     */
    @Operation(summary = "Card service health check")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "UP", "service", "card-service"),
                "Card service is healthy"));
    }
}
