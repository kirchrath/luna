package de.kirchrath.luna.note.services;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

@Component
public class TimeProvider {

    public OffsetDateTime now() {
        return OffsetDateTime.now();
    }

    public OffsetDateTime atServerOffset(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
