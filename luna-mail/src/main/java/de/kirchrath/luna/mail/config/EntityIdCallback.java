package de.kirchrath.luna.mail.config;

import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.mail.data.entity.EntityWithId;
import de.kirchrath.luna.mail.services.UuidProvider;

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
