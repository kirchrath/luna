package de.kirchrath.luna.core.services;

import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.config.PromptProperties;
import de.kirchrath.luna.core.config.TopicProperties;
import de.kirchrath.luna.core.converter.NoteElementConverter;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.NoteElement;
import de.kirchrath.luna.core.data.repository.NoteElementRepository;
import de.kirchrath.luna.core.dto.NoteEventDataDto;
import de.kirchrath.luna.core.dto.NoteEventDto;
import de.kirchrath.luna.core.outbound.ElementEventPublisher;
import de.kirchrath.luna.core.outbound.ElementQualificationClient;
import jakarta.validation.Validator;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class NoteElementProcessor extends ElementProcessor<NoteEventDataDto, NoteElement, NoteEventDto> {

    private final NoteElementRepository noteElementRepository;
    private final NoteElementConverter noteElementConverter;

    public NoteElementProcessor(
            TopicProperties topicProperties,
            PromptProperties promptProperties,
            ElementQualificationClient elementQualificationClient,
            ElementEventPublisher elementEventPublisher,
            Validator validator,
            NoteElementRepository noteElementRepository,
            NoteElementConverter noteElementConverter) {
        super(LunaElementType.NOTE, topicProperties.note(), promptProperties.note(), new ParameterizedTypeReference<>() {
        }, elementQualificationClient, elementEventPublisher, validator);
        this.noteElementRepository = noteElementRepository;
        this.noteElementConverter = noteElementConverter;
    }

    @Override
    protected NoteElement toEntity(UUID outboundEventId, NoteEventDataDto data) {
        return noteElementConverter.toEntity(outboundEventId, data);
    }

    @Override
    protected NoteElement save(NoteElement element) {
        return noteElementRepository.save(element);
    }

    @Override
    protected NoteEventDto toEventDto(OutboundEvent outboundEvent, NoteElement element) {
        return noteElementConverter.toEventDto(outboundEvent, element);
    }
}
