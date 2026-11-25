package de.kirchrath.luna.core.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.core.data.entity.InvoiceElement;

public interface InvoiceElementRepository extends CrudRepository<InvoiceElement, UUID> {

    Optional<InvoiceElement> findByOutboundEventId(UUID outboundEventId);
}
