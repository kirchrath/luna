package de.kirchrath.luna.core.dto;

import java.util.UUID;

public record NoteEventDto(UUID id, AuditDto audit, NoteEventDataDto data) {
}
