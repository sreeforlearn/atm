package com.atm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for registering a new card.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Card number is required")
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 digits")
    @Pattern(regexp = "^\\d{16}$", message = "Card number must contain only digits")
    private String cardNumber;

    @NotBlank(message = "Holder name is required")
    @Size(min = 2, max = 120, message = "Holder name must be between 2 and 120 characters")
    private String holderName;

    @NotBlank(message = "PIN is required")
    @Size(min = 4, max = 4, message = "PIN must be exactly 4 digits")
    @Pattern(regexp = "^\\d{4}$", message = "PIN must contain only digits")
    private String pin;
}
