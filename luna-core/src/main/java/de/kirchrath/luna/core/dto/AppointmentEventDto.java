package de.kirchrath.luna.core.dto;

import java.util.UUID;

public record AppointmentEventDto(UUID id, AuditDto audit, AppointmentEventDataDto data) {
}
