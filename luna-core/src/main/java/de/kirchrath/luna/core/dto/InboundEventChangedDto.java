package de.kirchrath.luna.core.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Signal that an inbound event changed its status; clients reload it")
public record InboundEventChangedDto(
        @Schema(description = "ID of the changed inbound event", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id) {
}
