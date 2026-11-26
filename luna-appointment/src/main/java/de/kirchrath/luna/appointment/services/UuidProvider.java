package de.kirchrath.luna.appointment.services;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class UuidProvider {

    public UUID randomUuid() {
        return UUID.randomUUID();
    }
}
