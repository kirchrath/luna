package de.kirchrath.luna.note.services;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.note.converter.NoteConverter;
import de.kirchrath.luna.note.data.entity.Note;
import de.kirchrath.luna.note.data.entity.NoteStatus;
import de.kirchrath.luna.note.data.repository.NoteRepository;
import de.kirchrath.luna.note.dto.NoteDto;
import de.kirchrath.luna.note.dto.NoteListDto;
import de.kirchrath.luna.note.dto.NoteStatusDto;
import de.kirchrath.luna.note.dto.NoteUpdateDto;
import de.kirchrath.luna.note.exception.NoteNotFoundException;
import de.kirchrath.luna.note.exception.NoteVersionConflictException;

@Service
@Transactional
public class NoteDomainService {

    /** Newest first. */
    private static final Sort LIST_ORDER = Sort.by(Sort.Order.desc(Note::createdDate));

    private final NoteRepository noteRepository;
    private final NoteConverter noteConverter;
    private final TimeProvider timeProvider;
    private final ApplicationEventPublisher eventPublisher;

    public NoteDomainService(
            NoteRepository noteRepository,
            NoteConverter noteConverter,
            TimeProvider timeProvider,
            ApplicationEventPublisher eventPublisher) {
        this.noteRepository = noteRepository;
        this.noteConverter = noteConverter;
        this.timeProvider = timeProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public NoteListDto listNotes(NoteStatusDto status, Pageable pageable) {
        var pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), LIST_ORDER);
        var notes = status == null
                ? noteRepository.findAll(pageRequest)
                : noteRepository.findByStatus(noteConverter.toEntity(status), pageRequest);
        return new NoteListDto(
                notes.map(noteConverter::toListItemDto).getContent(), notes.getTotalPages(), notes.getTotalElements());
    }

    @Transactional(readOnly = true)
    public NoteDto getNote(UUID id) {
        return noteConverter.toDto(findNote(id));
    }

    public NoteDto updateNote(UUID id, NoteUpdateDto update) {
        var note = findNote(id);
        if (!note.version().equals(update.version())) {
            throw new NoteVersionConflictException(id, update.version(), note.version());
        }
        return save(noteConverter.applyUpdate(note, update));
    }

    public NoteDto archiveNote(UUID id) {
        var note = findNote(id);
        if (note.status() == NoteStatus.ARCHIVED) {
            return noteConverter.toDto(note);
        }
        return save(note.toBuilder().status(NoteStatus.ARCHIVED).archivedDate(timeProvider.now()).edited(true).build());
    }

    public NoteDto restoreNote(UUID id) {
        var note = findNote(id);
        if (note.status() == NoteStatus.ACTIVE) {
            return noteConverter.toDto(note);
        }
        return save(note.toBuilder().status(NoteStatus.ACTIVE).archivedDate(null).edited(true).build());
    }

    private Note findNote(UUID id) {
        return noteRepository.findById(id).orElseThrow(() -> new NoteNotFoundException(id));
    }

    private NoteDto save(Note note) {
        var saved = noteRepository.save(note);
        eventPublisher.publishEvent(new NoteChangedEvent(noteConverter.toEventDto(saved)));
        return noteConverter.toDto(saved);
    }
}
