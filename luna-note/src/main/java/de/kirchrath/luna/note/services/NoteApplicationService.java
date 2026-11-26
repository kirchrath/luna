package de.kirchrath.luna.note.services;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import de.kirchrath.luna.note.dto.NoteDto;
import de.kirchrath.luna.note.dto.NoteListDto;
import de.kirchrath.luna.note.dto.NoteStatusDto;
import de.kirchrath.luna.note.dto.NoteUpdateDto;

@Service
public class NoteApplicationService {

    private final NoteDomainService noteDomainService;

    public NoteApplicationService(NoteDomainService noteDomainService) {
        this.noteDomainService = noteDomainService;
    }

    public NoteListDto listNotes(NoteStatusDto status, Pageable pageable) {
        return noteDomainService.listNotes(status, pageable);
    }

    public NoteDto getNote(UUID id) {
        return noteDomainService.getNote(id);
    }

    public NoteDto updateNote(UUID id, NoteUpdateDto update) {
        return noteDomainService.updateNote(id, update);
    }

    public NoteDto archiveNote(UUID id) {
        return noteDomainService.archiveNote(id);
    }

    public NoteDto restoreNote(UUID id) {
        return noteDomainService.restoreNote(id);
    }
}
