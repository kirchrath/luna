package de.kirchrath.luna.alert.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record AlertListDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AlertListItemDto> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalCount) {
}
