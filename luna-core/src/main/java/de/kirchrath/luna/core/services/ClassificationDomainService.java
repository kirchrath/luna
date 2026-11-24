package de.kirchrath.luna.core.services;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.config.ClassificationProperties;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class ClassificationDomainService {

    private final InboundEventRepository inboundEventRepository;
    private final OutboundEventRepository outboundEventRepository;
    private final ClassificationProperties classificationProperties;

    public ClassificationDomainService(
            InboundEventRepository inboundEventRepository,
            OutboundEventRepository outboundEventRepository,
            ClassificationProperties classificationProperties) {
        this.inboundEventRepository = inboundEventRepository;
        this.outboundEventRepository = outboundEventRepository;
        this.classificationProperties = classificationProperties;
    }

    public List<InboundEvent> findInboundEventsToClassify() {
        return inboundEventRepository.findByStatusOrderByCreatedDateAsc(
                InboundEventStatus.RECEIVED, Limit.of(classificationProperties.batchSize()));
    }

    public void completeClassification(InboundEvent inboundEvent, ClassificationOutcome outcome) {
        if (outcome.failed()) {
            recordFailedAttempt(inboundEvent, outcome.failure());
            return;
        }
        outcome.types().forEach(type -> outboundEventRepository.save(
                new OutboundEvent(null, inboundEvent.id(), type, OutboundEventStatus.CLASSIFIED, null, null, null)));
        inboundEventRepository.save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
        log.debug("Classified inbound event {} as {}", inboundEvent.id(), outcome.types());
    }

    private void recordFailedAttempt(InboundEvent inboundEvent, RuntimeException cause) {
        var attempts = inboundEvent.attempts() + 1;
        var maxAttempts = classificationProperties.maxAttempts();
        if (attempts >= maxAttempts) {
            log.error("Classification of inbound event {} failed finally after {} attempts",
                    inboundEvent.id(), attempts, cause);
            inboundEventRepository.save(inboundEvent.withAttempts(attempts).withStatus(InboundEventStatus.FAILED));
        } else {
            log.warn("Classification of inbound event {} failed (attempt {}/{})",
                    inboundEvent.id(), attempts, maxAttempts, cause);
            inboundEventRepository.save(inboundEvent.withAttempts(attempts).withStatus(InboundEventStatus.RECEIVED));
        }
    }
}
