package de.kirchrath.luna.core.services;

import java.util.List;

import org.springframework.stereotype.Service;

import de.kirchrath.luna.core.data.entity.InboundEvent;

@Service
public class ClassificationApplicationService {

    private final ClassificationDomainService classificationDomainService;

    public ClassificationApplicationService(ClassificationDomainService classificationDomainService) {
        this.classificationDomainService = classificationDomainService;
    }

    public List<InboundEvent> findInboundEventsToClassify() {
        return classificationDomainService.findInboundEventsToClassify();
    }

    public void classify(InboundEvent inboundEvent) {
        classificationDomainService.classify(inboundEvent);
    }
}
