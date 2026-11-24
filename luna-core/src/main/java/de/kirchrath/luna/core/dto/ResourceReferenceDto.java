package de.kirchrath.luna.core.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reference to a created resource")
public record ResourceReferenceDto(
        @Schema(description = "ID of the created resource", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id) {
}
