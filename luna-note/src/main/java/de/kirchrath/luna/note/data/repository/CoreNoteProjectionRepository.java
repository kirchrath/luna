package de.kirchrath.luna.note.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.note.data.entity.CoreNoteProjection;

public interface CoreNoteProjectionRepository extends CrudRepository<CoreNoteProjection, UUID> {

    Optional<CoreNoteProjection> findByCoreNoteId(UUID coreNoteId);
}
