package de.kirchrath.luna.core.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvoiceEventDataDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String issuer,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String invoiceNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull BigDecimal amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String currency,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OffsetDateTime dueDate,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String description) {
}
