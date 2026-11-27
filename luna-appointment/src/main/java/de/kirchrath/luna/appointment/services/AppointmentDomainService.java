package de.kirchrath.luna.appointment.services;

import java.util.List;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.appointment.converter.AppointmentConverter;
import de.kirchrath.luna.appointment.data.entity.Appointment;
import de.kirchrath.luna.appointment.data.entity.AppointmentStatus;
import de.kirchrath.luna.appointment.data.repository.AppointmentRepository;
import de.kirchrath.luna.appointment.dto.AppointmentDto;
import de.kirchrath.luna.appointment.dto.AppointmentListDto;
import de.kirchrath.luna.appointment.dto.AppointmentPeriodDto;
import de.kirchrath.luna.appointment.dto.AppointmentUpdateDto;
import de.kirchrath.luna.appointment.dto.LifeAreaDto;
import de.kirchrath.luna.appointment.exception.AppointmentEndBeforeStartException;
import de.kirchrath.luna.appointment.exception.AppointmentNotFoundException;
import de.kirchrath.luna.appointment.exception.AppointmentVersionConflictException;

@Service
@Transactional
public class AppointmentDomainService {

    private static final Sort EARLIEST_FIRST = Sort.by(Sort.Order.asc(Appointment::start));
    private static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc(Appointment::start));
    private static final int SINGLE_PAGE = 1;

    private final AppointmentRepository appointmentRepository;
    private final AppointmentConverter appointmentConverter;
    private final TimeProvider timeProvider;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentDomainService(
            AppointmentRepository appointmentRepository,
            AppointmentConverter appointmentConverter,
            TimeProvider timeProvider,
            ApplicationEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentConverter = appointmentConverter;
        this.timeProvider = timeProvider;
        this.eventPublisher = eventPublisher;
    }

    /** Upcoming appointments earliest first, past ones latest first. */
    @Transactional(readOnly = true)
    public AppointmentListDto listAppointments(
            AppointmentPeriodDto period, List<LifeAreaDto> areas, Pageable pageable) {
        var now = timeProvider.now();
        var lifeAreas = appointmentConverter.toAreas(areas);
        var appointments = switch (period) {
            case UPCOMING -> appointmentRepository.findByEffectiveEndGreaterThanEqualAndAreaIn(
                    now, lifeAreas, page(pageable, EARLIEST_FIRST));
            case PAST -> appointmentRepository.findByEffectiveEndLessThanAndAreaIn(
                    now, lifeAreas, page(pageable, LATEST_FIRST));
            case ALL -> appointmentRepository.findByAreaIn(lifeAreas, page(pageable, EARLIEST_FIRST));
        };
        return new AppointmentListDto(
                appointments.map(appointmentConverter::toListItemDto).getContent(), appointments.getTotalPages(),
                appointments.getTotalElements());
    }

    /** All appointments overlapping the range, e.g. a month of the calendar. */
    @Transactional(readOnly = true)
    public AppointmentListDto listAppointmentsInRange(
            OffsetDateTime from, OffsetDateTime to, List<LifeAreaDto> areas) {
        var items = appointmentRepository
                .findByStartLessThanAndEffectiveEndGreaterThanAndAreaIn(
                        to, from, appointmentConverter.toAreas(areas), EARLIEST_FIRST)
                .stream()
                .map(appointmentConverter::toListItemDto)
                .toList();
        return new AppointmentListDto(items, SINGLE_PAGE, items.size());
    }

    @Transactional(readOnly = true)
    public AppointmentDto getAppointment(UUID id) {
        return appointmentConverter.toDto(findAppointment(id));
    }

    public AppointmentDto updateAppointment(UUID id, AppointmentUpdateDto update) {
        if (update.end() != null && update.end().isBefore(update.start())) {
            throw new AppointmentEndBeforeStartException();
        }
        var appointment = findAppointment(id);
        if (!appointment.version().equals(update.version())) {
            throw new AppointmentVersionConflictException(id, update.version(), appointment.version());
        }
        return save(appointmentConverter.applyUpdate(appointment, update));
    }

    public AppointmentDto cancelAppointment(UUID id) {
        var appointment = findAppointment(id);
        if (appointment.status() == AppointmentStatus.CANCELLED) {
            return appointmentConverter.toDto(appointment);
        }
        return save(appointment.toBuilder()
                .status(AppointmentStatus.CANCELLED)
                .cancelledDate(timeProvider.now())
                .edited(true)
                .build());
    }

    public AppointmentDto restoreAppointment(UUID id) {
        var appointment = findAppointment(id);
        if (appointment.status() == AppointmentStatus.SCHEDULED) {
            return appointmentConverter.toDto(appointment);
        }
        return save(appointment.toBuilder().status(AppointmentStatus.SCHEDULED).cancelledDate(null).edited(true).build());
    }

    private PageRequest page(Pageable pageable, Sort sort) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Appointment findAppointment(UUID id) {
        return appointmentRepository.findById(id).orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    private AppointmentDto save(Appointment appointment) {
        var saved = appointmentRepository.save(appointment);
        eventPublisher.publishEvent(new AppointmentChangedEvent(appointmentConverter.toEventDto(saved)));
        return appointmentConverter.toDto(saved);
    }
}
