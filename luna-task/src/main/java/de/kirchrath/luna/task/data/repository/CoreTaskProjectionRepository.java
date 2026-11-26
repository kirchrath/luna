package de.kirchrath.luna.task.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.task.data.entity.CoreTaskProjection;

public interface CoreTaskProjectionRepository extends CrudRepository<CoreTaskProjection, UUID> {

    Optional<CoreTaskProjection> findByCoreTaskId(UUID coreTaskId);
}
