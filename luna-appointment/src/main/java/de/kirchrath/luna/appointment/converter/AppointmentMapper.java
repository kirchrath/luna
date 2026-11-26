package de.kirchrath.luna.appointment.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.appointment.data.entity.Appointment;
import de.kirchrath.luna.appointment.dto.AuditDto;
import de.kirchrath.luna.appointment.dto.AppointmentDataDto;
import de.kirchrath.luna.appointment.dto.AppointmentDto;
import de.kirchrath.luna.appointment.dto.AppointmentEventDto;
import de.kirchrath.luna.appointment.dto.AppointmentListItemDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AppointmentMapper {

    AppointmentDataDto toDataDto(Appointment appointment);

    AuditDto toAuditDto(Appointment appointment);

    @Mapping(target = "data", source = "appointment")
    @Mapping(target = "audit", source = "appointment")
    AppointmentDto toDto(Appointment appointment);

    @Mapping(target = "audit", source = "appointment")
    @Mapping(target = "data", source = "appointment")
    AppointmentEventDto toEventDto(Appointment appointment);

    AppointmentListItemDto toListItemDto(Appointment appointment);
}
