package de.kirchrath.luna.core.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.converter.ContractElementConverter;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.ContractElement;
import de.kirchrath.luna.core.data.repository.ContractElementRepository;
import de.kirchrath.luna.core.dto.ContractEventDataDto;
import de.kirchrath.luna.core.outbound.ElementEventPublisher;
import de.kirchrath.luna.core.outbound.ElementQualificationClient;
import jakarta.validation.Validator;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class ContractElementProcessor extends ElementProcessor<ContractEventDataDto, ContractElement> {

    private final ContractElementRepository contractElementRepository;
    private final ContractElementConverter contractElementConverter;

    public ContractElementProcessor(
            @Value("classpath:prompts/qualification/contract.st") Resource prompt,
            ElementQualificationClient elementQualificationClient,
            ElementEventPublisher elementEventPublisher,
            Validator validator,
            ContractElementRepository contractElementRepository,
            ContractElementConverter contractElementConverter) {
        super(LunaElementType.CONTRACT, "luna_core_contract", prompt, ContractEventDataDto.class,
                elementQualificationClient, elementEventPublisher, validator);
        this.contractElementRepository = contractElementRepository;
        this.contractElementConverter = contractElementConverter;
    }

    @Override
    protected ContractElement toEntity(UUID outboundEventId, ContractEventDataDto data) {
        return contractElementConverter.toEntity(outboundEventId, data);
    }

    @Override
    protected ContractElement save(ContractElement element) {
        return contractElementRepository.save(element);
    }

    @Override
    protected Object toEventDto(OutboundEvent outboundEvent, ContractElement element) {
        return contractElementConverter.toEventDto(outboundEvent, element);
    }
}
