package de.kirchrath.luna.core.data.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;

public interface InboundEventRepository
        extends CrudRepository<InboundEvent, UUID>, PagingAndSortingRepository<InboundEvent, UUID> {

    List<InboundEvent> findByStatusOrderByCreatedDateAsc(InboundEventStatus status, Limit limit);

    boolean existsBySourceAndSourceId(EventSource source, UUID sourceId);
}
