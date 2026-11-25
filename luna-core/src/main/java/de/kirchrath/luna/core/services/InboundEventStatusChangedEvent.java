package de.kirchrath.luna.core.services;

import java.util.UUID;

/** Published within the transaction when an inbound event reaches a final status. */
public record InboundEventStatusChangedEvent(UUID inboundEventId) {
}
