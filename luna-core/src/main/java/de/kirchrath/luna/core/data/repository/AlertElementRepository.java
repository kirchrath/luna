package de.kirchrath.luna.core.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.core.data.entity.AlertElement;

public interface AlertElementRepository extends CrudRepository<AlertElement, UUID> {

    Optional<AlertElement> findByOutboundEventId(UUID outboundEventId);
}
