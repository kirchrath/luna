package de.kirchrath.luna.core.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Manual input in a list together with its processing status")
public record ManualInboundEventListItemDto(
        @Schema(description = "ID of the inbound event", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(description = "Free text of the input", requiredMode = Schema.RequiredMode.REQUIRED)
        String text,

        @Schema(description = "Hint which element should result from the input, if given")
        LunaElementTypeDto typeHint,

        @Schema(description = "Processing status of the input", requiredMode = Schema.RequiredMode.REQUIRED)
        InboundEventStatusDto status,

        @Schema(description = "Point in time the input was received", requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime createdDate,

        @Schema(description = "Elements created from the input", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ElementReferenceDto> elements) {
}
