package de.kirchrath.luna.core.services;

import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.config.PromptProperties;
import de.kirchrath.luna.core.config.TopicProperties;
import de.kirchrath.luna.core.converter.ContractElementConverter;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.ContractElement;
import de.kirchrath.luna.core.data.repository.ContractElementRepository;
import de.kirchrath.luna.core.dto.ContractEventDataDto;
import de.kirchrath.luna.core.dto.ContractEventDto;
import de.kirchrath.luna.core.outbound.ElementEventPublisher;
import de.kirchrath.luna.core.outbound.ElementQualificationClient;
import jakarta.validation.Validator;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class ContractElementProcessor extends ElementProcessor<ContractEventDataDto, ContractElement, ContractEventDto> {

    private final ContractElementRepository contractElementRepository;
    private final ContractElementConverter contractElementConverter;

    public ContractElementProcessor(
            TopicProperties topicProperties,
            PromptProperties promptProperties,
            ElementQualificationClient elementQualificationClient,
            ElementEventPublisher elementEventPublisher,
            Validator validator,
            ContractElementRepository contractElementRepository,
            ContractElementConverter contractElementConverter) {
        super(LunaElementType.CONTRACT, topicProperties.contract(), promptProperties.contract(), new ParameterizedTypeReference<>() {
        }, elementQualificationClient, elementEventPublisher, validator);
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
    protected ContractEventDto toEventDto(OutboundEvent outboundEvent, ContractElement element) {
        return contractElementConverter.toEventDto(outboundEvent, element);
    }
}
