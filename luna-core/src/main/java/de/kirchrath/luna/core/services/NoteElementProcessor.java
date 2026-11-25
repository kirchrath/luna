package de.kirchrath.luna.core.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.converter.NoteElementConverter;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.NoteElement;
import de.kirchrath.luna.core.data.repository.NoteElementRepository;
import de.kirchrath.luna.core.dto.NoteEventDataDto;
import de.kirchrath.luna.core.outbound.ElementEventPublisher;
import de.kirchrath.luna.core.outbound.ElementQualificationClient;
import jakarta.validation.Validator;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class NoteElementProcessor extends ElementProcessor<NoteEventDataDto, NoteElement> {

    private final NoteElementRepository noteElementRepository;
    private final NoteElementConverter noteElementConverter;

    public NoteElementProcessor(
            @Value("classpath:prompts/qualification/note.st") Resource prompt,
            ElementQualificationClient elementQualificationClient,
            ElementEventPublisher elementEventPublisher,
            Validator validator,
            NoteElementRepository noteElementRepository,
            NoteElementConverter noteElementConverter) {
        super(LunaElementType.NOTE, "luna_core_note", prompt, NoteEventDataDto.class,
                elementQualificationClient, elementEventPublisher, validator);
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
    protected Object toEventDto(OutboundEvent outboundEvent, NoteElement element) {
        return noteElementConverter.toEventDto(outboundEvent, element);
    }
}
