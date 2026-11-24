package de.kirchrath.luna.core.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;

@Service
@Transactional
public class InboundEventDomainService {

    private final InboundEventRepository inboundEventRepository;

    public InboundEventDomainService(InboundEventRepository inboundEventRepository) {
        this.inboundEventRepository = inboundEventRepository;
    }

    public UUID createInboundEvent(EventSource source, EventData data) {
        var inboundEvent = new InboundEvent(null, source, data, InboundEventStatus.RECEIVED, 0, null, null, null);
        return inboundEventRepository.save(inboundEvent).id();
    }
}
