package de.kirchrath.luna.core.data.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.core.data.entity.OutboundEvent;

public interface OutboundEventRepository extends CrudRepository<OutboundEvent, UUID> {

    List<OutboundEvent> findByInboundEventId(UUID inboundEventId);

    List<OutboundEvent> findByInboundEventIdIn(Collection<UUID> inboundEventIds);
}
