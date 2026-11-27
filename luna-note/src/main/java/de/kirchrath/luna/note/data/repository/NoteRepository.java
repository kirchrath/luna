package de.kirchrath.luna.note.data.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import de.kirchrath.luna.note.data.entity.LifeArea;
import de.kirchrath.luna.note.data.entity.Note;
import de.kirchrath.luna.note.data.entity.NoteSource;
import de.kirchrath.luna.note.data.entity.NoteStatus;

public interface NoteRepository extends CrudRepository<Note, UUID>, PagingAndSortingRepository<Note, UUID> {

    Optional<Note> findBySourceAndSourceId(NoteSource source, UUID sourceId);

    Page<Note> findByAreaIn(Collection<LifeArea> areas, Pageable pageable);

    Page<Note> findByStatusAndAreaIn(NoteStatus status, Collection<LifeArea> areas, Pageable pageable);
}
