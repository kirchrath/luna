package de.kirchrath.luna.appointment.converter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.appointment.data.entity.Appointment;
import de.kirchrath.luna.appointment.data.entity.AppointmentSource;
import de.kirchrath.luna.appointment.data.entity.AppointmentStatus;
import de.kirchrath.luna.appointment.data.entity.LifeArea;
import de.kirchrath.luna.appointment.dto.AppointmentDto;
import de.kirchrath.luna.appointment.dto.AppointmentEventDto;
import de.kirchrath.luna.appointment.dto.AppointmentListItemDto;
import de.kirchrath.luna.appointment.dto.AppointmentUpdateDto;
import de.kirchrath.luna.appointment.dto.LifeAreaDto;
import de.kirchrath.luna.appointment.dto.core.AppointmentEventDataDto;

@Component
public class AppointmentConverter {

    private static final long ONE_DAY = 1;

    private final AppointmentMapper appointmentMapper;

    public AppointmentConverter(AppointmentMapper appointmentMapper) {
        this.appointmentMapper = appointmentMapper;
    }

    /** A new, unedited and scheduled appointment from an appointment event of luna-core. */
    public Appointment fromCore(de.kirchrath.luna.appointment.dto.core.AppointmentEventDto event) {
        return applyCore(Appointment.builder()
                .source(AppointmentSource.CORE)
                .sourceId(event.getId())
                .area(appointmentMapper.toEntity(event.getArea()))
                .status(AppointmentStatus.SCHEDULED)
                .edited(false)
                .build(), event);
    }

    /** The data of a newer appointment event of luna-core; status, origin and audit stay. */
    public Appointment applyCore(Appointment appointment, de.kirchrath.luna.appointment.dto.core.AppointmentEventDto event) {
        AppointmentEventDataDto data = event.getData();
        return withTimes(appointment.toBuilder()
                .title(data.getTitle())
                .location(data.getLocation())
                .description(data.getDescription()), data.getStart(), data.getEnd(), data.getAllDay());
    }

    /** The data edited by the user; the appointment counts as edited from now on. */
    public Appointment applyUpdate(Appointment appointment, AppointmentUpdateDto update) {
        return withTimes(appointment.toBuilder()
                .title(update.title())
                .location(update.location())
                .description(update.description())
                .edited(true), update.start(), update.end(), update.allDay());
    }

    public AppointmentDto toDto(Appointment appointment) {
        return appointmentMapper.toDto(appointment);
    }

    public AppointmentEventDto toEventDto(Appointment appointment) {
        return appointmentMapper.toEventDto(appointment);
    }

    public AppointmentListItemDto toListItemDto(Appointment appointment) {
        return appointmentMapper.toListItemDto(appointment);
    }

    /**
     * An appointment is over at its end, without end at its start; an all-day appointment at the end of its last
     * day.
     */
    private Appointment withTimes(
            Appointment.AppointmentBuilder builder, OffsetDateTime start, OffsetDateTime end, boolean allDay) {
        var last = end == null ? start : end;
        return builder
                .start(start)
                .end(end)
                .allDay(allDay)
                .effectiveEnd(allDay ? last.plusDays(ONE_DAY) : last)
                .build();
    }

    /** The given areas, or all areas without any. */
    public Set<LifeArea> toAreas(List<LifeAreaDto> areas) {
        return areas.isEmpty()
                ? Set.of(LifeArea.values())
                : areas.stream().map(appointmentMapper::toEntity).collect(Collectors.toUnmodifiableSet());
    }
}
