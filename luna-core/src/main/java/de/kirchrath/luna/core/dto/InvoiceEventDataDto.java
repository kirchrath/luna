package de.kirchrath.luna.core.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvoiceEventDataDto(
        @NotBlank String issuer,
        String invoiceNumber,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        OffsetDateTime dueDate,
        String description) {
}
