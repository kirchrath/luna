package de.kirchrath.luna.alert.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record AlertChangedDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id) {
}
