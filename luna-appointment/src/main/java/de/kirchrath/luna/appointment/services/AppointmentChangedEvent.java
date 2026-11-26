package de.kirchrath.luna.appointment.services;

import de.kirchrath.luna.appointment.dto.AppointmentEventDto;

/** Published within the transaction after a appointment was created or changed; sent to Kafka and clients after commit. */
public record AppointmentChangedEvent(AppointmentEventDto appointment) {
}
