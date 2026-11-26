package de.kirchrath.luna.alert.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.alert.converter.AlertConverter;
import de.kirchrath.luna.alert.data.entity.CoreAlertProjection;
import de.kirchrath.luna.alert.data.entity.Alert;
import de.kirchrath.luna.alert.data.entity.AlertSource;
import de.kirchrath.luna.alert.data.repository.CoreAlertProjectionRepository;
import de.kirchrath.luna.alert.data.repository.AlertRepository;
import de.kirchrath.luna.alert.dto.core.AlertEventDto;
import lombok.extern.slf4j.Slf4j;

/** Processes alert events of luna-core: only newer versions pass the projection and create or update the alert. */
@Slf4j
@Service
@Transactional
public class CoreAlertDomainService {

    private final CoreAlertProjectionRepository projectionRepository;
    private final AlertRepository alertRepository;
    private final JdbcAggregateTemplate aggregateTemplate;
    private final AlertConverter alertConverter;
    private final ApplicationEventPublisher eventPublisher;

    public CoreAlertDomainService(
            CoreAlertProjectionRepository projectionRepository,
            AlertRepository alertRepository,
            JdbcAggregateTemplate aggregateTemplate,
            AlertConverter alertConverter,
            ApplicationEventPublisher eventPublisher) {
        this.projectionRepository = projectionRepository;
        this.alertRepository = alertRepository;
        this.aggregateTemplate = aggregateTemplate;
        this.alertConverter = alertConverter;
        this.eventPublisher = eventPublisher;
    }

    public void process(AlertEventDto event) {
        var coreVersion = event.getAudit().getVersion();
        var projection = projectionRepository.findByCoreAlertId(event.getId());
        if (projection.isEmpty()) {
            // The projection has no optimistic lock, so Spring Data cannot tell a new one from an existing one.
            aggregateTemplate.insert(new CoreAlertProjection(null, event.getId(), coreVersion));
            publish(alertRepository.save(alertConverter.fromCore(event)));
            return;
        }
        if (projection.get().version() >= coreVersion) {
            log.debug("Ignored core alert {} in version {}; version {} was processed already",
                    event.getId(), coreVersion, projection.get().version());
            return;
        }
        projectionRepository.save(projection.get().withVersion(coreVersion));
        var alert = alertRepository.findBySourceAndSourceId(AlertSource.CORE, event.getId()).orElseThrow();
        // Alerts are not edited by the user, so newer content of luna-core is always applied.
        publish(alertRepository.save(alertConverter.applyCore(alert, event)));
    }

    private void publish(Alert alert) {
        eventPublisher.publishEvent(new AlertChangedEvent(alertConverter.toEventDto(alert)));
    }
}
