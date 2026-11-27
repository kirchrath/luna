package de.kirchrath.luna.appointment.data.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
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
import de.kirchrath.luna.appointment.data.entity.LifeArea;

public interface AppointmentRepository
        extends CrudRepository<Appointment, UUID>, PagingAndSortingRepository<Appointment, UUID> {

    Optional<Appointment> findBySourceAndSourceId(AppointmentSource source, UUID sourceId);

    Page<Appointment> findByAreaIn(Collection<LifeArea> areas, Pageable pageable);

    Page<Appointment> findByEffectiveEndGreaterThanEqualAndAreaIn(
            OffsetDateTime now, Collection<LifeArea> areas, Pageable pageable);

    Page<Appointment> findByEffectiveEndLessThanAndAreaIn(
            OffsetDateTime now, Collection<LifeArea> areas, Pageable pageable);

    /** All appointments of the areas that overlap the range from (inclusive) to (exclusive). */
    List<Appointment> findByStartLessThanAndEffectiveEndGreaterThanAndAreaIn(
            OffsetDateTime to, OffsetDateTime from, Collection<LifeArea> areas, Sort sort);
}
