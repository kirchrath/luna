package de.kirchrath.luna.appointment.services;

import org.springframework.stereotype.Service;

import de.kirchrath.luna.appointment.dto.core.AppointmentEventDto;

@Service
public class CoreAppointmentApplicationService {

    private final CoreAppointmentDomainService coreAppointmentDomainService;

    public CoreAppointmentApplicationService(CoreAppointmentDomainService coreAppointmentDomainService) {
        this.coreAppointmentDomainService = coreAppointmentDomainService;
    }

    public void process(AppointmentEventDto event) {
        coreAppointmentDomainService.process(event);
    }
}
