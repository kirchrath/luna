package de.kirchrath.luna.core.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class ClassificationApplicationService {

    private final ClassificationDomainService classificationDomainService;

    public ClassificationApplicationService(ClassificationDomainService classificationDomainService) {
        this.classificationDomainService = classificationDomainService;
    }

    public List<UUID> findInboundEventIdsToClassify() {
        return classificationDomainService.findInboundEventIdsToClassify();
    }

    public void classify(UUID inboundEventId) {
        classificationDomainService.classify(inboundEventId);
    }
}
