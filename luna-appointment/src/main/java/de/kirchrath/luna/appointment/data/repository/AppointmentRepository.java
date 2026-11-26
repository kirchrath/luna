package de.kirchrath.luna.appointment.data.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import de.kirchrath.luna.appointment.data.entity.Appointment;
import de.kirchrath.luna.appointment.data.entity.AppointmentSource;

public interface AppointmentRepository
        extends CrudRepository<Appointment, UUID>, PagingAndSortingRepository<Appointment, UUID> {

    Optional<Appointment> findBySourceAndSourceId(AppointmentSource source, UUID sourceId);

    Page<Appointment> findByEffectiveEndGreaterThanEqual(OffsetDateTime now, Pageable pageable);

    Page<Appointment> findByEffectiveEndLessThan(OffsetDateTime now, Pageable pageable);

    /** All appointments that overlap the range from (inclusive) to (exclusive). */
    List<Appointment> findByStartLessThanAndEffectiveEndGreaterThan(OffsetDateTime to, OffsetDateTime from, Sort sort);
}
