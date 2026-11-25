package de.kirchrath.luna.core.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.core.data.entity.AppointmentElement;

public interface AppointmentElementRepository extends CrudRepository<AppointmentElement, UUID> {

    Optional<AppointmentElement> findByOutboundEventId(UUID outboundEventId);
}
