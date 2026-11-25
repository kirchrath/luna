package de.kirchrath.luna.core.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.core.data.entity.NoteElement;

public interface NoteElementRepository extends CrudRepository<NoteElement, UUID> {

    Optional<NoteElement> findByOutboundEventId(UUID outboundEventId);
}
