package de.kirchrath.luna.core.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.config.ClassificationProperties;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;

@Component
public class ClassificationJob {

    private final InboundEventRepository inboundEventRepository;
    private final ClassificationService classificationService;
    private final ClassificationProperties classificationProperties;

    public ClassificationJob(
            InboundEventRepository inboundEventRepository,
            ClassificationService classificationService,
            ClassificationProperties classificationProperties) {
        this.inboundEventRepository = inboundEventRepository;
        this.classificationService = classificationService;
        this.classificationProperties = classificationProperties;
    }

    @Scheduled(fixedDelayString = "${app.classification.interval}")
    public void classifyReceivedEvents() {
        inboundEventRepository
                .findOldestByStatus(InboundEventStatus.RECEIVED.name(), classificationProperties.batchSize())
                .forEach(classificationService::classify);
    }
}
