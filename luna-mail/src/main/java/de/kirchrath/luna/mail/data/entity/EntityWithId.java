package de.kirchrath.luna.mail.data.entity;

import java.util.UUID;

public interface EntityWithId<T extends EntityWithId<T>> {

    UUID id();

    T withId(UUID id);
}
