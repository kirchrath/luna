package de.kirchrath.luna.core.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record InboundEventListDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<InboundEventListItemDto> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalCount) {
}
