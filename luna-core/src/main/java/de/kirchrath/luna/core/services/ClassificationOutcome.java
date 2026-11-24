package de.kirchrath.luna.core.services;

import java.util.Set;

import de.kirchrath.luna.core.data.entity.LunaElementType;

public record ClassificationOutcome(Set<LunaElementType> types, RuntimeException failure) {

    public boolean failed() {
        return failure != null;
    }
}
