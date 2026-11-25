package de.kirchrath.luna.core.converter;

import java.util.List;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.ManualInboundEventListItemDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;

@Component
public class InboundEventConverter {

    private final InboundEventMapper inboundEventMapper;

    public InboundEventConverter(InboundEventMapper inboundEventMapper) {
        this.inboundEventMapper = inboundEventMapper;
    }

    public ManualInboundEventListItemDto toManualListItemDto(
            InboundEvent inboundEvent, ManualInputDataDto input, List<OutboundEvent> outboundEvents) {
        return inboundEventMapper.toManualListItemDto(inboundEvent, input, outboundEvents);
    }
}
