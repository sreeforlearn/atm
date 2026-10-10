package com.atm.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for blocking an existing card.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockCardRequest {

    @NotBlank(message = "Card number is required")
    @Size(min = 8, max = 32, message = "Card number must be between 8 and 32 characters")
    private String cardNumber;

    @NotBlank(message = "Block reason is required")
    @Size(max = 256, message = "Reason cannot exceed 256 characters")
    private String reason;
}
