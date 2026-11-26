package de.kirchrath.luna.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManualInputDataDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 10_000)
        String text,

        LunaElementTypeDto typeHint) {
}
