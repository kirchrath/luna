package de.kirchrath.luna.appointment.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.appointment.data.entity.CoreAppointmentProjection;

public interface CoreAppointmentProjectionRepository extends CrudRepository<CoreAppointmentProjection, UUID> {

    Optional<CoreAppointmentProjection> findByCoreAppointmentId(UUID coreAppointmentId);
}
