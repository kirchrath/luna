package de.kirchrath.luna.core.converter;

import java.util.UUID;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.data.entity.NoteElement;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.NoteEventDataDto;
import de.kirchrath.luna.core.dto.NoteEventDto;

@Component
public class NoteElementConverter {

    private final NoteElementMapper noteElementMapper;

    public NoteElementConverter(NoteElementMapper noteElementMapper) {
        this.noteElementMapper = noteElementMapper;
    }

    public NoteElement toEntity(UUID outboundEventId, NoteEventDataDto data) {
        return noteElementMapper.toEntity(outboundEventId, data);
    }

    public NoteEventDto toEventDto(OutboundEvent outboundEvent, NoteElement element) {
        return noteElementMapper.toEventDto(outboundEvent, element);
    }
}
