package de.kirchrath.luna.core.data.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.core.data.entity.InboundEvent;

public interface InboundEventRepository extends CrudRepository<InboundEvent, UUID> {
}
