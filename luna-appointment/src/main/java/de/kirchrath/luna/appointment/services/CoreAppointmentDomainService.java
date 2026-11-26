package de.kirchrath.luna.appointment.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.appointment.converter.AppointmentConverter;
import de.kirchrath.luna.appointment.data.entity.CoreAppointmentProjection;
import de.kirchrath.luna.appointment.data.entity.Appointment;
import de.kirchrath.luna.appointment.data.entity.AppointmentSource;
import de.kirchrath.luna.appointment.data.repository.CoreAppointmentProjectionRepository;
import de.kirchrath.luna.appointment.data.repository.AppointmentRepository;
import de.kirchrath.luna.appointment.dto.core.AppointmentEventDto;
import lombok.extern.slf4j.Slf4j;

/** Processes appointment events of luna-core: only newer versions pass the projection and create or update the appointment. */
@Slf4j
@Service
@Transactional
public class CoreAppointmentDomainService {

    private final CoreAppointmentProjectionRepository projectionRepository;
    private final AppointmentRepository appointmentRepository;
    private final JdbcAggregateTemplate aggregateTemplate;
    private final AppointmentConverter appointmentConverter;
    private final ApplicationEventPublisher eventPublisher;

    public CoreAppointmentDomainService(
            CoreAppointmentProjectionRepository projectionRepository,
            AppointmentRepository appointmentRepository,
            JdbcAggregateTemplate aggregateTemplate,
            AppointmentConverter appointmentConverter,
            ApplicationEventPublisher eventPublisher) {
        this.projectionRepository = projectionRepository;
        this.appointmentRepository = appointmentRepository;
        this.aggregateTemplate = aggregateTemplate;
        this.appointmentConverter = appointmentConverter;
        this.eventPublisher = eventPublisher;
    }

    public void process(AppointmentEventDto event) {
        var coreVersion = event.getAudit().getVersion();
        var projection = projectionRepository.findByCoreAppointmentId(event.getId());
        if (projection.isEmpty()) {
            // The projection has no optimistic lock, so Spring Data cannot tell a new one from an existing one.
            aggregateTemplate.insert(new CoreAppointmentProjection(null, event.getId(), coreVersion));
            publish(appointmentRepository.save(appointmentConverter.fromCore(event)));
            return;
        }
        if (projection.get().version() >= coreVersion) {
            log.debug("Ignored core appointment {} in version {}; version {} was processed already",
                    event.getId(), coreVersion, projection.get().version());
            return;
        }
        projectionRepository.save(projection.get().withVersion(coreVersion));
        var appointment = appointmentRepository.findBySourceAndSourceId(AppointmentSource.CORE, event.getId()).orElseThrow();
        if (appointment.edited()) {
            log.debug("Kept edited appointment {}; core appointment {} version {} is not applied", appointment.id(), event.getId(),
                    coreVersion);
            return;
        }
        publish(appointmentRepository.save(appointmentConverter.applyCore(appointment, event)));
    }

    private void publish(Appointment appointment) {
        eventPublisher.publishEvent(new AppointmentChangedEvent(appointmentConverter.toEventDto(appointment)));
    }
}
