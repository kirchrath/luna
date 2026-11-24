package de.kirchrath.luna.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Manual input of the user that luna-core analyses")
public record ManualInputDataDto(
        @Schema(description = "Free text of the input", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 10_000)
        String text,

        @Schema(description = "Optional hint which element should result from the input; overrides the analysis")
        LunaElementTypeDto typeHint) {
}
