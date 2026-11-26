package de.kirchrath.luna.alert.services;

import de.kirchrath.luna.alert.dto.AlertEventDto;

/** Published within the transaction after a alert was created or changed; sent to Kafka and clients after commit. */
public record AlertChangedEvent(AlertEventDto alert) {
}
