package de.kirchrath.luna.alert.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.alert.data.entity.CoreAlertProjection;

public interface CoreAlertProjectionRepository extends CrudRepository<CoreAlertProjection, UUID> {

    Optional<CoreAlertProjection> findByCoreAlertId(UUID coreAlertId);
}
