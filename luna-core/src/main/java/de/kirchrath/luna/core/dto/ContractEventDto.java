package de.kirchrath.luna.core.dto;

import java.util.UUID;

public record ContractEventDto(UUID id, AuditDto audit, ContractEventDataDto data) {
}
