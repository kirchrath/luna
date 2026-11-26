package de.kirchrath.luna.core.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = ElementEventDto.TYPE_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(name = LunaElementTypeDto.Constants.TASK, value = TaskEventDto.class),
        @JsonSubTypes.Type(name = LunaElementTypeDto.Constants.ALERT, value = AlertEventDto.class),
        @JsonSubTypes.Type(name = LunaElementTypeDto.Constants.APPOINTMENT, value = AppointmentEventDto.class),
        @JsonSubTypes.Type(name = LunaElementTypeDto.Constants.INVOICE, value = InvoiceEventDto.class),
        @JsonSubTypes.Type(name = LunaElementTypeDto.Constants.CONTRACT, value = ContractEventDto.class),
        @JsonSubTypes.Type(name = LunaElementTypeDto.Constants.NOTE, value = NoteEventDto.class),
})
@Schema(
        oneOf = {
                TaskEventDto.class,
                AlertEventDto.class,
                AppointmentEventDto.class,
                InvoiceEventDto.class,
                ContractEventDto.class,
                NoteEventDto.class,
        },
        discriminatorProperty = ElementEventDto.TYPE_PROPERTY,
        discriminatorMapping = {
                @DiscriminatorMapping(value = LunaElementTypeDto.Constants.TASK, schema = TaskEventDto.class),
                @DiscriminatorMapping(value = LunaElementTypeDto.Constants.ALERT, schema = AlertEventDto.class),
                @DiscriminatorMapping(value = LunaElementTypeDto.Constants.APPOINTMENT, schema = AppointmentEventDto.class),
                @DiscriminatorMapping(value = LunaElementTypeDto.Constants.INVOICE, schema = InvoiceEventDto.class),
                @DiscriminatorMapping(value = LunaElementTypeDto.Constants.CONTRACT, schema = ContractEventDto.class),
                @DiscriminatorMapping(value = LunaElementTypeDto.Constants.NOTE, schema = NoteEventDto.class),
        })
public sealed interface ElementEventDto
        permits TaskEventDto, AlertEventDto, AppointmentEventDto, InvoiceEventDto, ContractEventDto, NoteEventDto {

    String TYPE_PROPERTY = "type";

    UUID id();
}
