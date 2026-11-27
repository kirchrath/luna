package de.kirchrath.luna.core.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An input of any source: manual inputs carry {@code text} and {@code typeHint}, emails {@code subject} and
 * {@code from}.
 */
public record InboundEventListItemDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        InboundEventSourceDto source,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String text,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LunaElementTypeDto typeHint,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String subject,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String from,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        InboundEventStatusDto status,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime createdDate,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<ElementReferenceDto> elements) {
}
