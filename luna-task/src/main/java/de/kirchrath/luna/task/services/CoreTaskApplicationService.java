package de.kirchrath.luna.task.services;

import org.springframework.stereotype.Service;

import de.kirchrath.luna.task.dto.core.TaskEventDto;

@Service
public class CoreTaskApplicationService {

    private final CoreTaskDomainService coreTaskDomainService;

    public CoreTaskApplicationService(CoreTaskDomainService coreTaskDomainService) {
        this.coreTaskDomainService = coreTaskDomainService;
    }

    public void process(TaskEventDto event) {
        coreTaskDomainService.process(event);
    }
}
