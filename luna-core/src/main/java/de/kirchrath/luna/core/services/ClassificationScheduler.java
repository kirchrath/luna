package de.kirchrath.luna.core.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClassificationScheduler {

    private final ClassificationApplicationService classificationApplicationService;

    public ClassificationScheduler(ClassificationApplicationService classificationApplicationService) {
        this.classificationApplicationService = classificationApplicationService;
    }

    @Scheduled(fixedDelayString = "${app.classification.interval}")
    public void classifyReceivedEvents() {
        classificationApplicationService.findInboundEventsToClassify()
                .forEach(classificationApplicationService::classify);
    }
}
