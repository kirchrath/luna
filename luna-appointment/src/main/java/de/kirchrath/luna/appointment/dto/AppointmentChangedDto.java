package de.kirchrath.luna.appointment.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record AppointmentChangedDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id) {
}
