package de.kirchrath.luna.core.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record ManualInboundEventListDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ManualInboundEventListItemDto> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalCount) {
}
