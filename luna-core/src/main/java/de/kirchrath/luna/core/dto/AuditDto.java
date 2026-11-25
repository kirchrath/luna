package de.kirchrath.luna.core.dto;

import java.time.OffsetDateTime;

public record AuditDto(OffsetDateTime createdDate, OffsetDateTime lastModifiedDate, Long version) {
}
