package de.kirchrath.luna.alert.services;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.alert.converter.AlertConverter;
import de.kirchrath.luna.alert.data.entity.Alert;
import de.kirchrath.luna.alert.data.entity.AlertStatus;
import de.kirchrath.luna.alert.data.repository.AlertRepository;
import de.kirchrath.luna.alert.dto.AlertDto;
import de.kirchrath.luna.alert.dto.AlertListDto;
import de.kirchrath.luna.alert.dto.AlertStatusDto;
import de.kirchrath.luna.alert.exception.AlertNotFoundException;
import de.kirchrath.luna.alert.exception.AlertTransitionException;

@Service
@Transactional
public class AlertDomainService {

    /** Most severe first, then the newest. */
    private static final Sort LIST_ORDER =
            Sort.by(Sort.Order.asc(Alert::severityRank), Sort.Order.desc(Alert::createdDate));

    private final AlertRepository alertRepository;
    private final AlertConverter alertConverter;
    private final TimeProvider timeProvider;
    private final ApplicationEventPublisher eventPublisher;

    public AlertDomainService(
            AlertRepository alertRepository,
            AlertConverter alertConverter,
            TimeProvider timeProvider,
            ApplicationEventPublisher eventPublisher) {
        this.alertRepository = alertRepository;
        this.alertConverter = alertConverter;
        this.timeProvider = timeProvider;
        this.eventPublisher = eventPublisher;
    }

    /** Without statuses all alerts are listed. */
    @Transactional(readOnly = true)
    public AlertListDto listAlerts(List<AlertStatusDto> statuses, Pageable pageable) {
        var pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), LIST_ORDER);
        var alerts = statuses.isEmpty()
                ? alertRepository.findAll(pageRequest)
                : alertRepository.findByStatusIn(alertConverter.toEntities(statuses), pageRequest);
        return new AlertListDto(
                alerts.map(alertConverter::toListItemDto).getContent(), alerts.getTotalPages(),
                alerts.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AlertDto getAlert(UUID id) {
        return alertConverter.toDto(findAlert(id));
    }

    /** An open alert becomes acknowledged; a resolved one has to be reopened first. */
    public AlertDto acknowledgeAlert(UUID id) {
        var alert = findAlert(id);
        return switch (alert.status()) {
            case OPEN -> save(alert.toBuilder()
                    .status(AlertStatus.ACKNOWLEDGED)
                    .acknowledgedDate(timeProvider.now())
                    .build());
            case ACKNOWLEDGED -> alertConverter.toDto(alert);
            case RESOLVED -> throw new AlertTransitionException(id, alert.status().name(),
                    AlertStatus.ACKNOWLEDGED.name());
        };
    }

    public AlertDto resolveAlert(UUID id) {
        var alert = findAlert(id);
        if (alert.status() == AlertStatus.RESOLVED) {
            return alertConverter.toDto(alert);
        }
        return save(alert.toBuilder().status(AlertStatus.RESOLVED).resolvedDate(timeProvider.now()).build());
    }

    /** Reopening forgets when the alert was acknowledged and resolved. */
    public AlertDto reopenAlert(UUID id) {
        var alert = findAlert(id);
        if (alert.status() == AlertStatus.OPEN) {
            return alertConverter.toDto(alert);
        }
        return save(alert.toBuilder().status(AlertStatus.OPEN).acknowledgedDate(null).resolvedDate(null).build());
    }

    private Alert findAlert(UUID id) {
        return alertRepository.findById(id).orElseThrow(() -> new AlertNotFoundException(id));
    }

    private AlertDto save(Alert alert) {
        var saved = alertRepository.save(alert);
        eventPublisher.publishEvent(new AlertChangedEvent(alertConverter.toEventDto(saved)));
        return alertConverter.toDto(saved);
    }
}
