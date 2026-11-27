package de.kirchrath.luna.appointment.services;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import de.kirchrath.luna.appointment.dto.AppointmentDto;
import de.kirchrath.luna.appointment.dto.AppointmentListDto;
import de.kirchrath.luna.appointment.dto.AppointmentPeriodDto;
import de.kirchrath.luna.appointment.dto.AppointmentUpdateDto;
import de.kirchrath.luna.appointment.dto.LifeAreaDto;

@Service
public class AppointmentApplicationService {

    private final AppointmentDomainService appointmentDomainService;

    public AppointmentApplicationService(AppointmentDomainService appointmentDomainService) {
        this.appointmentDomainService = appointmentDomainService;
    }

    public AppointmentListDto listAppointments(AppointmentPeriodDto period, LifeAreaDto area, Pageable pageable) {
        return appointmentDomainService.listAppointments(period, area, pageable);
    }

    public AppointmentListDto listAppointmentsInRange(OffsetDateTime from, OffsetDateTime to, LifeAreaDto area) {
        return appointmentDomainService.listAppointmentsInRange(from, to, area);
    }

    public AppointmentDto getAppointment(UUID id) {
        return appointmentDomainService.getAppointment(id);
    }

    public AppointmentDto updateAppointment(UUID id, AppointmentUpdateDto update) {
        return appointmentDomainService.updateAppointment(id, update);
    }

    public AppointmentDto cancelAppointment(UUID id) {
        return appointmentDomainService.cancelAppointment(id);
    }

    public AppointmentDto restoreAppointment(UUID id) {
        return appointmentDomainService.restoreAppointment(id);
    }
}
