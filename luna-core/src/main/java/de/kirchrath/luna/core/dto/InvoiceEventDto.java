package de.kirchrath.luna.core.dto;

import java.util.UUID;

public record InvoiceEventDto(UUID id, AuditDto audit, InvoiceEventDataDto data) {
}
