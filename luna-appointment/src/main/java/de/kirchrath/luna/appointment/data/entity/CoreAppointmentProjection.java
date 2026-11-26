package de.kirchrath.luna.appointment.data.entity;

import java.util.UUID;

import org.springframework.data.annotation.Id;

import lombok.With;

/**
 * Last processed version of a appointment from luna-core. {@code version} is the version of the core event, not an
 * optimistic lock: only events with a newer version are processed further.
 */
public record CoreAppointmentProjection(@With @Id UUID id, UUID coreAppointmentId, @With Long version)
        implements EntityWithId<CoreAppointmentProjection> {
}
