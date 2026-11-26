package de.kirchrath.luna.appointment.config;

import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.appointment.data.entity.EntityWithId;
import de.kirchrath.luna.appointment.services.UuidProvider;

@Component
public class EntityIdCallback implements BeforeConvertCallback<EntityWithId<?>> {

    private final UuidProvider uuidProvider;

    public EntityIdCallback(UuidProvider uuidProvider) {
        this.uuidProvider = uuidProvider;
    }

    @Override
    public EntityWithId<?> onBeforeConvert(EntityWithId<?> entity) {
        return entity.id() == null ? entity.withId(uuidProvider.randomUuid()) : entity;
    }
}
