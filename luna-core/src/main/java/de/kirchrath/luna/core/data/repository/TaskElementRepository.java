package de.kirchrath.luna.core.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.core.data.entity.TaskElement;

public interface TaskElementRepository extends CrudRepository<TaskElement, UUID> {

    Optional<TaskElement> findByOutboundEventId(UUID outboundEventId);
}
