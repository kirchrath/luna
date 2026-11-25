package de.kirchrath.luna.core.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;

public record ContractEventDataDto(
        @NotBlank String counterparty,
        @NotBlank String title,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        OffsetDateTime cancellationDeadline,
        String description) {
}
