package de.kirchrath.luna.mail.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailAttachmentDto(
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String name,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String contentType,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long size) {
}
