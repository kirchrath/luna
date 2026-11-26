package de.kirchrath.luna.core.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record ManualInboundEventListItemDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String text,

        LunaElementTypeDto typeHint,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        InboundEventStatusDto status,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime createdDate,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<ElementReferenceDto> elements) {
}
