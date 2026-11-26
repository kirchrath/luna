package de.kirchrath.luna.note.services;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class UuidProvider {

    public UUID randomUuid() {
        return UUID.randomUUID();
    }
}
