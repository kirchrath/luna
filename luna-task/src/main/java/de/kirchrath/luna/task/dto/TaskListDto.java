package de.kirchrath.luna.task.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record TaskListDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<TaskListItemDto> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalCount) {
}
