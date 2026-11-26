package de.kirchrath.luna.mail.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailEventDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        AuditDto audit,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        EmailEventDataDto data) {
}
