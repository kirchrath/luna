package de.kirchrath.luna.core.converter;

import java.util.UUID;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.data.entity.ContractElement;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.ContractEventDataDto;
import de.kirchrath.luna.core.dto.ContractEventDto;

@Component
public class ContractElementConverter {

    private final ContractElementMapper contractElementMapper;

    public ContractElementConverter(ContractElementMapper contractElementMapper) {
        this.contractElementMapper = contractElementMapper;
    }

    public ContractElement toEntity(UUID outboundEventId, ContractEventDataDto data) {
        return contractElementMapper.toEntity(outboundEventId, data);
    }

    public ContractEventDto toEventDto(OutboundEvent outboundEvent, ContractElement element) {
        return contractElementMapper.toEventDto(outboundEvent, element);
    }
}
