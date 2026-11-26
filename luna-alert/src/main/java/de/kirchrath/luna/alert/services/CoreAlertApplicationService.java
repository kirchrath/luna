package de.kirchrath.luna.alert.services;

import org.springframework.stereotype.Service;

import de.kirchrath.luna.alert.dto.core.AlertEventDto;

@Service
public class CoreAlertApplicationService {

    private final CoreAlertDomainService coreAlertDomainService;

    public CoreAlertApplicationService(CoreAlertDomainService coreAlertDomainService) {
        this.coreAlertDomainService = coreAlertDomainService;
    }

    public void process(AlertEventDto event) {
        coreAlertDomainService.process(event);
    }
}
