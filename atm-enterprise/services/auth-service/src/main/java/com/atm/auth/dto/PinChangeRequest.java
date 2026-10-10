package com.atm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for PIN change operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinChangeRequest {

    @NotBlank(message = "Old PIN is required")
    @Size(min = 4, max = 4, message = "Old PIN must be exactly 4 digits")
    @Pattern(regexp = "^\\d{4}$", message = "Old PIN must contain only digits")
    private String oldPin;

    @NotBlank(message = "New PIN is required")
    @Size(min = 4, max = 4, message = "New PIN must be exactly 4 digits")
    @Pattern(regexp = "^\\d{4}$", message = "New PIN must contain only digits")
    private String newPin;

    @NotBlank(message = "Confirmation PIN is required")
    @Size(min = 4, max = 4, message = "Confirm PIN must be exactly 4 digits")
    @Pattern(regexp = "^\\d{4}$", message = "Confirm PIN must contain only digits")
    private String confirmPin;
}
