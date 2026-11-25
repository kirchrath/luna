package de.kirchrath.luna.core.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Invoice element published by luna-core")
public record InvoiceEventDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AuditDto audit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) InvoiceEventDataDto data) implements ElementEventDto {
}
