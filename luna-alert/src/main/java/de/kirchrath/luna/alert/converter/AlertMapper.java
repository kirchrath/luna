package de.kirchrath.luna.alert.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.alert.data.entity.Alert;
import de.kirchrath.luna.alert.data.entity.AlertSeverity;
import de.kirchrath.luna.alert.data.entity.AlertStatus;
import de.kirchrath.luna.alert.dto.AlertDataDto;
import de.kirchrath.luna.alert.dto.AlertDto;
import de.kirchrath.luna.alert.dto.AlertEventDto;
import de.kirchrath.luna.alert.dto.AlertListItemDto;
import de.kirchrath.luna.alert.dto.AlertStatusDto;
import de.kirchrath.luna.alert.dto.AuditDto;
import de.kirchrath.luna.alert.dto.core.AlertEventDataDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AlertMapper {

    AlertDataDto toDataDto(Alert alert);

    AuditDto toAuditDto(Alert alert);

    @Mapping(target = "data", source = "alert")
    @Mapping(target = "audit", source = "alert")
    AlertDto toDto(Alert alert);

    @Mapping(target = "audit", source = "alert")
    @Mapping(target = "data", source = "alert")
    AlertEventDto toEventDto(Alert alert);

    AlertListItemDto toListItemDto(Alert alert);

    AlertStatus toEntity(AlertStatusDto status);

    AlertSeverity toEntity(AlertEventDataDto.SeverityEnum severity);
}
