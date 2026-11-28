package de.kirchrath.luna.mail.dto;

import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailEventDataDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String account,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String address,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LifeAreaDto area,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String messageId,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String from,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> to,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String subject,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        OffsetDateTime sentDate,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime receivedDate,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String text,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean textTruncated,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<EmailAttachmentDto> attachments) {
}
