package de.kirchrath.luna.note.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record NoteChangedDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id) {
}
