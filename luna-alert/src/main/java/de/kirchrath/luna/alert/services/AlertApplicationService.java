package de.kirchrath.luna.alert.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import de.kirchrath.luna.alert.dto.AlertDto;
import de.kirchrath.luna.alert.dto.AlertListDto;
import de.kirchrath.luna.alert.dto.AlertStatusDto;
import de.kirchrath.luna.alert.dto.LifeAreaDto;

@Service
public class AlertApplicationService {

    private final AlertDomainService alertDomainService;

    public AlertApplicationService(AlertDomainService alertDomainService) {
        this.alertDomainService = alertDomainService;
    }

    public AlertListDto listAlerts(List<AlertStatusDto> statuses, List<LifeAreaDto> areas, Pageable pageable) {
        return alertDomainService.listAlerts(statuses, areas, pageable);
    }

    public AlertDto getAlert(UUID id) {
        return alertDomainService.getAlert(id);
    }

    public AlertDto acknowledgeAlert(UUID id) {
        return alertDomainService.acknowledgeAlert(id);
    }

    public AlertDto resolveAlert(UUID id) {
        return alertDomainService.resolveAlert(id);
    }

    public AlertDto reopenAlert(UUID id) {
        return alertDomainService.reopenAlert(id);
    }
}
