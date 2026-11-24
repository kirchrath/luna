package de.kirchrath.luna.core.data.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.core.data.entity.InboundEvent;

public interface InboundEventRepository extends CrudRepository<InboundEvent, UUID> {

    @Query("SELECT * FROM inbound_event WHERE status = :status ORDER BY created_date LIMIT :limit")
    List<InboundEvent> findOldestByStatus(String status, int limit);
}
