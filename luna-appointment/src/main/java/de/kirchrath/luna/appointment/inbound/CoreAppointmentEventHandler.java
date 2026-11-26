package de.kirchrath.luna.appointment.inbound;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.appointment.dto.core.AppointmentEventDto;
import de.kirchrath.luna.appointment.services.CoreAppointmentApplicationService;

/** Appointment events of luna-core; failures are retried via the retry topic and end up in the DLT (see KafkaConfig). */
@Component
public class CoreAppointmentEventHandler {

    private final CoreAppointmentApplicationService coreAppointmentApplicationService;

    public CoreAppointmentEventHandler(CoreAppointmentApplicationService coreAppointmentApplicationService) {
        this.coreAppointmentApplicationService = coreAppointmentApplicationService;
    }

    @KafkaListener(topics = "${app.kafka.topics.core-appointment}")
    public void handle(AppointmentEventDto event) {
        coreAppointmentApplicationService.process(event);
    }
}
