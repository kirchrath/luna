package de.kirchrath.luna.core.data.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LifeArea;

public interface InboundEventRepository
        extends CrudRepository<InboundEvent, UUID>, PagingAndSortingRepository<InboundEvent, UUID> {

    List<InboundEvent> findByStatusOrderByCreatedDateAsc(InboundEventStatus status, Limit limit);

    Page<InboundEvent> findByAreaIn(Collection<LifeArea> areas, Pageable pageable);

    boolean existsBySourceAndSourceId(EventSource source, UUID sourceId);
}
