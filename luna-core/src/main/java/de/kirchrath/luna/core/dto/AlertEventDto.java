package de.kirchrath.luna.core.dto;

import java.util.UUID;

public record AlertEventDto(UUID id, AuditDto audit, AlertEventDataDto data) {
}
