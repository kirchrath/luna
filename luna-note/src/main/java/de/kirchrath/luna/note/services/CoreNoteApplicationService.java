package de.kirchrath.luna.note.services;

import org.springframework.stereotype.Service;

import de.kirchrath.luna.note.dto.core.NoteEventDto;

@Service
public class CoreNoteApplicationService {

    private final CoreNoteDomainService coreNoteDomainService;

    public CoreNoteApplicationService(CoreNoteDomainService coreNoteDomainService) {
        this.coreNoteDomainService = coreNoteDomainService;
    }

    public void process(NoteEventDto event) {
        coreNoteDomainService.process(event);
    }
}
