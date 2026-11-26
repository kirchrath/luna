package de.kirchrath.luna.note.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.note.converter.NoteConverter;
import de.kirchrath.luna.note.data.entity.CoreNoteProjection;
import de.kirchrath.luna.note.data.entity.Note;
import de.kirchrath.luna.note.data.entity.NoteSource;
import de.kirchrath.luna.note.data.repository.CoreNoteProjectionRepository;
import de.kirchrath.luna.note.data.repository.NoteRepository;
import de.kirchrath.luna.note.dto.core.NoteEventDto;
import lombok.extern.slf4j.Slf4j;

/** Processes note events of luna-core: only newer versions pass the projection and create or update the note. */
@Slf4j
@Service
@Transactional
public class CoreNoteDomainService {

    private final CoreNoteProjectionRepository projectionRepository;
    private final NoteRepository noteRepository;
    private final JdbcAggregateTemplate aggregateTemplate;
    private final NoteConverter noteConverter;
    private final ApplicationEventPublisher eventPublisher;

    public CoreNoteDomainService(
            CoreNoteProjectionRepository projectionRepository,
            NoteRepository noteRepository,
            JdbcAggregateTemplate aggregateTemplate,
            NoteConverter noteConverter,
            ApplicationEventPublisher eventPublisher) {
        this.projectionRepository = projectionRepository;
        this.noteRepository = noteRepository;
        this.aggregateTemplate = aggregateTemplate;
        this.noteConverter = noteConverter;
        this.eventPublisher = eventPublisher;
    }

    public void process(NoteEventDto event) {
        var coreVersion = event.getAudit().getVersion();
        var projection = projectionRepository.findByCoreNoteId(event.getId());
        if (projection.isEmpty()) {
            // The projection has no optimistic lock, so Spring Data cannot tell a new one from an existing one.
            aggregateTemplate.insert(new CoreNoteProjection(null, event.getId(), coreVersion));
            publish(noteRepository.save(noteConverter.fromCore(event)));
            return;
        }
        if (projection.get().version() >= coreVersion) {
            log.debug("Ignored core note {} in version {}; version {} was processed already",
                    event.getId(), coreVersion, projection.get().version());
            return;
        }
        projectionRepository.save(projection.get().withVersion(coreVersion));
        var note = noteRepository.findBySourceAndSourceId(NoteSource.CORE, event.getId()).orElseThrow();
        if (note.edited()) {
            log.debug("Kept edited note {}; core note {} version {} is not applied", note.id(), event.getId(),
                    coreVersion);
            return;
        }
        publish(noteRepository.save(noteConverter.applyCore(note, event)));
    }

    private void publish(Note note) {
        eventPublisher.publishEvent(new NoteChangedEvent(noteConverter.toEventDto(note)));
    }
}
