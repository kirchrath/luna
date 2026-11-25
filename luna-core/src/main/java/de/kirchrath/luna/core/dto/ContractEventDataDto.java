package de.kirchrath.luna.core.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ContractEventDataDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String counterparty,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OffsetDateTime startDate,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OffsetDateTime endDate,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OffsetDateTime cancellationDeadline,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String description) {
}
