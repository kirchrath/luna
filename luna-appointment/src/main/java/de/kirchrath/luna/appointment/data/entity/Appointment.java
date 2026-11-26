package de.kirchrath.luna.appointment.data.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import lombok.Builder;
import lombok.With;

/**
 * An appointment. All-day appointments start and end at 00:00 local time, {@code end} being the last day.
 * {@code effectiveEnd} is the moment the appointment is over; it separates upcoming from past appointments.
 */
@Builder(toBuilder = true)
public record Appointment(
        @With @Id UUID id,
        AppointmentSource source,
        UUID sourceId,
        String title,
        OffsetDateTime start,
        OffsetDateTime end,
        boolean allDay,
        String location,
        String description,
        OffsetDateTime effectiveEnd,
        AppointmentStatus status,
        OffsetDateTime cancelledDate,
        boolean edited,
        @CreatedDate OffsetDateTime createdDate,
        @LastModifiedDate OffsetDateTime lastModifiedDate,
        @Version Long version) implements EntityWithId<Appointment> {
}
