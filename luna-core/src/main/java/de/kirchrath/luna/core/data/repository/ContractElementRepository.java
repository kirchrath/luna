package de.kirchrath.luna.core.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.core.data.entity.ContractElement;

public interface ContractElementRepository extends CrudRepository<ContractElement, UUID> {

    Optional<ContractElement> findByOutboundEventId(UUID outboundEventId);
}
