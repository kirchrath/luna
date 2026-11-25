package de.kirchrath.luna.core.dto;

import java.util.UUID;

public sealed interface ElementEventDto
        permits TaskEventDto, AlertEventDto, AppointmentEventDto, InvoiceEventDto, ContractEventDto, NoteEventDto {

    UUID id();
}
