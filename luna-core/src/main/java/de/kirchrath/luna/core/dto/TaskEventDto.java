package de.kirchrath.luna.core.dto;

import java.util.UUID;

public record TaskEventDto(UUID id, AuditDto audit, TaskEventDataDto data) {
}
