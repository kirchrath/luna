package de.kirchrath.luna.core.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reference to an element that luna-core created from an inbound event")
public record ElementReferenceDto(
        @Schema(description = "ID of the element; equals the id of the event published on Kafka",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(description = "Type of the element", requiredMode = Schema.RequiredMode.REQUIRED)
        LunaElementTypeDto type) {
}
