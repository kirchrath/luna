package de.kirchrath.luna.task.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record TaskChangedDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id) {
}
